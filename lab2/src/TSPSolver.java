import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TSPSolver {

    private final double[][] distanceMatrix;
    private final List<Node> nodes;
    private final int targetCount;

    public TSPSolver(double[][] distanceMatrix, List<Node> nodes) {
        this.distanceMatrix = distanceMatrix;
        this.nodes = nodes;
        this.targetCount = Math.max(2, (int) Math.ceil(nodes.size() / 2.0));
    }

    private int countUsed(boolean[] used) {
        int count = 0;
        for (boolean b : used) if (b) count++;
        return count;
    }

    private double computeTotalCost(List<Integer> route) {
        double cost = 0.0;
        for (int i = 0; i < route.size() - 1; i++) {
            int a = route.get(i);
            int b = route.get(i + 1);
            cost += distanceMatrix[a][b] + nodes.get(a).getCost();
        }
        return cost;
    }
}