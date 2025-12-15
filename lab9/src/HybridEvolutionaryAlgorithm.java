import java.util.*;

public class HybridEvolutionaryAlgorithm extends TSPSolver {

    private final int maxTimeMs;
    private final int populationSize = 20; // STRICT: Elite population of 20
    private final Random random;

    // Algorithm configuration
    private final boolean useOperator2;
    private final boolean useLocalSearchAfterRecombination;

    private final double MUTATION_RATE = 0.30;
    private final double CONVERGENCE_THRESHOLD = 5.0; // Trigger restart if best/worst gap is small

    public HybridEvolutionaryAlgorithm(double[][] distanceMatrix, List<Node> nodes, int maxTimeMs,
                                       boolean useOperator2, boolean useLocalSearchAfterRecombination) {
        super(distanceMatrix, nodes);
        this.maxTimeMs = maxTimeMs;
        this.random = new Random();
        this.useOperator2 = useOperator2;
        this.useLocalSearchAfterRecombination = useLocalSearchAfterRecombination;
    }

    public Result solve() {
        long startTime = System.currentTimeMillis();
        LocalSearch localSearch = new LocalSearch(distanceMatrix, nodes, true, false);

        // --- 1. Initialization (Steady State) ---
        List<Result> population = new ArrayList<>();

        // Fill initial population (ALWAYS use LS for initialization per requirements)
        fillPopulation(population, localSearch, startTime);

        // --- 2. Evolutionary Loop ---
        while (System.currentTimeMillis() - startTime < maxTimeMs) {

            // A. DIVERSIFICATION: Cataclysm (Restart)
            // If population is converged, keep the Best, wipe the rest, and refill.
            if (population.size() >= populationSize && isConverged(population)) {
                performCataclysm(population, localSearch, startTime);
            }
            if (System.currentTimeMillis() - startTime >= maxTimeMs) break;

            // B. Parent Selection (Uniform Probability)
            if (population.size() < 2) break;
            Result parent1 = population.get(random.nextInt(population.size()));
            Result parent2 = population.get(random.nextInt(population.size()));

            // Try to ensure distinct parents
            int tries = 0;
            while (parent1 == parent2 && tries < 10) {
                parent2 = population.get(random.nextInt(population.size()));
                tries++;
            }

            // C. Recombination
            List<Integer> offspringRoute;
            if (useOperator2) {
                offspringRoute = recombinationOperator2(parent1.getRoute(), parent2.getRoute());
            } else {
                offspringRoute = recombinationOperator1(parent1.getRoute(), parent2.getRoute());
            }

            // D. Mutation (Diversification)
            // We apply a kick (Double-Bridge) to avoid getting stuck in the same valley
            if (random.nextDouble() < MUTATION_RATE) {
                applyDoubleBridgeMutation(offspringRoute);
            }

            // E. Improvement / Evaluation
            Result offspringResult;
            if (useLocalSearchAfterRecombination) {
                // Version 1: Apply Local Search to offspring
                offspringResult = localSearch.solve(offspringRoute);
            } else {
                // Version 2: NO Local Search after recombination
                // Must ensure cycle is closed and valid for evaluation
                ensureCycleClosed(offspringRoute);
                double cost = computeTotalCost(offspringRoute);
                offspringResult = new Result(new ArrayList<>(offspringRoute), cost);
            }

            // F. Replacement (Steady State + Uniqueness)
            // "There must be no copies... compare objective function"
            addUniqueAndReplaceWorst(population, offspringResult);
        }

        return getBestSolution(population);
    }

    // --- Helpers ---

    private void fillPopulation(List<Result> pop, LocalSearch ls, long startTime) {
        int attempts = 0;
        while (pop.size() < populationSize && System.currentTimeMillis() - startTime < maxTimeMs) {
            List<Integer> randomRoute = generateRandomRoute();
            Result optimized = ls.solve(randomRoute);
            addSolutionToPopulation(pop, optimized);

            attempts++;
            if (attempts > populationSize * 10) break; // Safety break
        }
    }

