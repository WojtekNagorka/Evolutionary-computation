import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String fileName = "TSPA";
        String filePath = STR."../data/\{fileName}.csv";
        List<Node> nodes = new ArrayList<>();

        SolutionSpace TwoRegretSolutions = new SolutionSpace();
        SolutionSpace WeightedSumHeuristicSolutions = new SolutionSpace();

        long TwoRegretTime = 0;
        long WeightedSumHeuristicTime = 0;

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

        // Convert List<Node> to coordinate arrays
        int n = nodes.size();
        double[] x = new double[n];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            x[i] = nodes.get(i).getX();
            y[i] = nodes.get(i).getY();
        }

        // Generate and print distance matrix
        DistanceMatrix dm = new DistanceMatrix(x, y);
        System.out.println("=== Distance Matrix ===");
        dm.printMatrix();

        Greedy2RegretHeuristic TwoRegretHeuristic = new Greedy2RegretHeuristic(dm.getMatrix(), nodes);
        GreedyHeuristicWithWeightedSumCriterion WeightedSumHeuristic = new GreedyHeuristicWithWeightedSumCriterion(dm.getMatrix(), nodes);

        for (int i = 0; i < n; i++) {
            // 1. Greedy2RegretHeuristic
            System.out.println(STR."Iteration number:\{i + 1}");
            long start = System.nanoTime();
            Result TwoRegretResult = TwoRegretHeuristic.solve(i);
            long end = System.nanoTime();
            TwoRegretSolutions.addSolution((TwoRegretResult));
            TwoRegretTime += end - start;

            // 2. GreedyHeuristicWithWeightedSumCriterion
            start = System.nanoTime();
            Result WeightedSumResult = WeightedSumHeuristic.solve(i);
            end = System.nanoTime();
            WeightedSumHeuristicSolutions.addSolution(WeightedSumResult);
            WeightedSumHeuristicTime += end - start;
        }
        // Printing time
        System.out.println(STR."GreedyTwoRegretHeuristic result time (ms): \{TwoRegretTime / 1_000_000}");
        System.out.println(STR."GreedyHeuristicWithWeightedSumCriterion result time (ms): \{WeightedSumHeuristicTime / 1_000_000}");

        // Writing times to the file
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_times.csv")) {
            writer.write("method_name,time\n");
            writer.write(STR."greedy_two_regret_heuristic,\{TwoRegretTime / 1_000_000}\n");
            writer.write(STR."greedy_heuristic_with_weighted_sum_criterion,\{WeightedSumHeuristicTime / 1_000_000}\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Printing stats
        System.out.println(STR."***GreedyTwoRegretHeuristic result stats:***\n\{TwoRegretSolutions.statsToStr()}\n");
        System.out.println(STR."***GreedyHeuristicWithWeightedSumCriterion result stats:***\n\{WeightedSumHeuristicSolutions.statsToStr()}\n");

        // Writing stats to the file
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_stats.csv")) {
            writer.write("method_name,min,max,avg,sd\n");
            String str="greedy_two_regret_heuristic,";
            for (Double st: TwoRegretSolutions.getAllStats()){
                str += STR."\{st.toString()},";
            }
            writer.write(str.substring(0, str.length()-1)+'\n');

            str = "greedy_heuristic_with_weighted_sum_criterion,";
            for (Double st: WeightedSumHeuristicSolutions.getAllStats()){
                str += STR."\{st.toString()},";
            }
            writer.write(str.substring(0, str.length()-1)+'\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Best solution
        TwoRegretSolutions.bestSolutionToCsv(STR."evaluation/results/\{fileName}_two_regret.csv");
        WeightedSumHeuristicSolutions.bestSolutionToCsv(STR."evaluation/results/\{fileName}_weighted_sum.csv");
    }
}