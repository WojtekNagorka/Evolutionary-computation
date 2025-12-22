import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {

    // --- Experiment Constants ---
    private static final int NUM_EXPERIMENT_RUNS = 20;
    private static final int TIME_LIMIT_MS = 8000;

    public static void main(String[] args) {
        conductExperiments("TSPA");
        conductExperiments("TSPB");
    }

    public static void conductExperiments(String fileName) {
        String filePath = "../data/" + fileName + ".csv";
        List<Node> nodes = new ArrayList<>();
        List<Integer> number_of_iterations = new ArrayList<>();

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
        System.out.println("=== Distance Matrix for " + fileName + " calculated ===");

        // --- Prepare Statistics Containers ---
        SolutionSpace ilsStats = new SolutionSpace();

        System.out.println("\n--- Starting ILS Experiment (" + NUM_EXPERIMENT_RUNS + " runs) ---");

        // ---------------------------------------------------------
        // 2. Run Iterated Local Search (ILS)
        // ---------------------------------------------------------
        ILS ilsSolver = new ILS(dm.getMatrix(), nodes, TIME_LIMIT_MS);

        for (int i = 0; i < NUM_EXPERIMENT_RUNS; i++) {
            long start = System.currentTimeMillis();

            Result result = ilsSolver.solve();

            long end = System.currentTimeMillis();

            ilsStats.addSolution(result);

            int nr_of_it = ilsSolver.getNumberOfIterations();
            number_of_iterations.add(nr_of_it);

            System.out.println("ILS Run " + (i+1) + "/" + NUM_EXPERIMENT_RUNS +
                    ": Cost=" + result.getTotalCost() +
                    " Time=" + (end - start) + "ms" +
                    " Iterations=" + nr_of_it);
        }

        // ---------------------------------------------------------
        // 3. Save and Report Results
        // ---------------------------------------------------------

        saveResults(fileName, new String[]{"ILS"}, new SolutionSpace[]{ilsStats});
        saveList(fileName, "number_of_iterations", number_of_iterations);

        System.out.println("\n=== Final Statistics for " + fileName + " ===");
        System.out.println("Method | Min | Avg | Max");

        System.out.println("ILS    | " + ilsStats.getMin() + " | " + ilsStats.getAvg() + " | " + ilsStats.getMax());
        System.out.println("==========================================\n");

        // ---------------------------------------------------------
        // 4. Report times (Fixed time limit)
        // ---------------------------------------------------------
        try (FileWriter writer = new FileWriter("evaluation/" + fileName + "_times.csv")) {
            writer.write(String.valueOf(TIME_LIMIT_MS));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveResults(String fileName, String[] methodNames, SolutionSpace[] solutionSpaces){
        // FIXED: Concatenation
        try (FileWriter writer = new FileWriter("evaluation/" + fileName + "_stats.csv")) {
            writer.write("method_name,min,max,avg,sd\n");
            StringBuilder line = new StringBuilder();

            for (int i=0; i < methodNames.length; i++){
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
        try (FileWriter writer = new FileWriter("evaluation/" + instance + "_" + title + ".csv")) {
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