    private void performCataclysm(List<Result> pop, LocalSearch ls, long startTime) {
        Result elite = pop.get(0); // Keep the absolute best
        pop.clear();
        pop.add(elite);
        fillPopulation(pop, ls, startTime);
    }

    private boolean isConverged(List<Result> pop) {
        if (pop.isEmpty()) return false;
        double best = pop.get(0).getTotalCost();
        double worst = pop.get(pop.size()-1).getTotalCost();
        return (worst - best) < CONVERGENCE_THRESHOLD;
    }

    private void addSolutionToPopulation(List<Result> pop, Result res) {
        if (isUnique(pop, res)) {
            pop.add(res);
            pop.sort(Comparator.comparingDouble(Result::getTotalCost));
        }
    }

    private void addUniqueAndReplaceWorst(List<Result> pop, Result res) {
        if (!isUnique(pop, res)) return;

        // If not full, just add
        if (pop.size() < populationSize) {
            pop.add(res);
            pop.sort(Comparator.comparingDouble(Result::getTotalCost));
            return;
        }

        // If full, replace worst IF better
        Result worst = pop.get(pop.size() - 1);
        if (res.getTotalCost() < worst.getTotalCost()) {
            pop.remove(pop.size() - 1);
            pop.add(res);
            pop.sort(Comparator.comparingDouble(Result::getTotalCost));
        }
    }

    private boolean isUnique(List<Result> pop, Result res) {
        for (Result existing : pop) {
            if (Math.abs(existing.getTotalCost() - res.getTotalCost()) < 1e-6) return false;
        }
        return true;
    }

    private Result getBestSolution(List<Result> pop) {
        if (pop.isEmpty()) return null;
        return pop.get(0);
    }

    private void ensureCycleClosed(List<Integer> route) {
        if (!route.isEmpty() && !route.get(0).equals(route.get(route.size() - 1))) {
            route.add(route.get(0));
        }
    }

