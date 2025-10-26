import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        conductExperiments("TSPA");
        conductExperiments("TSPB");
    }

    public static void conductExperiments(String fileName) {
        String filePath = STR."../data/\{fileName}.csv";
        List<Node> nodes = new ArrayList<>();

        // --- Load nodes from CSV ---
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                if (values.length >= 3) {
                    int x = Integer.parseInt(values[0].trim());
                    int y = Integer.parseInt(values[1].trim());
                    int cost = Integer.parseInt(values[2].trim());
                    nodes.add(new Node(x, y, cost));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int n = nodes.size();
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = nodes.get(i).getX();
            y[i] = nodes.get(i).getY();
        }

        DistanceMatrix dm = new DistanceMatrix(x, y);
        System.out.println(STR."=== Distance Matrix for \{fileName} ===");
        dm.printMatrix();

        // --- Heuristic for initialization ---
        Greedy2RegretHeuristicFlexibleNN WeightedFlexibleNNHeuristic =
                new Greedy2RegretHeuristicFlexibleNN(dm.getMatrix(), nodes, 0.5);

        // --- Local Search Variants ---
        LocalSearch steepestNode = new LocalSearch(dm.getMatrix(), nodes, true, true);
        LocalSearch steepestEdge = new LocalSearch(dm.getMatrix(), nodes, true, false);
        LocalSearch greedyNode = new LocalSearch(dm.getMatrix(), nodes, false, true);
        LocalSearch greedyEdge = new LocalSearch(dm.getMatrix(), nodes, false, false);

        // --- Solution spaces for all 8 LS methods ---
        Map<String, SolutionSpace> localSearchSpaces = new LinkedHashMap<>();
        String[] methodNames = {
                "steepest_node_heuristic",
                "steepest_edge_heuristic",
                "greedy_node_heuristic",
                "greedy_edge_heuristic",
                "steepest_node_random",
                "steepest_edge_random",
                "greedy_node_random",
                "greedy_edge_random"
        };
        for (String name : methodNames) {
            localSearchSpaces.put(name, new SolutionSpace());
        }

        long[] totalTimes = new long[methodNames.length];
        Random random = new Random();

        // --- Run for every node as start point ---
        for (int i = 0; i < n; i++) {
            System.out.println(STR."Iteration number: \{i + 1}");

            // 1. Heuristic initialization
            Result heuristicResult = WeightedFlexibleNNHeuristic.solve(i);
            List<Integer> heuristicRoute = heuristicResult.getRoute();

            // 2. Random initialization
            List<Integer> randomRoute = new ArrayList<>();
            for (int j = 0; j < n; j++) randomRoute.add(j);
            Collections.shuffle(randomRoute, random);
            randomRoute = randomRoute.subList(0, (n/2));
            randomRoute.add(randomRoute.get(0));

            // --- Run all 8 local search configurations ---
            LocalSearch[] methods = {
                    steepestNode, steepestEdge, greedyNode, greedyEdge,
                    steepestNode, steepestEdge, greedyNode, greedyEdge
            };
            List<List<Integer>> routes = List.of(
                    heuristicRoute, heuristicRoute, heuristicRoute, heuristicRoute,
                    randomRoute, randomRoute, randomRoute, randomRoute
            );

            for (int m = 0; m < methods.length; m++) {
                long start = System.nanoTime();
                Result res = methods[m].solve(routes.get(m));
                long end = System.nanoTime();
                totalTimes[m] += (end - start);
                localSearchSpaces.get(methodNames[m]).addSolution(res);
            }
        }

        // --- Print execution times ---
        System.out.println("\n=== Execution Times (ms) ===");
        for (int i = 0; i < methodNames.length; i++) {
            System.out.println(STR."\{methodNames[i]}: \{totalTimes[i] / 1_000_000}");
        }

        // --- Write times to CSV ---
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_local_times.csv")) {
            writer.write("method_name,time_ms\n");
            for (int i = 0; i < methodNames.length; i++) {
                writer.write(STR."\{methodNames[i]},\{totalTimes[i] / 1_000_000}\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- Print statistics ---
        System.out.println(STR."\n=== Local Search Statistics for \{fileName} ===");
        for (String name : methodNames) {
            System.out.println(STR."*** \{name} stats: ***\n\{localSearchSpaces.get(name).statsToStr()}\n");
        }

        // --- Write statistics to file ---
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_local_stats.csv")) {
            writer.write("method_name,min,max,avg,sd\n");
            for (String name : methodNames) {
                StringBuilder line = new StringBuilder(name + ",");
                for (Double st : localSearchSpaces.get(name).getAllStats()) {
                    line.append(st).append(",");
                }
                writer.write(line.substring(0, line.length() - 1) + '\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- Save best solutions ---
        for (String name : methodNames) {
            localSearchSpaces.get(name).bestSolutionToCsv(STR."evaluation/results/\{fileName}_\{name}.csv");
        }

        System.out.println(STR."=== All results saved for \{fileName} ===\n");
    }
}
