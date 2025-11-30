import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LargeNeighborhoodSearch extends TSPSolver{
    private final boolean useLocalSearch;
    private final int maxTimeMs;
    private int nr_of_iterations;
    private final boolean useCycle;

    public LargeNeighborhoodSearch(double[][] distanceMatrix, List<Node> nodes, boolean useLocalSearch, int maxTimeMs, boolean useCycle) {
        super(distanceMatrix, nodes);
        this.useLocalSearch = useLocalSearch;
        this.maxTimeMs = maxTimeMs;
        this.useCycle = useCycle;
    }

    public Result solve(){
        long startTime = System.currentTimeMillis();

        Result route = generateRandomSolution();
        LocalSearch localSearch = new LocalSearch(distanceMatrix, nodes, true, false);
        route = localSearch.solve(route.getRoute());

        nr_of_iterations=0;
        while (System.currentTimeMillis() - startTime < maxTimeMs){
            nr_of_iterations += 1;
            List<Integer> destroyedRoute = destroy(route.getRoute());
            Result newRoute = repair(destroyedRoute);
            if (useLocalSearch){
                newRoute = localSearch.solve(newRoute.getRoute());
            }

            if (newRoute.getTotalCost() < route.getTotalCost()){
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
        if (useCycle) {
            Greedy2RegretHeuristicCycleRepairer repairer = new Greedy2RegretHeuristicCycleRepairer(
                    distanceMatrix, nodes, 2, 0.5
            );
            return repairer.repair(route);
        }
        Greedy2RegretHeuristicFlexibleNNRepairer repairer = new Greedy2RegretHeuristicFlexibleNNRepairer(
                distanceMatrix, nodes, 0.5
        );
        return repairer.repair(route);
    }

    public int getNumberOfIterations(){
        return nr_of_iterations;
    }
}