    private List<Integer> generateRandomRoute() {
        List<Integer> allIndices = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) allIndices.add(i);
        Collections.shuffle(allIndices, random);
        int routeSize = (int)(nodes.size() * 0.5); // 50% Selection
        List<Integer> r = new ArrayList<>(allIndices.subList(0, routeSize));
        r.add(r.get(0));
        return r;
    }

    // --- RECOMBINATION OPERATORS ---

    /**
     * Operator 1: Common parts + Random Fill + Random Connect
     */
    private List<Integer> recombinationOperator1(List<Integer> p1, List<Integer> p2) {
        List<Integer> r1 = new ArrayList<>(p1);
        if (r1.size() > 1 && r1.get(0).equals(r1.get(r1.size()-1))) r1.remove(r1.size()-1);
        List<Integer> r2 = new ArrayList<>(p2);
        if (r2.size() > 1 && r2.get(0).equals(r2.get(r2.size()-1))) r2.remove(r2.size()-1);

        Set<Integer> nodesP2 = new HashSet<>(r2);
        Set<Integer> commonNodes = new HashSet<>(r1);
        commonNodes.retainAll(nodesP2); // Intersection of nodes

        Set<String> edgesP1 = getEdges(r1);
        Set<String> edgesP2 = getEdges(r2);
        edgesP1.retainAll(edgesP2); // Intersection of edges

        // Build fragments from common edges
        List<List<Integer>> fragments = buildFragmentsFromEdges(commonNodes, edgesP1);

        // Add isolated common nodes as single-node fragments
        Set<Integer> nodesInFragments = new HashSet<>();
        for (List<Integer> frag : fragments) nodesInFragments.addAll(frag);
        for (Integer node : commonNodes) {
            if (!nodesInFragments.contains(node)) {
                List<Integer> single = new ArrayList<>();
                single.add(node);
                fragments.add(single);
            }
        }

        // Fill with random nodes until 50% size is reached
        int targetSize = (int)(nodes.size() * 0.5);
        int currentSize = commonNodes.size();

        List<Integer> availableNodes = new ArrayList<>();
        for(int i = 0; i < nodes.size(); i++) {
            if (!commonNodes.contains(i)) availableNodes.add(i);
        }
        Collections.shuffle(availableNodes, random);

        int needed = targetSize - currentSize;
        for (int i = 0; i < needed && i < availableNodes.size(); i++) {
            List<Integer> newFrag = new ArrayList<>();
            newFrag.add(availableNodes.get(i));
            fragments.add(newFrag);
        }

        // Connect subpaths at random
        Collections.shuffle(fragments, random);
        List<Integer> newRoute = new ArrayList<>();
        for (List<Integer> frag : fragments) {
            // Randomly flip fragments
            if (random.nextBoolean() && frag.size() > 1) {
                Collections.reverse(frag);
            }
            newRoute.addAll(frag);
        }

        if (!newRoute.isEmpty()) newRoute.add(newRoute.get(0));
        return newRoute;
    }

    /**
     * Operator 2: P1 Base -> Filter by P2 -> Repair
     */
    private List<Integer> recombinationOperator2(List<Integer> p1, List<Integer> p2) {
        List<Integer> child = new ArrayList<>(p1);
        if(child.size() > 1 && child.get(0).equals(child.get(child.size()-1))) {
            child.remove(child.size()-1);
        }

        Set<Integer> nodesP2 = new HashSet<>(p2);
        // Remove nodes not in P2
        child.removeIf(node -> !nodesP2.contains(node));

        // Repair using Heuristic (Greedy 2-Regret assumed)
        Greedy2RegretHeuristicCycleRepairer repairer = new Greedy2RegretHeuristicCycleRepairer(
                distanceMatrix, nodes, 2, 0.5
        );
        // Result includes cycle closure
        Result repairedResult = repairer.repair(child);
        return repairedResult.getRoute();
    }

    // --- Mutation ---
    private void applyDoubleBridgeMutation(List<Integer> route) {
        int n = route.size();
        if (n > 1 && route.get(0).equals(route.get(n - 1))) n--;
        if (n < 8) return;

        List<Integer> cuts = new ArrayList<>();
        for (int i = 0; i < 4; i++) cuts.add(1 + random.nextInt(n - 1));
        Collections.sort(cuts);
        if (cuts.get(0).equals(cuts.get(1)) || cuts.get(1).equals(cuts.get(2)) || cuts.get(2).equals(cuts.get(3))) return;

        List<Integer> a = new ArrayList<>(route.subList(0, cuts.get(0)));
        List<Integer> b = new ArrayList<>(route.subList(cuts.get(0), cuts.get(1)));
        List<Integer> c = new ArrayList<>(route.subList(cuts.get(1), cuts.get(2)));
        List<Integer> d = new ArrayList<>(route.subList(cuts.get(2), n));

        route.clear();
        route.addAll(a);
        route.addAll(d);
        route.addAll(c);
        route.addAll(b);
        route.add(route.get(0));
    }

    // --- Graph Utils ---
    private Set<String> getEdges(List<Integer> route) {
        Set<String> edges = new HashSet<>();
        for (int i = 0; i < route.size() - 1; i++) {
            int u = route.get(i);
            int v = route.get(i+1);
            if (u < v) edges.add(u + "-" + v);
            else edges.add(v + "-" + u);
        }
        return edges;
    }

    private List<List<Integer>> buildFragmentsFromEdges(Set<Integer> nodes, Set<String> edges) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (Integer n : nodes) adj.put(n, new ArrayList<>());
        for (String edge : edges) {
            String[] parts = edge.split("-");
            int u = Integer.parseInt(parts[0]);
            int v = Integer.parseInt(parts[1]);
            if (nodes.contains(u) && nodes.contains(v)) {
                adj.get(u).add(v);
                adj.get(v).add(u);
            }
        }
        List<List<Integer>> fragments = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        for (Integer node : nodes) {
            if (!visited.contains(node) && !adj.get(node).isEmpty()) {
                List<Integer> path = new ArrayList<>();
                dfsPath(node, adj, visited, path);
                fragments.add(path);
            }
        }
        return fragments;
    }

    private void dfsPath(int u, Map<Integer, List<Integer>> adj, Set<Integer> visited, List<Integer> path) {
        visited.add(u);
        path.add(u);
        for (int v : adj.get(u)) {
            if (!visited.contains(v)) dfsPath(v, adj, visited, path);
        }
    }
}