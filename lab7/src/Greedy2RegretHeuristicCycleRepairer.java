import java.util.*;

public class Greedy2RegretHeuristicCycleRepairer extends TSPSolver {

    private final double regretWeight;
    private final int k; // how many top deltas to use for regret (in our case 2)

    public Greedy2RegretHeuristicCycleRepairer(double[][] distanceMatrix, List<Node> nodes, int k, double regretWeight) {
        super(distanceMatrix, nodes);
        this.k = k;
        this.regretWeight = regretWeight;
    }

    public Result repair(List<Integer> route) {
        int n = nodes.size();
        boolean[] used = new boolean[n];

        // populate used array
        for (Integer idx: route){
            used[idx] = true;
        }

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
        route.add(route.getFirst());
        double totalCost = computeTotalCost(route);
        return new Result(route, totalCost);
    }

}
