import java.util.*;

public class Greedy2RegretHeuristicFlexibleNN extends TSPSolver {

    private final double regretWeight;

    public Greedy2RegretHeuristicFlexibleNN(double[][] distanceMatrix, List<Node> nodes, double regretWeight) {
        super(distanceMatrix, nodes);
        this.regretWeight = regretWeight;
    }

    public Result solve(int startIndex) {
        int n = nodes.size();
        List<Integer> route = new ArrayList<>();
        boolean[] used = new boolean[n];

        // start with single node
        route.add(startIndex);
        used[startIndex] = true;

        // continue until reaching targetCount nodes
        while (route.size() < targetCount) {
            int bestNode = -1;
            int bestPos = -1;
            double bestScore = Double.NEGATIVE_INFINITY;
            double bestDeltaForBestNode = Double.POSITIVE_INFINITY;

            // evaluate all unused nodes
            for (int j = 0; j < n; j++) {
                if (used[j]) continue;

                Node curr = nodes.get(j);
                List<Double> deltas = new ArrayList<>();

                // compute insertion delta for all possible positions (before first, between, after last)
                for (int pos = 0; pos <= route.size(); pos++) {
                    double delta;
                    if (pos == 0) {
                        int kNode = route.get(0);
                        delta = distanceMatrix[j][kNode] + curr.getCost();
                    } else if (pos == route.size()) {
                        int i = route.get(route.size() - 1);
                        delta = distanceMatrix[i][j] + curr.getCost();
                    } else {
                        int i = route.get(pos - 1);
                        int kNode = route.get(pos);
                        delta = distanceMatrix[i][j] + distanceMatrix[j][kNode]
                                - distanceMatrix[i][kNode] + curr.getCost();
                    }
                    deltas.add(delta);
                }

                // sort deltas ascending (smallest = best insertion position)
                List<Double> sorted = new ArrayList<>(deltas);
                Collections.sort(sorted);

                // take the two best deltas
                double bestDelta = sorted.get(0);
                double secondBestDelta = sorted.size() > 1 ? sorted.get(1) : sorted.get(0);
                double regret = secondBestDelta - bestDelta;

                double alpha = regretWeight;
                double beta = 1.0 - regretWeight;
                double score = alpha * regret + beta * (-bestDelta);

                // tie-breaking: prefer smaller bestDelta when scores equal
                if (score > bestScore || (score == bestScore && bestDelta < bestDeltaForBestNode)) {
                    bestScore = score;
                    bestNode = j;
                    bestPos = deltas.indexOf(bestDelta);
                    bestDeltaForBestNode = bestDelta;
                }
            }

            // insert node with best score
            route.add(bestPos, bestNode);
            used[bestNode] = true;
        }

        // close the route (return to start)
        route.add(route.get(0));

        double totalCost = computeTotalCost(route);
        return new Result(route, totalCost);
    }
}
