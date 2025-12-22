import java.util.*;

public class ILSWithSimulatedAnnealing extends TSPSolver {

    private final int maxTimeMs;
    private final Random random;

    // Track the number of iterations
    private int numberOfIterations = 0;

    private final double coolingRate = 0.9999;
    private final double minTemperature = 1.0;
    private final double typicalDelta = 50.0; // Expected degradation (delta) for a move
    private final double acceptanceProbability = 0.01; // Probability to accept a worse solution initially
    private final int perturbationSize = 3;

    public ILSWithSimulatedAnnealing(double[][] distanceMatrix, List<Node> nodes, int maxTimeMs) {
        super(distanceMatrix, nodes);
        this.maxTimeMs = maxTimeMs;
        this.random = new Random();
    }

    public Result solve() {
        long startTime = System.currentTimeMillis();
        numberOfIterations = 0;

        // 1. Generate Initial Solution (x)
        List<Integer> currentRoute = generateRandomRoute();

        LocalSearch localSearch = new LocalSearch(distanceMatrix, nodes, true, false);

        // 2. Initial Descent (baseLS.solve)
        Result currentResult = localSearch.solve(currentRoute);
        Result bestResult = currentResult;

        // Clean up route for perturbation
        List<Integer> currentSolRoute = new ArrayList<>(currentResult.getRoute());
        if(currentSolRoute.size() > 1 && currentSolRoute.get(0).equals(currentSolRoute.get(currentSolRoute.size()-1))) {
            currentSolRoute.remove(currentSolRoute.size()-1);
        }

        double temperature = -typicalDelta / Math.log(acceptanceProbability);

        // 3. Iteration Loop
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            numberOfIterations++;

            // A. Perturbation (y <- PERTURB(x))
            List<Integer> perturbedRoute = new ArrayList<>(currentSolRoute);
            applyPerturbation(perturbedRoute, perturbationSize); // Apply k moves

            // B. Local Search (y <- baseLS.solve(y))
            Result newResult = localSearch.solve(perturbedRoute);

            // C. Acceptance Criterion
            double currentCost = currentResult.getTotalCost(); // xCost
            double newCost = newResult.getTotalCost();         // yCost
            double delta = newCost - currentCost;

            boolean accept = false;

            if (delta < 0) {
                accept = true; // Always accept better
            } else {
                // Accept worse solution with probability exp(-delta / temperature)
                double probability = Math.exp(-delta / temperature);
                if (random.nextDouble() < probability) {
                    accept = true;
                }
            }

            if (accept) {
                currentResult = newResult;
                currentSolRoute = new ArrayList<>(newResult.getRoute());

                // Maintain clean route format
                if(currentSolRoute.size() > 1 && currentSolRoute.get(0).equals(currentSolRoute.get(currentSolRoute.size()-1))) {
                    currentSolRoute.remove(currentSolRoute.size()-1);
                }
            }

            // Update Global Best
            if (newResult.getTotalCost() < bestResult.getTotalCost()) {
                bestResult = newResult;
            }

            // D. Cooling
            temperature = Math.max(temperature * coolingRate, minTemperature);
        }

        return bestResult;
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }

    /**
     * Applies perturbation: k random 2-opt moves + Node Swapping (for subset selection)
     */
    private void applyPerturbation(List<Integer> route, int k) {
        int n = route.size();
        if (n < 4) return;

        for (int m = 0; m < k; m++) {
            int idx1 = random.nextInt(n - 1);     // 0 to n-2
            int idx2 = random.nextInt(n);         // 0 to n-1

            // Make sure idx1 < idx2
            if (idx1 > idx2) {
                int temp = idx1;
                idx1 = idx2;
                idx2 = temp;
            } else if (idx1 == idx2) {
                if (idx2 < n - 1) idx2++;
                else if (idx1 > 0) idx1--;
            }


            reverseSublist(route, idx1, idx2);
        }

        Set<Integer> inRoute = new HashSet<>(route);
        List<Integer> unselected = new ArrayList<>();
        for(int i=0; i<nodes.size(); i++) {
            if(!inRoute.contains(i)) unselected.add(i);
        }

        if (!unselected.isEmpty()) {
            int swaps = Math.min(2, unselected.size());
            for (int m = 0; m < swaps; m++) {
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