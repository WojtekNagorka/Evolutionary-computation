import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LargeNeighborhoodSearch extends TSPSolver{
    private final boolean useLocalSearch;
    private final int maxTimeMs;

    public LargeNeighborhoodSearch(double[][] distanceMatrix, List<Node> nodes, boolean useLocalSearch, int maxTimeMs) {
        super(distanceMatrix, nodes);
        this.useLocalSearch = useLocalSearch;
        this.maxTimeMs = maxTimeMs;
    }

    public Result solve(){
        long startTime = System.currentTimeMillis();

        Result route = generateRandomSolution();
        LocalSearch localSearch = new LocalSearch(distanceMatrix, nodes, true, false);

        if (useLocalSearch){
            route = localSearch.solve(route.getRoute());
        }
        while (System.currentTimeMillis() - startTime < maxTimeMs){
            List<Integer> destroyedRoute = destroy(route.getRoute());
            Result newRoute = repair(destroyedRoute);
            if (useLocalSearch){
                newRoute = localSearch.solve(newRoute.getRoute());
            }

            if (newRoute.getTotalCost() > route.getTotalCost()){
                route = newRoute;
            }
        }
        return route;
    }

    public Result generateRandomSolution(){
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

    public List<Integer> destroy(List<Integer> route){
        int nodesToDestroy = (int) (0.3 * targetCount);
        route.removeLast();

        for (int i=0; i < nodesToDestroy; i++){
            int randomIdx = (int)(Math.random() * route.size());
            route.remove(randomIdx);
        }
        return route;
    }

    public Result repair(List<Integer> route){
        ///
    }
}
