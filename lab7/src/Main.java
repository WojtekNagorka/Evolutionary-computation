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
        List<Node> nodes = loadNodesFromCSV(fileName);
        DistanceMatrix dm = createDistanceMatrix(nodes);

        System.out.println(STR."=== Distance Matrix for \{fileName} calculated ===");

        // --- Prepare Statistics Containers ---
        SolutionSpace mslsStats = new SolutionSpace();
        SolutionSpace lnsWithLocalSearchStats = new SolutionSpace();
        SolutionSpace lnsWithoutLocalSearchStats = new SolutionSpace();

        List<Integer> number_of_iterations = new ArrayList<>();

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
            int nr_of_it = ilsSolver.getNumberOfIterations();
            number_of_iterations.add(nr_of_it);

            System.out.println(STR."ILS Run \{i+1}/\{NUM_EXPERIMENT_RUNS}: Cost=\{result.getTotalCost()} Time=\{end - start}ms");
        }


        // ---------------------------------------------------------
        // 4. Save and Report Results
        // ---------------------------------------------------------
//        saveResults(fileName, "MSLS", mslsStats);
//        saveResults(fileName, "ILS", ilsStats);
        saveResults(fileName, new String[]{"MSLS", "ILS"}, new SolutionSpace[]{mslsStats, ilsStats});
        saveList(fileName, "number_of_iterations", number_of_iterations);

        System.out.println(STR."\n=== Final Statistics for \{fileName} ===");
        System.out.println("Method | Min | Avg | Max");
        System.out.println(STR."MSLS   | \{mslsStats.getMin()} | \{mslsStats.getAvg()} | \{mslsStats.getMax()}");
        System.out.println(STR."ILS    | \{ilsStats.getMin()} | \{ilsStats.getAvg()} | \{ilsStats.getMax()}");
        System.out.println("==========================================\n");

        // ---------------------------------------------------------
        // 5. Report times
        // ---------------------------------------------------------
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_times.csv")) {
            writer.write(STR."\{(int)avgMslsTimeMs}");
        } catch (IOException e) {
            e.printStackTrace();
        }
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