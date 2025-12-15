import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {

    // Experiment settings
    private static final int NUM_RUNS = 20;
    private static final int TIME_LIMIT_MS = 8000;

    // Helper class to store results for comparison
    static class Stats {
        String name;
        double best = Double.MAX_VALUE;
        double worst = Double.MIN_VALUE;
        double avg = 0;

        Stats(String name) { this.name = name; }
    }

    public static void main(String[] args) {
        // Run full suite for TSPA
        runSuite("TSPA");

        // Run full suite for TSPB
        runSuite("TSPB");
    }

    public static void runSuite(String instanceName) {
        System.out.println(STR."\n******************************************");
        System.out.println(STR." STARTING SUITE FOR: \{instanceName}");
        System.out.println("******************************************");

        // 1. Operator 1: Common Parts + Random Fill
        Stats op1_LS   = runExperiment(instanceName, false, true);  // Op1 + With LS
        Stats op1_NoLS = runExperiment(instanceName, false, false); // Op1 + No LS

        // 2. Operator 2: Repair Heuristic (LNS-style)
        Stats op2_LS   = runExperiment(instanceName, true, true);   // Op2 + With LS
        Stats op2_NoLS = runExperiment(instanceName, true, false);  // Op2 + No LS

        // 3. Print Comparison
        printComparisonTable(instanceName, op1_LS, op1_NoLS, op2_LS, op2_NoLS);
    }

    /**
     * @param useOperator2 If true, uses Operator 2 (Repair). If false, uses Operator 1 (Constructive).
     * @param useLocalSearch If true, applies LS after recombination.
     */
    public static Stats runExperiment(String instanceName, boolean useOperator2, boolean useLocalSearch) {
        String opName = useOperator2 ? "Op2" : "Op1";
        String lsName = useLocalSearch ? "WithLS" : "NoLS";
        String runLabel = STR."\{opName}_\{lsName}";

        System.out.println(STR."\n--- Running \{instanceName} [\{runLabel}] ---");

        List<Node> nodes = loadNodes(STR."../../data/\{instanceName}.csv");
        if (nodes == null) return new Stats("Error");

        DistanceMatrix dm = createDistanceMatrix(nodes);
        Stats stats = new Stats(runLabel);
        double totalCost = 0;

        String outputFilePath = STR."../evaluation/\{instanceName}_\{runLabel}.csv";

        try (FileWriter writer = new FileWriter(outputFilePath)) {
            writer.write("run_id,cost,route\n");

            for (int i = 0; i < NUM_RUNS; i++) {
                // Initialize Solver with dynamic parameters
                HybridEvolutionaryAlgorithm solver = new HybridEvolutionaryAlgorithm(
                        dm.getMatrix(), nodes, TIME_LIMIT_MS,
                        useOperator2,    // <--- Dynamic now
                        useLocalSearch   // <--- Dynamic now
                );

                Result result = solver.solve();
                double cost = result.getTotalCost();

                // Update Stats
                if (cost < stats.best) stats.best = cost;
                if (cost > stats.worst) stats.worst = cost;
                totalCost += cost;

                System.out.println(STR."Run \{i + 1}/\{NUM_RUNS} Cost: \{cost}");
                String routeStr = result.getRoute().toString().replace(", ", "|");
                writer.write(STR."\{i + 1},\{cost},\{routeStr}\n");
                writer.flush();
            }
            stats.avg = totalCost / NUM_RUNS;
            System.out.println(STR."Saved to \{outputFilePath}");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return stats;
    }

    private static void printComparisonTable(String instance, Stats s1, Stats s2, Stats s3, Stats s4) {
        System.out.println(STR."\n===================================================================");
        System.out.println(STR." FINAL RESULTS: \{instance}");
        System.out.println("===================================================================");
        System.out.printf("%-15s | %-12s | %-12s | %-12s%n", "Configuration", "Best", "Avg", "Worst");
        System.out.println("-------------------------------------------------------------------");
        printRow(s1);
        printRow(s2);
        printRow(s3);
        printRow(s4);
        System.out.println("===================================================================\n");
    }

    private static void printRow(Stats s) {
        System.out.printf("%-15s | %-12.2f | %-12.2f | %-12.2f%n", s.name, s.best, s.avg, s.worst);
    }

    // --- Data Loading Helpers ---

    private static DistanceMatrix createDistanceMatrix(List<Node> nodes) {
        int n = nodes.size();
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = nodes.get(i).getX();
            y[i] = nodes.get(i).getY();
        }
        return new DistanceMatrix(x, y);
    }

    private static List<Node> loadNodes(String filePath) {
        List<Node> nodes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                if (values.length >= 3) {
                    nodes.add(new Node(
                            Integer.parseInt(values[0].trim()),
                            Integer.parseInt(values[1].trim()),
                            Integer.parseInt(values[2].trim())
                    ));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return nodes;
    }
}