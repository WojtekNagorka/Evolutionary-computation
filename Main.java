import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        String filePath = "data/TSPA.csv"; // path to your CSV file
        List<Node> nodes = new ArrayList<>();
        SolutionSpace randomSolutions = new SolutionSpace();
        SolutionSpace nearestNeighboursAtEnd = new SolutionSpace();
        SolutionSpace nearestNeighboursFlexible = new SolutionSpace();

        // Read CSV into Node list
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

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
        for (int i = 0; i < n; i++){
            // 1. Random solution
            TSPSolver.Result randomResult = solver.randomSolution();
            randomSolutions.addSolution((randomResult));

            //NN at the end
            TSPSolver.Result nnAtTheEndResult = solver.nearestNeighborEnd(i);
            nearestNeighboursAtEnd.addSolution(nnAtTheEndResult);

            //NN Flexible
            TSPSolver.Result nnFlexibleResult = solver.nearestNeighborFlexible(i);
            nearestNeighboursAtEnd.addSolution(nnFlexibleResult);
        }


        int startIndex = 0;

        // Run all 4 algorithms
        System.out.println("\n=== Random Solution ===");
        TSPSolver.Result randomResult = solver.randomSolution();
        System.out.println(randomResult);

        System.out.println("\n=== Nearest Neighbor (End) ===");
        TSPSolver.Result nnEndResult = solver.nearestNeighborEnd(startIndex);
        System.out.println(nnEndResult);

        System.out.println("\n=== Nearest Neighbor (Flexible) ===");
        TSPSolver.Result nnFlexibleResult = solver.nearestNeighborFlexible(startIndex);
        System.out.println(nnFlexibleResult);

        System.out.println("\n=== Greedy Cycle ===");
        TSPSolver.Result greedyResult = solver.greedyCycle(startIndex);
        System.out.println(greedyResult);

        // Compare all solutions and print best one
        TSPSolver.Result best = randomResult;
        String bestName = "Random Solution";

        if (nnEndResult.getTotalCost() < best.getTotalCost()) {
            best = nnEndResult;
            bestName = "Nearest Neighbor (End)";
        }
        if (nnFlexibleResult.getTotalCost() < best.getTotalCost()) {
            best = nnFlexibleResult;
            bestName = "Nearest Neighbor (Flexible)";
        }
        if (greedyResult.getTotalCost() < best.getTotalCost()) {
            best = greedyResult;
            bestName = "Greedy Cycle";
        }

        System.out.println("\n=== Best Solution ===");
        System.out.println("Algorithm: " + bestName);
        System.out.println(best);
    }
}
