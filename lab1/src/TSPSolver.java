package lab1.src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TSPSolver {

    private final double[][] distanceMatrix;
    private final List<Node> nodes;
    private final int targetCount;

    public TSPSolver(double[][] distanceMatrix, List<Node> nodes) {
        this.distanceMatrix = distanceMatrix;
        this.nodes = nodes;
        this.targetCount = Math.max(2, (int) Math.ceil(nodes.size() / 2.0));
    }

    // ================================
    // 1 RANDOM SOLUTION
    // ================================
    public Result randomSolution() {
        List<Integer> route = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) route.add(i);
        Collections.shuffle(route, new Random());

        // only keep 50% of nodes
        route = route.subList(0, targetCount);

        // close the cycle
        route.add(route.getFirst());

        double totalCost = computeTotalCost(route);
        return new Result(new ArrayList<>(route), totalCost);
    }

    // ================================
    // 2 NEAREST NEIGHBOR (ADD AT END)
    // ================================
    public Result nearestNeighborEnd(int startIndex) {
        int n = nodes.size();
        List<Integer> route = new ArrayList<>();
        boolean[] used = new boolean[n];
        route.add(startIndex);
        used[startIndex] = true;
        int current = startIndex;

        while (route.size() < targetCount) {
            double bestDist = Double.MAX_VALUE;
            int next = -1;
            for (int i = 0; i < n; i++) {
                if (!used[i]) {
                    double d = distanceMatrix[current][i] + nodes.get(i).getCost();
                    if (d < bestDist) {
                        bestDist = d;
                        next = i;
                    }
                }
            }
            if (next == -1) break;
            route.add(next);
            used[next] = true;
            current = next;
        }

        route.add(startIndex);
        double totalCost = computeTotalCost(route);
        return new Result(route, totalCost);
    }

    // ================================
    // 3 NEAREST NEIGHBOR (FLEXIBLE INSERTION)
    // ================================
    public Result nearestNeighborFlexible(int startIndex) {
        int n = nodes.size();
        List<Integer> route = new ArrayList<>();
        route.add(startIndex);
        route.add(startIndex); // start and end same (cycle)
        boolean[] used = new boolean[n];
        used[startIndex] = true;

        while (countUsed(used) < targetCount) {
            double bestIncrease = Double.MAX_VALUE;
            int bestNode = -1;
            int bestPos = -1;

            for (int node = 0; node < n; node++) {
                if (!used[node]) {
                    // try inserting this node at all positions
                    for (int pos = 0; pos < route.size() - 1; pos++) {
                        int a = route.get(pos);
                        int b = route.get(pos + 1);
                        double increase = distanceMatrix[a][node] + distanceMatrix[node][b] - distanceMatrix[a][b] + nodes.get(node).getCost();
                        if (increase < bestIncrease) {
                            bestIncrease = increase;
                            bestNode = node;
                            bestPos = pos + 1;
                        }
                    }
                }
            }

            if (bestNode == -1) break;
            route.add(bestPos, bestNode);
            used[bestNode] = true;
        }

        double totalCost = computeTotalCost(route);
        return new Result(route, totalCost);
    }

    // ================================
    // 4 GREEDY CYCLE
    // ================================
    public Result greedyCycle(int startIndex) {
        int n = nodes.size();
        int numToSelect = Math.max(2, (int) Math.ceil(n / 2.0));

        List<Integer> route = new ArrayList<>();
        boolean[] used = new boolean[n];
        used[startIndex] = true;
        route.add(startIndex);

        int bestSecond = -1;
        double minObj = Double.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            if (!used[i]) {
                double obj = 2 * distanceMatrix[startIndex][i]
                        + nodes.get(startIndex).getCost()
                        + nodes.get(i).getCost();
                if (obj < minObj) {
                    minObj = obj;
                    bestSecond = i;
                }
            }
        }

        if (bestSecond == -1){
            return new Result(route, 0);
        }
        route.add(bestSecond);
        used[bestSecond] = true;

        // Iteratively insert nodes
        while (route.size() < numToSelect) {
            int bestNode = -1;
            int bestPos = -1;
            double bestIncrease = Double.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                if (!used[i]) {
                    for (int j = 0; j < route.size(); j++) {
                        int a = route.get(j);
                        int b = route.get((j + 1) % route.size());
                        double increase = distanceMatrix[a][i]
                                + distanceMatrix[i][b]
                                - distanceMatrix[a][b]
                                + nodes.get(i).getCost();

                        if (increase < bestIncrease) {
                            bestIncrease = increase;
                            bestNode = i;
                            bestPos = j + 1;
                        }
                    }
                }
            }

            if (bestNode == -1){
                break;
            }
            route.add(bestPos, bestNode);
            used[bestNode] = true;
        }
        route.add(route.getFirst());

        double totalCost = computeTotalCost(route);
        return new Result(route, totalCost);
    }

    // ================================
    // Utility Methods
    // ================================
    private int countUsed(boolean[] used) {
        int count = 0;
        for (boolean b : used) if (b) count++;
        return count;
    }

//    private int closestUnused(int index, boolean[] used) {
//        double best = Double.MAX_VALUE;
//        int next = -1;
//        for (int i = 0; i < nodes.size(); i++) {
//            if (!used[i] && i != index) {
//                double d = distanceMatrix[index][i];
//                if (d < best) {
//                    best = d;
//                    next = i;
//                }
//            }
//        }
//        return next == -1 ? index : next;
//    }

    private double computeTotalCost(List<Integer> route) {
        double cost = 0.0;
        for (int i = 0; i < route.size() - 1; i++) {
            int a = route.get(i);
            int b = route.get(i + 1);
            cost += distanceMatrix[a][b] + nodes.get(a).getCost();
        }
        return cost;
    }

    // ================================
    // Result Class
    // ================================
    public static class Result {
        private final List<Integer> route;
        private final double totalCost;

        public Result(List<Integer> route, double totalCost) {
            this.route = route;
            this.totalCost = totalCost;
        }

        public List<Integer> getRoute() {
            return route;
        }

        public double getTotalCost() {
            return totalCost;
        }

        @Override
        public String toString() {
            return "Route: " + route + "\nTotal cost: " + String.format("%.2f", totalCost);
        }
    }
}
