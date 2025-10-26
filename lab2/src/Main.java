import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        conductExperiments("TSPA");
        conductExperiments("TSPB");

    }

    public static void conductExperiments(String fileName){
        String filePath = STR."../data/\{fileName}.csv";
        List<Node> nodes = new ArrayList<>();

        // 4 solution spaces for 4 heuristics
        SolutionSpace WeightedCycleSolutions = new SolutionSpace();
        SolutionSpace PureCycleSolutions = new SolutionSpace();
        SolutionSpace WeightedFlexibleNNSolutions = new SolutionSpace();
        SolutionSpace PureFlexibleNNSolutions = new SolutionSpace();

        long WeightedCycleTime = 0;
        long PureCycleTime = 0;
        long WeightedFlexibleNNTime = 0;
        long PureFlexibleNNTime = 0;

        // Load nodes from CSV
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

        // Create distance matrix
        int n = nodes.size();
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = nodes.get(i).getX();
            y[i] = nodes.get(i).getY();
        }

        DistanceMatrix dm = new DistanceMatrix(x, y);
        System.out.println("=== Distance Matrix ===");
        dm.printMatrix();

        // --- Instantiate heuristics ---
        Greedy2RegretHeuristicCycle WeightedCycleHeuristic =
                new Greedy2RegretHeuristicCycle(dm.getMatrix(), nodes, 2, 0.5);
        Greedy2RegretHeuristicCycle PureCycleHeuristic =
                new Greedy2RegretHeuristicCycle(dm.getMatrix(), nodes, 2, 1.0);

        Greedy2RegretHeuristicFlexibleNN WeightedFlexibleNNHeuristic =
                new Greedy2RegretHeuristicFlexibleNN(dm.getMatrix(), nodes, 0.5);
        Greedy2RegretHeuristicFlexibleNN PureFlexibleNNHeuristic =
                new Greedy2RegretHeuristicFlexibleNN(dm.getMatrix(), nodes, 1.0);

        // --- Run all heuristics ---
        for (int i = 0; i < n; i++) {
            System.out.println(STR."Iteration number: \{i + 1}");

            // 1. Weighted Regret Cycle (0.5)
            long start = System.nanoTime();
            Result weightedCycleResult = WeightedCycleHeuristic.solve(i);
            long end = System.nanoTime();
            WeightedCycleSolutions.addSolution(weightedCycleResult);
            WeightedCycleTime += end - start;

            // 2. Pure Regret Cycle (1.0)
            start = System.nanoTime();
            Result pureCycleResult = PureCycleHeuristic.solve(i);
            end = System.nanoTime();
            PureCycleSolutions.addSolution(pureCycleResult);
            PureCycleTime += end - start;

            // 3. Weighted Regret FlexibleNN (0.5)
            start = System.nanoTime();
            Result weightedFlexibleNNResult = WeightedFlexibleNNHeuristic.solve(i);
            end = System.nanoTime();
            WeightedFlexibleNNSolutions.addSolution(weightedFlexibleNNResult);
            WeightedFlexibleNNTime += end - start;

            // 4. Pure Regret FlexibleNN (1.0)
            start = System.nanoTime();
            Result pureFlexibleNNResult = PureFlexibleNNHeuristic.solve(i);
            end = System.nanoTime();
            PureFlexibleNNSolutions.addSolution(pureFlexibleNNResult);
            PureFlexibleNNTime += end - start;
        }

        // --- Print execution times ---
        System.out.println(STR."Weighted Regret Cycle time (ms): \{WeightedCycleTime / 1_000_000}");
        System.out.println(STR."Pure Regret Cycle time (ms): \{PureCycleTime / 1_000_000}");
        System.out.println(STR."Weighted Regret FlexibleNN time (ms): \{WeightedFlexibleNNTime / 1_000_000}");
        System.out.println(STR."Pure Regret FlexibleNN time (ms): \{PureFlexibleNNTime / 1_000_000}");

        // --- Write times to file ---
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_times.csv")) {
            writer.write("method_name,time\n");
            writer.write(STR."weighted_regret_cycle,\{WeightedCycleTime / 1_000_000}\n");
            writer.write(STR."pure_regret_cycle,\{PureCycleTime / 1_000_000}\n");
            writer.write(STR."weighted_regret_flexibleNN,\{WeightedFlexibleNNTime / 1_000_000}\n");
            writer.write(STR."pure_regret_flexibleNN,\{PureFlexibleNNTime / 1_000_000}\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- Print statistics ---
        System.out.println(STR."*** Weighted Regret Cycle stats: ***\n\{WeightedCycleSolutions.statsToStr()}\n");
        System.out.println(STR."*** Pure Regret Cycle stats: ***\n\{PureCycleSolutions.statsToStr()}\n");
        System.out.println(STR."*** Weighted Regret FlexibleNN stats: ***\n\{WeightedFlexibleNNSolutions.statsToStr()}\n");
        System.out.println(STR."*** Pure Regret FlexibleNN stats: ***\n\{PureFlexibleNNSolutions.statsToStr()}\n");

        // --- Write statistics to file ---
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_stats.csv")) {
            writer.write("method_name,min,max,avg,sd\n");

            writeStatsLine(writer, "weighted_regret_cycle", WeightedCycleSolutions);
            writeStatsLine(writer, "pure_regret_cycle", PureCycleSolutions);
            writeStatsLine(writer, "weighted_regret_flexibleNN", WeightedFlexibleNNSolutions);
            writeStatsLine(writer, "pure_regret_flexibleNN", PureFlexibleNNSolutions);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- Save best solutions ---
        // Ensure directories exist
        WeightedCycleSolutions.bestSolutionToCsv(STR."evaluation/results/\{fileName}_weighted_regret_cycle.csv");
        PureCycleSolutions.bestSolutionToCsv(STR."evaluation/results/\{fileName}_pure_regret_cycle.csv");
        WeightedFlexibleNNSolutions.bestSolutionToCsv(STR."evaluation/results/\{fileName}_weighted_regret_flexibleNN.csv");
        PureFlexibleNNSolutions.bestSolutionToCsv(STR."evaluation/results/\{fileName}_pure_regret_flexibleNN.csv");
    }

    private static void writeStatsLine(FileWriter writer, String name, SolutionSpace space) throws IOException {
        StringBuilder str = new StringBuilder(name + ",");
        for (Double st : space.getAllStats()) {
            str.append(STR."\{st},");
        }
        writer.write(str.substring(0, str.length() - 1) + '\n');
    }
}
