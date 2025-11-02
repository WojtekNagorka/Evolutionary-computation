import java.util.*;

public class CandidateMovesLocalSearch extends TSPSolver{
    private final int topN;
    private final boolean useNodeExchange;
    private final List<List<Integer>> candidateEdges;
    public CandidateMovesLocalSearch(double[][] distanceMatrix, List<Node> nodes, boolean useNodeExchange){
        super(distanceMatrix, nodes);
        this.topN = 10;
        this.useNodeExchange = useNodeExchange;
        this.candidateEdges = getCandidateEdges();
    }

    private List<List<Integer>> getCandidateEdges() {
        List<List<Integer>> candidates = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            double[] costs = new double[nodes.size()];
            for (int j = 0; j < nodes.size(); j++) {
                if (i == j) {
                    costs[j] = Double.POSITIVE_INFINITY;
                } else {
                    double edgeLength = distanceMatrix[i][j];
                    double vertexCost = nodes.get(j).getCost();
                    costs[j] = edgeLength + vertexCost;
                }
            }

            Integer[] indices = new Integer[nodes.size()];
            for (int j = 0; j < nodes.size(); j++) indices[j] = j;
            Arrays.sort(indices, Comparator.comparingDouble(j -> costs[j]));

            List<Integer> nearest = new ArrayList<>();
            for (int t = 0; t < Math.min(topN, nodes.size() - 1); t++) {
                nearest.add(indices[t]);
            }
            candidates.add(nearest);
        }

