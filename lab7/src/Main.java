import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {

    // --- Experiment Constants ---
    private static final int NUM_EXPERIMENT_RUNS = 20; // Run LNS and ILS 20 times each
    private static final int MSLS_ITERATIONS = 200;    // LNS performs 200 LS runs internally

    public static void main(String[] args) {
        conductExperiments("TSPA");
        conductExperiments("TSPB");
    }

    public static void conductExperiments(String fileName) {
        List<Node> nodes = loadNodesFromCSV(fileName);
        DistanceMatrix dm = createDistanceMatrix(nodes);
        boolean useCycle = false;

        System.out.println(STR."=== Distance Matrix for \{fileName} calculated ===");

        // --- Prepare Statistics Containers ---
        SolutionSpace LnsWithLocalSearchStats = new SolutionSpace();
        SolutionSpace LnsWithoutLocalSearchStats = new SolutionSpace();

        List<Integer> LnsWithLSIterations = new ArrayList<>();
        List<Integer> LnsWithoutLSIterations = new ArrayList<>();

        long totalMslsTime = 0;
        long totalLnsNoLSTime = 0;
        long totalLnsWithLSTime = 0;

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

            System.out.println(STR."MSLS Run \{i+1}/\{NUM_EXPERIMENT_RUNS}: Cost=\{result.getTotalCost()} Time=\{duration}ms");
        }

        // Calculate Average Time to limit LNS
        long avgMslsTimeMs = totalMslsTime / NUM_EXPERIMENT_RUNS;
        System.out.println(STR."\n*** Average MSLS Time: \{avgMslsTimeMs} ms ***");
        System.out.println("This will be used as the time limit later.");

        System.out.println(STR."\n--- Starting LNS Experiment (\{NUM_EXPERIMENT_RUNS} runs) ---");

        // ---------------------------------------------------------
        // 3. Run LNS with Local Search
        // ---------------------------------------------------------
        LargeNeighborhoodSearch LnsWithLS = new LargeNeighborhoodSearch(dm.getMatrix(), nodes, true, (int)avgMslsTimeMs, useCycle);

        for (int i = 0; i < NUM_EXPERIMENT_RUNS; i++) {
            long start = System.currentTimeMillis();

            Result result = LnsWithLS.solve();

            long end = System.currentTimeMillis();
            totalLnsWithLSTime += (end - start);

            LnsWithLocalSearchStats.addSolution(result);
            int nr_of_it = LnsWithLS.getNumberOfIterations();
            LnsWithLSIterations.add(nr_of_it);

            System.out.println(STR."LNS+LS Run \{i+1}/\{NUM_EXPERIMENT_RUNS}: Cost=\{result.getTotalCost()} Time=\{end - start}ms");
        }
        long avgLNSWithLSTimeMs = totalLnsWithLSTime / NUM_EXPERIMENT_RUNS;

        // ---------------------------------------------------------
        // 4. Run LNS without Local Search
        // ---------------------------------------------------------
        LargeNeighborhoodSearch LnsWithoutLS = new LargeNeighborhoodSearch(dm.getMatrix(), nodes, false, (int)avgMslsTimeMs, useCycle);
        for (int i = 0; i < NUM_EXPERIMENT_RUNS; i++) {
            long start = System.currentTimeMillis();

            Result result = LnsWithoutLS.solve();

            long end = System.currentTimeMillis();
            totalLnsNoLSTime += (end - start);

            LnsWithoutLocalSearchStats.addSolution(result);
            int nr_of_it = LnsWithoutLS.getNumberOfIterations();
            LnsWithoutLSIterations.add(nr_of_it);

            System.out.println(STR."LNS-LS Run \{i+1}/\{NUM_EXPERIMENT_RUNS}: Cost=\{result.getTotalCost()} Time=\{end - start}ms");
        }
        long avgLNSWithoutLSTimeMs = totalLnsNoLSTime / NUM_EXPERIMENT_RUNS;

        // ---------------------------------------------------------
        // 5. Save and Report Results
        // ---------------------------------------------------------
        saveResults(fileName, new String[]{"LNSWithLS", "LNSWithoutLS"}, new SolutionSpace[]{LnsWithLocalSearchStats, LnsWithoutLocalSearchStats});

        System.out.println(STR."\n=== Final Statistics for \{fileName} ===");
        System.out.println("Method | Min | Avg | Max");
        System.out.println(STR."LNS + LS   | \{LnsWithLocalSearchStats.getMin()} | \{LnsWithLocalSearchStats.getAvg()} | \{LnsWithLocalSearchStats.getMax()}");
        System.out.println(STR."LNS - LS   | \{LnsWithoutLocalSearchStats.getMin()} | \{LnsWithoutLocalSearchStats.getAvg()} | \{LnsWithoutLocalSearchStats.getMax()}");
        System.out.println("==========================================\n");

        // ---------------------------------------------------------
        // 6. Report times
        // ---------------------------------------------------------
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_times.csv")) {
            StringBuilder line = new StringBuilder();
            line.append("method_name,time\n");
            line.append(STR."LNSWithLS,\{avgLNSWithLSTimeMs}\n");
            line.append(STR."LNSWithoutLS,\{avgLNSWithoutLSTimeMs}");
            writer.write(line.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ---------------------------------------------------------
        // 7. Save number of iterations
        // ---------------------------------------------------------
        saveList(STR."iterations/\{fileName}", "LNS_with_LS_iterations", LnsWithLSIterations);
        saveList(STR."iterations/\{fileName}", "LNS_without_LS_iterations", LnsWithoutLSIterations);

        // ---------------------------------------------------------
        // 8. Save best routes
        // ---------------------------------------------------------
        LnsWithLocalSearchStats.bestSolutionToCsv(STR."evaluation/results/\{fileName}_LnsWithLS.csv");
        LnsWithoutLocalSearchStats.bestSolutionToCsv(STR."evaluation/results/\{fileName}_LnsWithoutLS.csv");
    }

    private static List<Node> loadNodesFromCSV(String fileName){
        // --- Load data from CSV ---
        String filePath = STR."../data/\{fileName}.csv";
        List<Node> nodes = new ArrayList<>();

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
        return nodes;
    }

    private static DistanceMatrix createDistanceMatrix(List<Node> nodes){
        // --- Create Distance Matrix from nodes ---
        int n = nodes.size();
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = nodes.get(i).getX();
            y[i] = nodes.get(i).getY();
        }

        return new DistanceMatrix(x, y);
    }
    private static void saveResults(String fileName, String[] methodNames, SolutionSpace[] solutionSpaces){
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_stats.csv")) {
            writer.write("method_name,min,max,avg,sd\n");
            StringBuilder line = new StringBuilder();
            for (int i=0; i<2; i++){
                line.append(methodNames[i]);
                for (Double st : solutionSpaces[i].getAllStats()) {
                    line.append(",").append(st);
                }
                line.append("\n");
            }
            writer.write(line.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void saveList(String instance, String title, List<Integer> list_to_save){
        try (FileWriter writer = new FileWriter(STR."evaluation/\{instance}_\{title}.csv")) {
            StringBuilder line = new StringBuilder();
            for (Integer el : list_to_save) {
                line.append(el).append("\n");
            }
            writer.write(line.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}