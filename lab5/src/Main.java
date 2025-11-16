import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

    private static final int NUM_RUNS = 200;

    public static void main(String[] args) {
        conductExperiments("TSPA");
        conductExperiments("TSPB");
    }

    /**
     * Helper method to generate a random solution
     * This creates a partial tour of size n/2, which is required for inter-route moves.
     */
    private static List<Integer> getRandomSolution(List<Node> nodes, Random rand) {
        int n = nodes.size();
        List<Integer> allNodes = IntStream.range(0, n).boxed().collect(Collectors.toList());
        Collections.shuffle(allNodes, rand);
        int size = (n + 1) / 2; // Create a tour of half the nodes
        return new ArrayList<>(allNodes.subList(0, size));
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


        // 1. Steepest, edge-swap, WITH move list
        LocalSearch lsWithLM = new LocalSearch(dm.getMatrix(), nodes, true, false, true);

        // 2. Steepest, edge-swap, WITHOUT move list (baseline)
        LocalSearch lsBaseline = new LocalSearch(dm.getMatrix(), nodes, true, false, false);


        // --- NEW: Solution spaces for only the 2 required methods ---
        Map<String, SolutionSpace> localSearchSpaces = new LinkedHashMap<>();
        String[] methodNames = {
                "steepest_edge_LM_random",
                "steepest_edge_baseline_random"
        };
        for (String name : methodNames) {
            localSearchSpaces.put(name, new SolutionSpace());
        }

        long[] totalTimes = new long[methodNames.length];
        Random random = new Random(42); // Use a fixed seed for reproducibility

        // --- NEW: Run NUM_RUNS times ---
        for (int i = 0; i < NUM_RUNS; i++) {
            if ((i+1) % 20 == 0) {
                System.out.println(STR."Iteration number: \{i + 1} / \{NUM_RUNS}");
            }

            // 1. Random initialization
            List<Integer> randomRoute = getRandomSolution(nodes, random);

            // --- NEW: Run only the 2 required local search configurations ---
            LocalSearch[] methods = {
                    lsWithLM,
                    lsBaseline
            };

            for (int m = 0; m < methods.length; m++) {
                long start = System.nanoTime();
                // Pass a *copy* of the route so it's not modified by the other search
                Result res = methods[m].solve(new ArrayList<>(randomRoute));
                long end = System.nanoTime();
                totalTimes[m] += (end - start);
                localSearchSpaces.get(methodNames[m]).addSolution(res);
            }
        }

        // --- Print execution times ---
        System.out.println("\n=== Execution Times (ms) ===");
        for (int i = 0; i < methodNames.length; i++) {
            // Calculate average time per run
            double avgTimeMs = (totalTimes[i] / 1_000_000.0) / NUM_RUNS;
            System.out.println(STR."\{methodNames[i]} (Avg): \{String.format("%.2f", avgTimeMs)} ms");
        }

        // --- Write times to CSV ---
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_times.csv")) {
            writer.write("method_name,avg_time_ms\n");
            for (int i = 0; i < methodNames.length; i++) {
                double avgTimeMs = (totalTimes[i] / 1_000_000.0) / NUM_RUNS;
                writer.write(STR."\{methodNames[i]},\{avgTimeMs}\n");
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
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_stats.csv")) {
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