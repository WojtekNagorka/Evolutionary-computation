package lab1.src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        String fileName = "TSPA";
        String filePath = STR."../data/\{fileName}.csv";
        List<Node> nodes = new ArrayList<>();

        SolutionSpace randomSolutions = new SolutionSpace();
        SolutionSpace nearestNeighboursAtEnd = new SolutionSpace();
        SolutionSpace nearestNeighboursFlexible = new SolutionSpace();
        SolutionSpace greedyCycleSolutions = new SolutionSpace();

        long randomTime = 0;
        long nnAtTheEndTime = 0;
        long nnFlexibleTime = 0;
        long greedyCycleTime = 0;

        // Read CSV into Node list
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

        // Initialize solver
        TSPSolver solver = new TSPSolver(dm.getMatrix(), nodes);
        for (int i = 0; i < n; i++) {
            System.out.println(STR."Iteration number:\{i + 1}");
            // 1. Random solution
            long start = System.nanoTime();
            TSPSolver.Result randomResult = solver.randomSolution();
            long end = System.nanoTime();
            randomSolutions.addSolution((randomResult));
            randomTime += end - start;

            //2. NN at the end
            start = System.nanoTime();
            TSPSolver.Result nnAtTheEndResult = solver.nearestNeighborEnd(i);
            end = System.nanoTime();
            nearestNeighboursAtEnd.addSolution(nnAtTheEndResult);
            nnAtTheEndTime += end - start;

            //3. NN Flexible
            start = System.nanoTime();
            TSPSolver.Result nnFlexibleResult = solver.nearestNeighborFlexible(i);
            end = System.nanoTime();
            nearestNeighboursFlexible.addSolution(nnFlexibleResult);
            nnFlexibleTime += end - start;

            //4. Greedy cycle
            start = System.nanoTime();
            TSPSolver.Result greedyCycleResult = solver.greedyCycle(i);
            end = System.nanoTime();
            greedyCycleSolutions.addSolution(greedyCycleResult);
            greedyCycleTime += end - start;
        }
        // Printing time
        System.out.println(STR."Random result time (ms): \{randomTime / 1_000_000}");
        System.out.println(STR."NN at the end result time (ms): \{nnAtTheEndTime / 1_000_000}");
        System.out.println(STR."NN flexible result time (ms): \{nnFlexibleTime / 1_000_000}\n");
        System.out.println(STR."Greedy cycle result time (ms): \{greedyCycleTime / 1_000_000}\n");

        // Writing times to the file
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_times.csv")) {
            writer.write("method_name,time\n");
            writer.write(STR."random_sol,\{randomTime / 1_000_000}\n");
            writer.write(STR."nn_at_end,\{nnAtTheEndTime / 1_000_000}\n");
            writer.write(STR."nn_flexible,\{nnFlexibleTime / 1_000_000}\n");
            writer.write(STR."greedy_cycle,\{greedyCycleTime / 1_000_000}\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Printing stats
        System.out.println(STR."***Random result stats:***\n\{randomSolutions.statsToStr()}\n");
        System.out.println(STR."***NN at the end result stats:***\n\{nearestNeighboursAtEnd.statsToStr()}\n");
        System.out.println(STR."***NN flexible result stats:***\n\{nearestNeighboursFlexible.statsToStr()}\n");
        System.out.println(STR."***Greedy cycle result stats:***\n\{greedyCycleSolutions.statsToStr()}\n");

        // Writing stats to the file
        try (FileWriter writer = new FileWriter(STR."evaluation/\{fileName}_stats.csv")) {
            writer.write("method_name,min,max,avg,sd\n");
            String str="random_sol,";
            for (Double st: randomSolutions.getAllStats()){
                str += STR."\{st.toString()},";
            }
            writer.write(str.substring(0, str.length()-1)+'\n');

            str = "nn_at_end,";
            for (Double st: nearestNeighboursAtEnd.getAllStats()){
                str += STR."\{st.toString()},";
            }
            writer.write(str.substring(0, str.length()-1)+'\n');

            str = "nn_flexible,";
            for (Double st: nearestNeighboursFlexible.getAllStats()){
                str += STR."\{st.toString()},";
            }
            writer.write(str.substring(0, str.length()-1)+'\n');

            str = "greedy_cycle,";
            for (Double st: greedyCycleSolutions.getAllStats()){
                str += STR."\{st.toString()},";
            }
            writer.write(str.substring(0, str.length()-1)+'\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Best solution
        randomSolutions.bestSolutionToCsv(STR."evaluation/results/\{fileName}_random.csv");
        nearestNeighboursAtEnd.bestSolutionToCsv(STR."evaluation/results/\{fileName}_nn_end.csv");
        nearestNeighboursFlexible.bestSolutionToCsv(STR."evaluation/results/\{fileName}_nn_flexible.csv");
        greedyCycleSolutions.bestSolutionToCsv(STR."evaluation/results/\{fileName}_greedy_cycle.csv");

//        int startIndex = 0;
//
//        System.out.println("\n=== Greedy Cycle ===");
//        TSPSolver.Result greedyResult = solver.greedyCycle(startIndex);
//        System.out.println(greedyResult);
    }
}