        return candidates;
    }

    public Result solve(List<Integer> initialRoute) {
        boolean improved;
        int iteration = 0;

        List<Integer> route = new ArrayList<>(initialRoute);
        if (route.size() > 1 && route.get(0).equals(route.get(route.size() - 1))) {
            route.remove(route.size() - 1);
        }
        double currentCost = computeTotalCost(closed(route));

        do {
            improved = false;
            Move bestMove = null;
            double bestDelta = 0.0;

            // Compose full neighborhood (intra + inter)
            List<Move> neighborhood = generateNeighborhood(route);

            for (Move move : neighborhood) {
                double delta = computeDelta(route, move);
                if (delta < -1e-9) {
                    if (bestMove == null || delta < bestDelta) {
                        bestMove = move;
                        bestDelta = delta;
                    }
                }
            }

            if (bestMove != null) {
                applyMove(route, bestMove);
                currentCost += bestDelta;
                improved = true;
            }

            iteration++;

        } while (improved);

        route.add(route.get(0));
        double finalCost = computeTotalCost(route);
        return new Result(route, finalCost);
    }

    /** Generate combined intra- and inter-route neighborhood */
    private List<Move> generateNeighborhood(List<Integer> route) {
        List<Move> moves = new ArrayList<>();
        int n = route.size();

        // Intra-route moves
        if (useNodeExchange) {
            for (int i = 0; i < n; i++) {
                int nodeI = route.get(i);
                List<Integer> nearest = candidateEdges.get(nodeI);

                for (int neighbor : nearest) {
                    int j = route.indexOf(neighbor);
                    if (j != -1 && j > i) { // only if neighbor is in the route
                        moves.add(new Move(MoveType.SWAP_NODES, i, j));
                    }
                }
            }
        } else { // 2-opt
            for (int i = 0; i < n - 2; i++) {
                int nodeI = route.get(i);
                for (int neighbor : candidateEdges.get(nodeI)) {
                    int j = route.indexOf(neighbor);
                    if (j != -1 && j > i + 1 && !(i == 0 && j == n - 1)) {
                        moves.add(new Move(MoveType.TWO_OPT, i, j));
                    }
                }
            }
        }

        // Inter-route: swap one selected and one unselected
        Set<Integer> selected = new HashSet<>(route);
        for (int i = 0; i < n; i++) {
            int selectedNode = route.get(i);
            for (int neighbor : candidateEdges.get(selectedNode)) {
                if (!selected.contains(neighbor)) {
                    moves.add(new Move(MoveType.EXCHANGE_SELECTED_UNSELECTED, i, neighbor));
                }
            }
        }

        return moves;
    }

    /** Compute delta change for given move */
    private double computeDelta(List<Integer> route, Move move) {
        switch (move.type) {
            case SWAP_NODES:
                return deltaSwap(route, move.i, move.j);
            case TWO_OPT:
                return deltaTwoOpt(route, move.i, move.j);
            case EXCHANGE_SELECTED_UNSELECTED:
                return deltaInter(route, move.i, move.j);
            default:
                return 0.0;
        }
    }

    /** Delta for swapping two nodes */
    private double deltaSwap(List<Integer> route, int i, int j) {
        int n = route.size();
        if (i == j) return 0;

        int ni = route.get(i);
        int nj = route.get(j);
        int iPrev = route.get((i - 1 + n) % n);
        int iNext = route.get((i + 1) % n);
        int jPrev = route.get((j - 1 + n) % n);
        int jNext = route.get((j + 1) % n);

        double before = 0, after = 0;

        if (iPrev != nj && jPrev != ni) {
            before += distanceMatrix[iPrev][ni] + distanceMatrix[ni][iNext];
            before += distanceMatrix[jPrev][nj] + distanceMatrix[nj][jNext];

            after += distanceMatrix[iPrev][nj] + distanceMatrix[nj][iNext];
            after += distanceMatrix[jPrev][ni] + distanceMatrix[ni][jNext];
        }

        return after - before;
    }

    /** Delta for 2-opt edge reversal */
    private double deltaTwoOpt(List<Integer> route, int i, int j) {
        int a = route.get(i);
        int b = route.get(i + 1);
        int c = route.get(j);
        int d = route.get((j + 1) % route.size());

        double before = distanceMatrix[a][b] + distanceMatrix[c][d];
        double after = distanceMatrix[a][c] + distanceMatrix[b][d];
        return after - before;
    }

    /** Delta for inter-route (selected ↔ unselected) swap */
    private double deltaInter(List<Integer> route, int selectedIndex, int unselectedNode) {
        int selectedNode = route.get(selectedIndex);
        int prev = route.get((selectedIndex - 1 + route.size()) % route.size());
        int next = route.get((selectedIndex + 1) % route.size());

        double before = distanceMatrix[prev][selectedNode] + distanceMatrix[selectedNode][next];
        double after = distanceMatrix[prev][unselectedNode] + distanceMatrix[unselectedNode][next];

        Node selected = nodes.get(selectedNode);
        Node unselected = nodes.get(unselectedNode);
        before += selected.getCost();
        after += unselected.getCost();

        return after - before;
    }

    /** Apply the move */
    private void applyMove(List<Integer> route, Move move) {
        switch (move.type) {
            case SWAP_NODES:
                Collections.swap(route, move.i, move.j);
                break;
            case TWO_OPT:
                reverseSublist(route, move.i + 1, move.j);
                break;
            case EXCHANGE_SELECTED_UNSELECTED:
                route.set(move.i, move.j);
                break;
        }
    }

    /** Reverse a sublist (for 2-opt) */
    private void reverseSublist(List<Integer> route, int start, int end) {
        while (start < end) {
            Collections.swap(route, start++, end--);
        }
    }

    /** Close the route */
    private List<Integer> closed(List<Integer> route) {
        List<Integer> r = new ArrayList<>(route);
        r.add(route.get(0));
        return r;
    }

    private enum MoveType {
        SWAP_NODES,
        TWO_OPT,
        EXCHANGE_SELECTED_UNSELECTED
    }

    private static class Move {
        MoveType type;
        int i, j;
        Move(MoveType type, int i, int j) {
            this.type = type;
            this.i = i;
            this.j = j;
        }
    }
}
