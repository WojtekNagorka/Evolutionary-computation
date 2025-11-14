import java.util.*;
import java.util.stream.Collectors;

/**
 * Local Search for TSP.
 *
 * Implements two steepest descent algorithms:
 * 1. Baseline: Re-evaluates the entire neighborhood each iteration.
 * 2. With Move List (LM): Reuses move evaluations (deltas) from previous iterations.
 *
 * Neighborhood = intra-route (2-opt) + inter-route (node exchange).
 */
public class LocalSearch extends TSPSolver {

    private final boolean useSteepest;
    private final boolean useNodeExchange;
    private final Random random;
    private final boolean useMoveList;
    private List<Move> improvingMoveList;



    public LocalSearch(double[][] distanceMatrix, List<Node> nodes,
                       boolean useSteepest, boolean useNodeExchange, boolean useMoveList) {
        super(distanceMatrix, nodes);
        // For LM search, we MUST use steepest and 2-opt (edge swap)
        if (useMoveList) {
            this.useSteepest = true;
            this.useNodeExchange = false; // Assignment implies 2-opt (edge exchange)
        } else {
            this.useSteepest = useSteepest;
            this.useNodeExchange = useNodeExchange;
        }
        this.useMoveList = useMoveList;
        this.random = new Random();

        this.improvingMoveList = new ArrayList<>();
    }

    /**
     * Main solver entry point.
     * Routes to the correct local search implementation based on the 'useMoveList' flag.
     */
    public Result solve(List<Integer> initialRoute) {
        if (useMoveList) {
            return solveSteepestLM(initialRoute);
        } else {
            // Use the original baseline method
            return solveSteepestBaseline(initialRoute);
        }
    }

    /**
     * The Steepest Local Search with Move List (LM).
     */
    public Result solveSteepestLM(List<Integer> initialRoute) {
        List<Integer> route = new ArrayList<>(initialRoute);
        if (route.size() > 1 && route.get(0).equals(route.get(route.size() - 1))) {
            route.remove(route.size() - 1);
        }

        // Build the set of unselected nodes
        Set<Integer> selected = new HashSet<>(route);
        Set<Integer> remainingNodes = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            if (!selected.contains(i)) {
                remainingNodes.add(i);
            }
        }

        // Clear the move list for a new run
        improvingMoveList.clear();
        boolean improved = true;

        while (improved) {
            improved = performSteepestStepLM(route, remainingNodes);
        }

