import java.util.ArrayList;
import java.util.List;

public class Result {
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