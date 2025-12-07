import java.util.*;

public class ILS extends TSPSolver {

    private final int maxTimeMs;
    private final Random random;

    public ILS(double[][] distanceMatrix, List<Node> nodes, int maxTimeMs) {
        super(distanceMatrix, nodes);
        this.maxTimeMs = maxTimeMs;
        this.random = new Random();
    }

    public Result solve() {
        long startTime = System.currentTimeMillis();

        // 1. Generate Initial Solution
        List<Integer> currentRoute = generateRandomRoute();

        LocalSearch localSearch = new LocalSearch(distanceMatrix, nodes, true, false);

        // 2. Initial Descent
        Result currentResult = localSearch.solve(currentRoute);
        Result bestResult = currentResult;


        List<Integer> currentSolRoute = new ArrayList<>(currentResult.getRoute());
        if(currentSolRoute.size() > 1 && currentSolRoute.get(0).equals(currentSolRoute.get(currentSolRoute.size()-1))) {
            currentSolRoute.remove(currentSolRoute.size()-1);
        }

        // 3. Iteration Loop
        while (System.currentTimeMillis() - startTime < maxTimeMs) {

            // A. Perturbation
            List<Integer> perturbedRoute = new ArrayList<>(currentSolRoute);
            applyPerturbation(perturbedRoute);

            // B. Local Search
            Result newResult = localSearch.solve(perturbedRoute);

            // C. Acceptance Criterion
            // FIXED: used .getTotalCost()
            if (newResult.getTotalCost() < currentResult.getTotalCost()) {
                currentResult = newResult;
                currentSolRoute = new ArrayList<>(newResult.getRoute());

                if(currentSolRoute.size() > 1 && currentSolRoute.get(0).equals(currentSolRoute.get(currentSolRoute.size()-1))) {
                    currentSolRoute.remove(currentSolRoute.size()-1);
                }

                if (newResult.getTotalCost() < bestResult.getTotalCost()) {
                    bestResult = newResult;
                }
            }
        }

        return bestResult;
    }

    private void applyPerturbation(List<Integer> route) {
        int n = route.size();
        if (n < 4) return;

        // (2 random 2-opt moves)
        for (int k = 0; k < 2; k++) {
            int i = random.nextInt(n - 2);
            int j = i + 2 + random.nextInt(n - (i + 2));
            reverseSublist(route, i + 1, j);
        }

        // (Swap Selected <-> Unselected)
        Set<Integer> inRoute = new HashSet<>(route);
        List<Integer> unselected = new ArrayList<>();
        for(int i=0; i<nodes.size(); i++) {
            if(!inRoute.contains(i)) unselected.add(i);
        }

        if (!unselected.isEmpty()) {
            int swaps = Math.min(2, unselected.size());
            for (int k = 0; k < swaps; k++) {
                int routeIdx = random.nextInt(route.size());
                int poolIdx = random.nextInt(unselected.size());

                Integer oldNode = route.get(routeIdx);
                Integer newNode = unselected.get(poolIdx);

                route.set(routeIdx, newNode);
                unselected.set(poolIdx, oldNode);
            }
        }
    }

    private void reverseSublist(List<Integer> route, int start, int end) {
        while (start < end) {
            Collections.swap(route, start++, end--);
        }
    }

    private List<Integer> generateRandomRoute() {
        List<Integer> allIndices = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) allIndices.add(i);
        Collections.shuffle(allIndices, random);
        int routeSize = (int)(nodes.size() * 0.5);
        return new ArrayList<>(allIndices.subList(0, routeSize));
    }
}