        route.add(route.get(0)); // close route
        double finalCost = computeTotalCost(route);
        return new Result(route, finalCost);
    }

    /**
     * Performs one step of the steepest search using the LM.
     * Finds the best valid move from the list, applies it, and updates the list.
     * Returns true if an improvement was made, false otherwise.
     */
    private boolean performSteepestStepLM(List<Integer> route, Set<Integer> remainingNodes) {

        // 1. Populate LM if it's empty (first iteration or after a local optimum)
        if (improvingMoveList.isEmpty()) {
            populateMoveList(route, remainingNodes);
            if (improvingMoveList.isEmpty()) {
                return false; // No improving moves found at all
            }
        }

        // Build successor/predecessor maps for fast edge validation
        Map<Integer, Integer> succMap = buildSuccMap(route);
        Map<Integer, Integer> predMap = buildPredMap(route);

        // 2. Recheck moves in LM, best first
        improvingMoveList.sort(Comparator.comparingDouble(m -> m.delta));

        Iterator<Move> it = improvingMoveList.iterator();
        while (it.hasNext()) {
            Move move = it.next();

            // 3. Validate the move against the current solution
            ValidationResult val = validateMove(move, succMap, predMap, remainingNodes);

            if (!val.keepMove) {
                it.remove(); // Remove move, edges no longer exist
                continue;
            }

            if (!val.applyMove) {
                // Keep move, but don't apply (e.g., direction is wrong)
                continue;
            }

            // 4. Apply the move
            Set<Integer> changedNodes = applyMoveLM(route, remainingNodes, move, val);
            it.remove(); // Remove the move we just applied

            updateLocalMoves(route, remainingNodes, changedNodes, move.type);

            return true; // Found and applied the best move
        }

        return false; // No valid improving move found in the list
    }

    /**
     * Fills the improvingMoveList with all possible improving moves
     * from the current solution.
     */
    private void populateMoveList(List<Integer> route, Set<Integer> remainingNodes) {
        int n = route.size();
        improvingMoveList.clear();

        for (int i = 0; i < n; i++) {
            int nodeInCycle = route.get(i);
            for (int nodeOutOfCycle : remainingNodes) {
                double delta = deltaInter(route, i, nodeOutOfCycle);
                if (delta < -1e-9) {
                    int prev = route.get((i - 1 + n) % n);
                    int next = route.get((i + 1) % n);
                    improvingMoveList.add(Move.forInterRoute(
                            MoveType.EXCHANGE_SELECTED_UNSELECTED, delta,
                            prev, next, nodeInCycle, nodeOutOfCycle
                    ));
                }
            }
        }


        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                // This delta calculation is for 2-opt
                double delta = deltaTwoOpt(route, i, j);

                if (delta < -1e-9) {
                    int a = route.get(i);
                    int b = route.get((i + 1) % n);
                    int c = route.get(j);
                    int d = route.get((j + 1) % n);


                    if (c == b) continue; // Avoid adjacent/overlapping

                    // Add moves for both forward and reverse
                    // We store the "forward" move (A-B, C-D)
                    improvingMoveList.add(Move.forIntraEdge(MoveType.TWO_OPT, delta, a, b, c, d));
                }
            }
        }

        improvingMoveList.sort(Comparator.comparingDouble(m -> m.delta));
    }

    /**
     * Validates a move from the LM against the current solution.
     */
    private ValidationResult validateMove(Move move, Map<Integer, Integer> succMap,
                                          Map<Integer, Integer> predMap, Set<Integer> remaining) {
        switch (move.type) {
            case EXCHANGE_SELECTED_UNSELECTED:
                // A=prev, B=next, C=inCycle, D=outOfCycle
                int A_inter = move.nodeA;
                int B_inter = move.nodeB;
                int C_inter = move.nodeC;
                int D_inter = move.nodeD;

                boolean edgesExist = (hasEdge(succMap, A_inter, C_inter) || hasEdge(succMap, C_inter, A_inter)) &&
                        (hasEdge(succMap, C_inter, B_inter) || hasEdge(succMap, B_inter, C_inter));
                boolean d_exists = remaining.contains(D_inter);

                if (!edgesExist || !d_exists) {
                    return new ValidationResult(false, false, false); // Case 1: Remove
                }

                // Check for forward direction: (A -> C -> B)
                if (hasEdge(succMap, A_inter, C_inter) && hasEdge(succMap, C_inter, B_inter)) {
                    return new ValidationResult(true, true, false); // Case 3: Apply forward
                }
                // Check for reversed direction: (B -> C -> A)
                if (hasEdge(succMap, B_inter, C_inter) && hasEdge(succMap, C_inter, A_inter)) {
                    return new ValidationResult(true, true, true); // Case 3: Apply reversed
                }

                return new ValidationResult(false, true, false); // Case 2: Keep

            case TWO_OPT:
                // A=a, B=b, C=c, D=d
                int A_intra = move.nodeA;
                int B_intra = move.nodeB;
                int C_intra = move.nodeC;
                int D_intra = move.nodeD;

                boolean ab_exists = hasEdge(succMap, A_intra, B_intra) || hasEdge(succMap, B_intra, A_intra);
                boolean cd_exists = hasEdge(succMap, C_intra, D_intra) || hasEdge(succMap, D_intra, C_intra);

                if (!ab_exists || !cd_exists) {
                    return new ValidationResult(false, false, false); // Case 1: Remove
                }

                // Check for forward direction: (A -> B) and (C -> D)
                if (hasEdge(succMap, A_intra, B_intra) && hasEdge(succMap, C_intra, D_intra)) {
                    return new ValidationResult(true, true, false); // Case 3: Apply forward
                }
                // Check for reversed direction: (B -> A) and (D -> C)
                if (hasEdge(succMap, B_intra, A_intra) && hasEdge(succMap, D_intra, C_intra)) {
                    return new ValidationResult(true, true, true); // Case 3: Apply reversed
                }

                return new ValidationResult(false, true, false); // Case 2: Keep
        }
        return new ValidationResult(false, false, false);
    }

    /**
     * Applies a move that is defined by NODES, not indices.
     * This is different from the baseline 'applyMove'.
     * Returns the set of nodes that were involved in the change.
     */
    private Set<Integer> applyMoveLM(List<Integer> route, Set<Integer> remaining, Move move, ValidationResult val) {
        Set<Integer> changedNodes = new HashSet<>();

        switch (move.type) {
            case EXCHANGE_SELECTED_UNSELECTED: {
                // A=prev, B=next, C=inCycle, D=outOfCycle
                int nodeC = move.nodeC;
                int nodeD = move.nodeD;
                int idxC = route.indexOf(nodeC);

                if (idxC != -1) {
                    route.set(idxC, nodeD);
                    remaining.remove(nodeD);
                    remaining.add(nodeC);

                    changedNodes.add(move.nodeA); // prev
                    changedNodes.add(move.nodeB); // next
                    changedNodes.add(nodeC);      // old node (now in remaining)
                    changedNodes.add(nodeD);      // new node (now in route)
                }
                break;
            }

            case TWO_OPT: {
                // A=a, B=b, C=c, D=d
                int nodeA = move.nodeA;
                int nodeB = move.nodeB;
                int nodeC = move.nodeC;
                int nodeD = move.nodeD;

                int startIdx, endIdx;

                if (!val.applyReversed) {
                    // Forward: A->B, C->D. Reverse (B...C)
                    startIdx = route.indexOf(nodeB);
                    endIdx = route.indexOf(nodeC);
                } else {
                    // Reversed: B->A, D->C. Reverse (A...D)
                    startIdx = route.indexOf(nodeA);
                    endIdx = route.indexOf(nodeD);
                }

                // Handle wrap-around
                if (startIdx > endIdx) {
                    List<Integer> sublist = new ArrayList<>();
                    for(int i = startIdx; i < route.size(); i++) sublist.add(route.get(i));
                    for(int i = 0; i <= endIdx; i++) sublist.add(route.get(i));

                    Collections.reverse(sublist);

                    int k = 0;
                    for(int i = startIdx; i < route.size(); i++) route.set(i, sublist.get(k++));
                    for(int i = 0; i <= endIdx; i++) route.set(i, sublist.get(k++));

                } else {
                    reverseSublist(route, startIdx, endIdx);
                }

                changedNodes.add(nodeA);
                changedNodes.add(nodeB);
                changedNodes.add(nodeC);
                changedNodes.add(nodeD);
                break;
            }
        }
        return changedNodes;
    }

    /**
     * Re-evaluates moves involving the nodes that just changed and
     * adds new improving moves to the LM.
     */
    private void updateLocalMoves(List<Integer> route, Set<Integer> remaining,
                                  Set<Integer> changedNodes, MoveType lastMoveType) {
        int n = route.size();

        improvingMoveList.removeIf(m ->
                changedNodes.contains(m.nodeA) || changedNodes.contains(m.nodeB) ||
                        changedNodes.contains(m.nodeC) || changedNodes.contains(m.nodeD)
        );

        Set<Integer> indicesToCheck = new HashSet<>();
        for (int node : changedNodes) {
            int idx = route.indexOf(node); // Check nodes now in the route
            if (idx != -1) {
                indicesToCheck.add((idx - 1 + n) % n); // neighbor
                indicesToCheck.add(idx);               // self
                indicesToCheck.add((idx + 1) % n); // neighbor
            }
        }

        // A. Re-evaluate neighbors of affected positions with all remaining nodes
        for (int i : indicesToCheck) {
            int nodeInCycle = route.get(i);
            for (int nodeOutOfCycle : remaining) {
                double delta = deltaInter(route, i, nodeOutOfCycle);
                if (delta < -1e-9) {
                    int prev = route.get((i - 1 + n) % n);
                    int next = route.get((i + 1) % n);
                    // *** Use static factory method ***
                    improvingMoveList.add(Move.forInterRoute(
                            MoveType.EXCHANGE_SELECTED_UNSELECTED, delta,
                            prev, next, nodeInCycle, nodeOutOfCycle
                    ));
                }
            }
        }

        // B. Re-evaluate all cycle nodes with the newly available remaining node
        if (lastMoveType == MoveType.EXCHANGE_SELECTED_UNSELECTED) {
            // Find the node that was just moved from the route to 'remaining'
            int newNodeInRemaining = -1;
            for (int node : changedNodes) {
                if (remaining.contains(node)) {
                    newNodeInRemaining = node;
                    break;
                }
            }

            if (newNodeInRemaining != -1) {
                for (int i = 0; i < n; i++) {
                    int nodeInCycle = route.get(i);
                    double delta = deltaInter(route, i, newNodeInRemaining);
                    if (delta < -1e-9) {
                        int prev = route.get((i - 1 + n) % n);
                        int next = route.get((i + 1) % n);
                        improvingMoveList.add(Move.forInterRoute(
                                MoveType.EXCHANGE_SELECTED_UNSELECTED, delta,
                                prev, next, nodeInCycle, newNodeInRemaining
                        ));
                    }
                }
            }
        }
        // Find all nodes in the cycle that were changed (A, B, C, D)
        Set<Integer> changedCycleNodes = changedNodes.stream()
                .filter(node -> route.contains(node))
                .collect(Collectors.toSet());

        // Check all new pairs (A', C') where A' is a changed node
        for (int nodeA_prime : changedCycleNodes) {
            int i = route.indexOf(nodeA_prime);
            if (i == -1) continue;

            for (int j = 0; j < n; j++) {
                if (i == j) continue;

                // Ensure i < j for consistent delta calculation
                int idx_i = i, idx_j = j;
                if (idx_i > idx_j) {
                    idx_i = j; idx_j = i;
                }

                // This delta calculation is for 2-opt
                double delta = deltaTwoOpt(route, idx_i, idx_j);

                if (delta < -1e-9) {
                    int a = route.get(idx_i);
                    int b = route.get((idx_i + 1) % n);
                    int c = route.get(idx_j);
                    int d = route.get((idx_j + 1) % n);

                    if (c == b) continue;

                    // *** Use static factory method ***
                    improvingMoveList.add(Move.forIntraEdge(MoveType.TWO_OPT, delta, a, b, c, d));
                }
            }
        }
    }


    private static class ValidationResult {
        boolean applyMove;
        boolean keepMove;
        boolean applyReversed;

        ValidationResult(boolean apply, boolean keep, boolean reversed) {
            this.applyMove = apply;
            this.keepMove = keep;
            this.applyReversed = reversed;
        }
    }

    private Map<Integer, Integer> buildSuccMap(List<Integer> route) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < route.size(); i++) {
            map.put(route.get(i), route.get((i + 1) % route.size()));
        }
        return map;
    }

    private Map<Integer, Integer> buildPredMap(List<Integer> route) {
        Map<Integer, Integer> map = new HashMap<>();
        int n = route.size();
        for (int i = 0; i < n; i++) {
            map.put(route.get(i), route.get((i - 1 + n) % n));
        }
        return map;
    }

    private boolean hasEdge(Map<Integer, Integer> succMap, int u, int v) {
        return succMap.getOrDefault(u, -1) == v;
    }



    /**
     * This is the ORIGINAL 'solve' method, renamed to be the baseline.
     * It runs a standard steepest local search.
     */
    public Result solveSteepestBaseline(List<Integer> initialRoute) {
        List<Integer> route = new ArrayList<>(initialRoute);
        if (route.size() > 1 && route.get(0).equals(route.get(route.size() - 1))) {
            route.remove(route.size() - 1);
        }

        double currentCost = computeTotalCost(closed(route));
        boolean improved;
        int iteration = 0;

        do {
            improved = false;
            Move baselineBestMove = null; // Use the old Move class
            double bestDelta = 0.0;

            List<Move> neighborhood = generateNeighborhood(route);

            if (!useSteepest) Collections.shuffle(neighborhood, random);

            for (Move move : neighborhood) {
                double delta = computeDelta(route, move);
                if (delta < -1e-9) {
                    if (useSteepest) {
                        if (baselineBestMove == null || delta < bestDelta) {
                            baselineBestMove = move;
                            bestDelta = delta;
                        }
                    } else {
                        applyMove(route, move);
                        currentCost += delta;
                        improved = true;
                        break;
                    }
                }
            }

            if (useSteepest && baselineBestMove != null) {
                applyMove(route, baselineBestMove);
                currentCost += bestDelta;
                improved = true;
            }

            iteration++;

        } while (improved);

        route.add(route.get(0));
        double finalCost = computeTotalCost(route);
        return new Result(route, finalCost);
    }

    /** Generate combined intra- and inter-route neighborhood (FOR BASELINE) */
    private List<Move> generateNeighborhood(List<Integer> route) {
        List<Move> moves = new ArrayList<>();
        int n = route.size();

        if (useNodeExchange) {
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    moves.add(new Move(MoveType.SWAP_NODES, i, j));
                }
            }
        } else { // 2-opt
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    moves.add(new Move(MoveType.TWO_OPT, i, j));
                }
            }
        }

        Set<Integer> selected = new HashSet<>(route);
        for (int i = 0; i < n; i++) {
            for (int node = 0; node < nodes.size(); node++) {
                if (!selected.contains(node)) {
                    moves.add(new Move(MoveType.EXCHANGE_SELECTED_UNSELECTED, i, node));
                }
            }
        }

        return moves;
    }

    /** Compute delta change for given move (FOR BASELINE) */
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

        // Handle adjacent swap (which is 2-opt, but in node-swap logic)
        if (i + 1 == j || (i == n-1 && j == 0)) {
            // Swap i and j if j < i
            if (i > j) { int temp = i; i = j; j = temp; }

            int ni = route.get(i);
            int nj = route.get(j);
            int iPrev = route.get((i - 1 + n) % n);
            int jNext = route.get((j + 1) % n);

            double before = distanceMatrix[iPrev][ni] + distanceMatrix[ni][nj] + distanceMatrix[nj][jNext];
            double after = distanceMatrix[iPrev][nj] + distanceMatrix[nj][ni] + distanceMatrix[ni][jNext];
            return after - before;
        }

        int ni = route.get(i);
        int nj = route.get(j);
        int iPrev = route.get((i - 1 + n) % n);
        int iNext = route.get((i + 1) % n);
        int jPrev = route.get((j - 1 + n) % n);
        int jNext = route.get((j + 1) % n);

        double before = distanceMatrix[iPrev][ni] + distanceMatrix[ni][iNext] +
                distanceMatrix[jPrev][nj] + distanceMatrix[nj][jNext];
        double after = distanceMatrix[iPrev][nj] + distanceMatrix[nj][iNext] +
                distanceMatrix[jPrev][ni] + distanceMatrix[ni][jNext];

        return after - before;
    }

    /** * Delta for edge reversal
     */
    private double deltaTwoOpt(List<Integer> route, int i, int j) {
        int n = route.size();
        // Ensure i < j
        if (i > j) { int temp = i; i = j; j = temp; }


        int a = route.get(i);
        int b = route.get((i + 1) % n);
        int c = route.get(j);
        int d = route.get((j + 1) % n);

        double before = distanceMatrix[a][b] + distanceMatrix[c][d];
        double after = distanceMatrix[a][c] + distanceMatrix[b][d];
        return after - before;
    }

    /**
     * Delta for inter-route (selected/unselected) swap.
     */
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

    /** Apply the move (FOR BASELINE) */
    private void applyMove(List<Integer> route, Move move) {
        switch (move.type) {
            case SWAP_NODES:
                Collections.swap(route, move.i, move.j);
                break;
            case TWO_OPT:
                // Ensure i < j
                int i = move.i;
                int j = move.j;
                if (i > j) { int temp = i; i = j; j = temp; }
                reverseSublist(route, i + 1, j);
                break;
            case EXCHANGE_SELECTED_UNSELECTED:
                route.set(move.i, move.j);
                break;
        }
    }

    /** Reverse a sublist (for edge exch) */
    private void reverseSublist(List<Integer> route, int start, int end) {
        while (start < end) {
            Collections.swap(route, start++, end--);
        }
    }

    /** Close the route */
    private List<Integer> closed(List<Integer> route) {
        if (route.isEmpty()) return new ArrayList<>();
        List<Integer> r = new ArrayList<>(route);
        r.add(route.get(0));
        return r;
    }



    /**
     * MoveType is simplified. For the LM search, we only use
     * TWO_OPT (intra-edge) and EXCHANGE_SELECTED_UNSELECTED (inter-node).
     * SWAP_NODES is only for the baseline.
     */
    private enum MoveType {
        SWAP_NODES, // Intra-node (Baseline only)
        TWO_OPT,    // Intra-edge
        EXCHANGE_SELECTED_UNSELECTED // Inter-node
    }

    /**
     * Move class with static factory methods
     * to resolve constructor ambiguity.
     */
    private static class Move {
        MoveType type;
        double delta;
        int i, j; // For baseline
        int nodeA, nodeB, nodeC, nodeD; // For LM search

        // Private constructor to be used by factory methods and baseline
        private Move(MoveType type, double delta) {
            this.type = type;
            this.delta = delta;
        }

        // Constructor for BASELINE moves
        Move(MoveType type, int i, int j) {
            this(type, 0.0); // Call private constructor, delta not needed
            this.i = i;
            this.j = j;
        }



        // Factory for LM 2-opt (intra-edge)
        // (A,B) and (C,D) are the edges to be removed
        public static Move forIntraEdge(MoveType type, double delta, int nodeA, int nodeB, int nodeC, int nodeD) {
            Move move = new Move(type, delta);
            move.nodeA = nodeA; // A
            move.nodeB = nodeB; // B (successor of A)
            move.nodeC = nodeC; // C
            move.nodeD = nodeD; // D (successor of C)
            return move;
        }

        // Factory for LM inter-route
        // (A,C) and (C,B) are edges to be removed
        public static Move forInterRoute(MoveType type, double delta, int prev, int next, int nodeInCycle, int nodeOutOfCycle) {
            Move move = new Move(type, delta);
            move.nodeA = prev;           // A (prev)
            move.nodeB = next;           // B (next)
            move.nodeC = nodeInCycle;    // C (node to swap out)
            move.nodeD = nodeOutOfCycle; // D (node to swap in)
            return move;
        }
    }
}