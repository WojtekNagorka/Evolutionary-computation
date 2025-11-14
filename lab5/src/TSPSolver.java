import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TSPSolver {

    protected final double[][] distanceMatrix;
    protected final List<Node> nodes;
    protected final int targetCount;

    public TSPSolver(double[][] distanceMatrix, List<Node> nodes) {
        this.distanceMatrix = distanceMatrix;
        this.nodes = nodes;
        this.targetCount = Math.max(2, (int) Math.ceil(nodes.size() / 2.0));
    }

    protected int countUsed(boolean[] used) {
        int count = 0;
        for (boolean b : used) if (b) count++;
        return count;
    }

    protected double computeTotalCost(List<Integer> route) {
        double cost = 0.0;
        for (int i = 0; i < route.size() - 1; i++) {
            int a = route.get(i);
            int b = route.get(i + 1);
            cost += distanceMatrix[a][b] + nodes.get(a).getCost();
        }
        return cost;
    }

    protected int roundToInt(double value){
        return (int) Math.round(value);
    }
}