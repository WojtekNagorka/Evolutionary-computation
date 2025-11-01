import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomSearch extends TSPSolver{
    public RandomSearch(double[][] distanceMatrix, List<Node> nodes){
        super(distanceMatrix, nodes);
    }

    public Result solve(){
        List<Integer> route = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) route.add(i);
        Collections.shuffle(route, new Random());

        route = route.subList(0, targetCount);
        route.add(route.getFirst());

        double totalCost = computeTotalCost(route);
        return new Result(new ArrayList<>(route), totalCost);
    }
}
