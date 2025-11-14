import java.util.*;

public class Greedy2RegretHeuristicCycle extends TSPSolver {

    private final double regretWeight;
    private final int k; // how many top deltas to use for regret (in our case 2)

    public Greedy2RegretHeuristicCycle(double[][] distanceMatrix, List<Node> nodes, int k, double regretWeight) {
        super(distanceMatrix, nodes);
        this.k = k;
        this.regretWeight = regretWeight;
    }

    public Result solve(int startIndex) {
        int n = nodes.size();
        List<Integer> route = new ArrayList<>();
        boolean[] used = new boolean[n];

        // start with best pair
        route.add(startIndex);
        used[startIndex] = true;

        int bestSecond = -1;
        double bestVal = Double.POSITIVE_INFINITY;
        for (int j = 0; j < n; j++) {
            if (used[j]) continue;
            double val = distanceMatrix[startIndex][j] + nodes.get(j).getCost();
            if (val < bestVal) {
                bestVal = val;
                bestSecond = j;
            }
        }

        route.add(bestSecond);
        used[bestSecond] = true;

        // build the cycle
        while (route.size() < targetCount) {
            int bestNode = -1;
            int bestPos = -1;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (int j = 0; j < n; j++) {
                if (used[j]) continue;
                Node curr = nodes.get(j);
                List<Double> deltas = new ArrayList<>();

                // include wrap-around edge
                for (int pos = 0; pos < route.size(); pos++) {
                    int i = route.get(pos);
                    int kNode = route.get((pos + 1) % route.size());
                    double delta = distanceMatrix[i][j] + distanceMatrix[j][kNode]
                            - distanceMatrix[i][kNode] + curr.getCost();
                    deltas.add(delta);
                }

                List<Double> sorted = new ArrayList<>(deltas);
                Collections.sort(sorted);

                double bestDelta = sorted.get(0);
                double regret = 0.0;
                for (int m = 1; m < Math.min(k, sorted.size()); m++) {
                    regret += (sorted.get(m) - bestDelta);
                }

                double score = regretWeight * regret - (1 - regretWeight) * bestDelta;

                if (score > bestScore) {
                    bestScore = score;
                    bestNode = j;
                    bestPos = deltas.indexOf(bestDelta) + 1;
                }
            }

            route.add(bestPos, bestNode);
            used[bestNode] = true;
        }
        route.add(startIndex);
        double totalCost = computeTotalCost(route);
        return new Result(route, totalCost);
    }

}
