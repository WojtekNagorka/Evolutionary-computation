import java.util.*;

public class MSLS extends TSPSolver {

    private final int iterations;
    private final Random random;

    public MSLS(double[][] distanceMatrix, List<Node> nodes, int iterations) {
        super(distanceMatrix, nodes);
        this.iterations = iterations;
        this.random = new Random();
    }

    public Result solve() {
        Result bestResult = null;

        // Instantiate Local Search (Steepest = true, NodeExchange = false -> 2-opt)
        LocalSearch localSearch = new LocalSearch(distanceMatrix, nodes, true, false);

        for (int i = 0; i < iterations; i++) {
            // 1. Generate random start
            List<Integer> randomRoute = generateRandomRoute();

            // 2. Apply Local Search
            Result currentResult = localSearch.solve(randomRoute);

            // 3. Update Best Found
            if (bestResult == null || currentResult.getTotalCost() < bestResult.getTotalCost()) {
                bestResult = currentResult;
            }
        }
        return bestResult;
    }

    private List<Integer> generateRandomRoute() {
        List<Integer> allIndices = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) allIndices.add(i);
        Collections.shuffle(allIndices, random);

        int routeSize = (int)(nodes.size() * 0.5);
        return new ArrayList<>(allIndices.subList(0, routeSize));
    }
}