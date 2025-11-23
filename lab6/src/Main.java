import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {

    // --- Experiment Constants ---
    private static final int NUM_EXPERIMENT_RUNS = 20; // Run MSLS and ILS 20 times each
    private static final int MSLS_ITERATIONS = 200;    // MSLS performs 200 LS runs internally

    public static void main(String[] args) {
        conductExperiments("TSPA");
        conductExperiments("TSPB");
    }

    public static void conductExperiments(String fileName) {
        String filePath = STR."../data/\{fileName}.csv";
        List<Node> nodes = new ArrayList<>();

        // --- 1. Load Data ---
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
            return;
        }

        int n = nodes.size();
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = nodes.get(i).getX();
            y[i] = nodes.get(i).getY();
        }

        DistanceMatrix dm = new DistanceMatrix(x, y);
        System.out.println(STR."=== Distance Matrix for \{fileName} calculated ===");

        // --- Prepare Statistics Containers ---
        SolutionSpace mslsStats = new SolutionSpace();
        SolutionSpace ilsStats = new SolutionSpace();

        long totalMslsTime = 0;

        System.out.println(STR."\n--- Starting MSLS Experiment (\{NUM_EXPERIMENT_RUNS} runs, \{MSLS_ITERATIONS} LS calls each) ---");

        // ---------------------------------------------------------
        // 2. Run Multiple Start Local Search (MSLS)
        // ---------------------------------------------------------
        MSLS mslsSolver = new MSLS(dm.getMatrix(), nodes, MSLS_ITERATIONS);

        for (int i = 0; i < NUM_EXPERIMENT_RUNS; i++) {
            long start = System.currentTimeMillis();

            Result result = mslsSolver.solve();

            long end = System.currentTimeMillis();
            long duration = end - start;
            totalMslsTime += duration;

            mslsStats.addSolution(result);

            System.out.println(STR."MSLS Run \{i+1}/\{NUM_EXPERIMENT_RUNS}: Cost=\{result.getTotalCost()} Time=\{duration}ms");
        }

        // Calculate Average Time to limit ILS
        long avgMslsTimeMs = totalMslsTime / NUM_EXPERIMENT_RUNS;
        System.out.println(STR."\n*** Average MSLS Time: \{avgMslsTimeMs} ms ***");
        System.out.println("This will be used as the time limit for ILS.");


        System.out.println(STR."\n--- Starting ILS Experiment (\{NUM_EXPERIMENT_RUNS} runs) ---");

        // ---------------------------------------------------------
        // 3. Run Iterated Local Search (ILS)
        // ---------------------------------------------------------
        ILS ilsSolver = new ILS(dm.getMatrix(), nodes, (int)avgMslsTimeMs);

        for (int i = 0; i < NUM_EXPERIMENT_RUNS; i++) {
            long start = System.currentTimeMillis();

            Result result = ilsSolver.solve();

            long end = System.currentTimeMillis();

            ilsStats.addSolution(result);

            // FIXED: Used result.getTotalCost() instead of result.cost
            System.out.println(STR."ILS Run \{i+1}/\{NUM_EXPERIMENT_RUNS}: Cost=\{result.getTotalCost()} Time=\{end - start}ms");
        }


        // ---------------------------------------------------------
        // 4. Save and Report Results
        // ---------------------------------------------------------
        saveResults(fileName, "MSLS", mslsStats);
        saveResults(fileName, "ILS", ilsStats);

        System.out.println(STR."\n=== Final Statistics for \{fileName} ===");
        System.out.println("Method | Min | Avg | Max");
        System.out.println(STR."MSLS   | \{mslsStats.getMin()} | \{mslsStats.getAvg()} | \{mslsStats.getMax()}");
        System.out.println(STR."ILS    | \{ilsStats.getMin()} | \{ilsStats.getAvg()} | \{ilsStats.getMax()}");
        System.out.println("==========================================\n");
    }

    private static void saveResults(String fileName, String methodName, SolutionSpace stats) {
        // 1. Save Stats
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_\{methodName}_stats.csv")) {
            writer.write("min,max,avg,sd\n");
            StringBuilder line = new StringBuilder();
            for (Double st : stats.getAllStats()) {
                line.append(st).append(",");
            }
            writer.write(line.substring(0, line.length() - 1) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 2. Save Best Solution Found
        stats.bestSolutionToCsv(STR."evaluation/results/\{fileName}_\{methodName}_best.csv");
    }
}