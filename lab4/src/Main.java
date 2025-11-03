import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        conductExperiments("TSPA");
        conductExperiments("TSPB");
    }

    public static void conductExperiments(String instance){
        String filePath = STR."../data/\{instance}.csv";
        List<Node> nodes = readCSV(filePath);
        Result improvedSolution;

        long start;
        long end;
        long timeNodesExchangeFalse = 0;
        long timeNodesExchangeTrue = 0;

        int n = nodes.size();
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = nodes.get(i).getX();
            y[i] = nodes.get(i).getY();
        }

        DistanceMatrix dm = new DistanceMatrix(x, y);
        System.out.println(STR."=== Distance Matrix for \{instance} ===");
        dm.printMatrix();

        SolutionSpace solutionsNodeExchangeFalse = new SolutionSpace();
        SolutionSpace solutionsNodeExchangeTrue = new SolutionSpace();

        RandomSearch RandomHeuristic = new RandomSearch(dm.getMatrix(), nodes);
        CandidateMovesLocalSearch CandidateMovesHeuristicUseNodesExchangeFalse = new CandidateMovesLocalSearch(dm.getMatrix(), nodes, false);
        CandidateMovesLocalSearch CandidateMovesHeuristicUseNodesExchangeTrue = new CandidateMovesLocalSearch(dm.getMatrix(), nodes, true);

        for (int iteration=0; iteration<200; iteration++) {
            // 0. Initial solution
            List<Integer> randomSolution = RandomHeuristic.solve().getRoute();

            // 1. Local Search with Candidates Moves, useNodesExchange = false
            start = System.nanoTime();
            improvedSolution = CandidateMovesHeuristicUseNodesExchangeFalse.solve(randomSolution);
            end = System.nanoTime();
            solutionsNodeExchangeFalse.addSolution(improvedSolution);
            timeNodesExchangeFalse += (end - start);

            //2. Local Search with Candidates Moves, useNodesExchange = true
            start = System.nanoTime();
            improvedSolution = CandidateMovesHeuristicUseNodesExchangeTrue.solve(randomSolution);
            end = System.nanoTime();
            solutionsNodeExchangeTrue.addSolution(improvedSolution);
            timeNodesExchangeTrue += (end - start);
        }

        // 3. Print statistics
        System.out.println(solutionsNodeExchangeFalse.statsToStr());
        System.out.println(solutionsNodeExchangeTrue.statsToStr());

        // 4. Print times
        System.out.println(timeNodesExchangeFalse/1_000_000);
        System.out.println(timeNodesExchangeTrue/1_000_000);

        // 5. Write the best solutions to CSV
        solutionsNodeExchangeFalse.bestSolutionToCsv(STR."evaluation/results/\{instance}_candidate_moves_heuristic_without_nodes_exchange.csv");
        solutionsNodeExchangeTrue.bestSolutionToCsv(STR."evaluation/results/\{instance}_candidate_moves_heuristic_with_nodes_exchange.csv");

        // 6. Write times to CSV
        List<String> textToWrite = Arrays.asList(
                "method,time\n",
                STR."local_search_with_candidate_moves_false,\{timeNodesExchangeFalse}\n",
                STR."local_search_with_candidate_moves_true,\{timeNodesExchangeTrue}\n"
                );
        String times_csv_path = STR."evaluation/\{instance}_times.csv";
        writeListToCSV(times_csv_path, textToWrite);

        // 7. Write stats to CSV
        List<Double> statsFalse = solutionsNodeExchangeFalse.getAllStats();
        List<Double> statsTrue = solutionsNodeExchangeTrue.getAllStats();
        textToWrite = Arrays.asList(
                "method_name,min,max,avg,sd\n",
                STR."candidate_moves_heuristic_without_nodes_exchange,\{statsFalse.getFirst()},\{statsFalse.get(1)},\{statsFalse.get(2)},\{statsFalse.get(3)}\n",
                STR."candidate_moves_heuristic_with_nodes_exchange,\{statsTrue.getFirst()},\{statsTrue.get(1)},\{statsTrue.get(2)},\{statsTrue.get(3)}"
        );
        String stats_csv_path = STR."evaluation/\{instance}_stats.csv";
        writeListToCSV(stats_csv_path, textToWrite);

        // 8. DONE
        System.out.println("DONE!");
    }

    public static List<Node> readCSV(String filePath){
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

    public static void writeListToCSV(String filePath, List<String> textToWrite){
        try (FileWriter writer = new FileWriter(filePath)) {
            for (String line : textToWrite) {
                writer.write(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}