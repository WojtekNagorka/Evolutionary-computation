import java.util.*;

public class Greedy2RegretHeuristicFlexibleNN extends TSPSolver {

    private final double regretWeight;
    private final int k; // number of deltas to consider for regret (in our case 2)

    public Greedy2RegretHeuristicFlexibleNN(double[][] distanceMatrix, List<Node> nodes, int k, double regretWeight) {
        super(distanceMatrix, nodes);
        this.k = k;
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

            // for every unused node
            for (int j = 0; j < n; j++) {
                if (used[j]) continue;

                Node curr = nodes.get(j);
                List<Double> deltas = new ArrayList<>();

                // compute insertion delta for every possible position (including before first and after last)
                for (int pos = 0; pos <= route.size(); pos++) {
                    double delta;

                    if (pos == 0) {
                        // insert before first node
                        int kNode = route.get(0);
                        delta = distanceMatrix[j][kNode] + curr.getCost();
                    } else if (pos == route.size()) {
                        // insert after last node
                        int i = route.get(route.size() - 1);
                        delta = distanceMatrix[i][j] + curr.getCost();
                    } else {
                        // insert between i and k
                        int i = route.get(pos - 1);
                        int kNode = route.get(pos);
                        delta = distanceMatrix[i][j] + distanceMatrix[j][kNode]
                                - distanceMatrix[i][kNode] + curr.getCost();
                    }

                    deltas.add(delta);
                }

                // sort deltas ascending to get best and k-th best insertions
                List<Double> sorted = new ArrayList<>(deltas);
                Collections.sort(sorted);

                double bestDelta = sorted.get(0);
                double regret = 0.0;

                for (int m = 1; m < Math.min(k, sorted.size()); m++) {
                    regret += (sorted.get(m) - bestDelta);
                }

                // Weighted-sum criterion (mix of regret and cost improvement)
                double score = regretWeight * regret - (1 - regretWeight) * bestDelta;

                if (score > bestScore) {
                    bestScore = score;
                    bestNode = j;
                    bestPos = deltas.indexOf(bestDelta);
                }
            }

            // insert node with best weighted score
            route.add(bestPos, bestNode);
            used[bestNode] = true;
        }
        route.add(route.get(0));
        double totalCost = computeTotalCost(route);
        return new Result(route, totalCost);
    }
}
