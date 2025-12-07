import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {

    // experiment variables
    private static final int NUM_LOCAL_OPTIMA = 1000;
    private static final int ILS_TIME_LIMIT_MS = 8000;

    public static void main(String[] args) {
        generateSolutions("TSPA");
        generateSolutions("TSPB");
    }

    public static void generateSolutions(String fileName) {
        System.out.println(STR."\n=== Generating Solutions for \{fileName} ===");
        String filePath = STR."../data/\{fileName}.csv";
        List<Node> nodes = new ArrayList<>();

        //Load Data
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

        System.out.println(STR."Running ILS for \{ILS_TIME_LIMIT_MS}ms to find reference solution...");
        ILS ilsSolver = new ILS(dm.getMatrix(), nodes, ILS_TIME_LIMIT_MS);
        Result ilsBestResult = ilsSolver.solve();
        System.out.println(STR."ILS Reference Cost: \{ilsBestResult.getTotalCost()}");
        saveSolution(STR."evaluation/\{fileName}_reference.csv", ilsBestResult, -1);

        // Generate 1000 Random Local Optima (Greedy LS)
        System.out.println(STR."Generating \{NUM_LOCAL_OPTIMA} Local Optima (Greedy)...");

        LocalSearch greedyLS = new LocalSearch(dm.getMatrix(), nodes, false, false);
        Random random = new Random();

        String outputCsv = STR."evaluation/\{fileName}_local_optima.csv";

        try (FileWriter writer = new FileWriter(outputCsv)) {
            writer.write("index,cost,route\n"); // CSV Header

            for (int i = 0; i < NUM_LOCAL_OPTIMA; i++) {
                // Generate random initial route (50% of nodes)
                List<Integer> initialRoute = generateRandomRoute(n, random);

                // Run Greedy Local Search
                Result lo = greedyLS.solve(initialRoute);

                // Format Route: Replace commas with pipes to avoid CSV conflicts
                // e.g., "[1, 2, 3]" -> "[1|2|3]"
                String routeStr = lo.getRoute().toString().replace(", ", "|");

                writer.write(STR."\{i},\{lo.getTotalCost()},\{routeStr}\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(STR."Saved \{NUM_LOCAL_OPTIMA} solutions to \{outputCsv}");
    }

    // --- Helper Methods ---

    private static void saveSolution(String filepath, Result result, int index) {
        try (FileWriter writer = new FileWriter(filepath)) {
            writer.write("index,cost,route\n");
            String routeStr = result.getRoute().toString().replace(", ", "|");
            writer.write(STR."\{index},\{result.getTotalCost()},\{routeStr}\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** * Generates a random initial route containing 50% of the nodes. */
    private static List<Integer> generateRandomRoute(int totalNodes, Random random) {
        List<Integer> allIndices = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) allIndices.add(i);
        Collections.shuffle(allIndices, random);
        int routeSize = (int)(totalNodes * 0.5);
        return new ArrayList<>(allIndices.subList(0, routeSize));
    }
}