import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;

public class SolutionSpace {
    private List<List<Integer>> solutions;
    private List<Double> costs;

    public SolutionSpace() {
        solutions = new ArrayList<>();
        costs = new ArrayList<>();
    }

    public void addSolution(Result sol) {
        solutions.add(sol.getRoute());
        costs.add(sol.getTotalCost());
    }

    public double getMin() {
        if (costs.isEmpty()) return 0.0;
        return Collections.min(costs);
    }

    public double getMax() {
        if (costs.isEmpty()) return 0.0;
        return Collections.max(costs);
    }

    public double getSd() {
        if (costs.isEmpty()) return 0.0;
        double sum = 0;
        double avg = getAvg();
        for (double n : costs) {
            sum += Math.pow((n - avg), 2);
        }
        return Math.sqrt(sum / costs.size());
    }

    public double getAvg() {
        if (costs.isEmpty()) return 0.0;
        double sum = 0;
        for (double n : costs) {
            sum += n;
        }
        return sum / costs.size();
    }

    public double roundToTwoDecimals(double number) {
        return (double) Math.round(number * 100) / 100;
    }

    public List<Double> getAllStats() {
        return Arrays.asList(getMin(), getMax(), roundToTwoDecimals(getAvg()), roundToTwoDecimals(getSd()));
    }

    public String statsToStr() {
        List<Double> stats = getAllStats();
        return "Min: " + stats.get(0) + "\n Max: " + stats.get(1) + "\n Avg: " + stats.get(2) + "\n Sd: " + stats.get(3);
    }

    public void bestSolutionToCsv(String filePath) {
        if (solutions.isEmpty()) return;

        double mini = getMin();
        List<Integer> bestSol = null;

        for (int i = 0; i < solutions.size(); i++) {
            if (costs.get(i) == mini) {
                bestSol = solutions.get(i);
                // Break once found to avoid overwriting with equal cost solutions (optional)
                break;
            }
        }

        if (bestSol == null) return;

        try (FileWriter writer = new FileWriter(filePath)) {
            for (Integer number : bestSol) {
                writer.write(number.toString());
                writer.write("\n");
            }
            System.out.println("CSV written to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printBestSolution(List<Node> nodes) {
        if (solutions.isEmpty()) return;

        double mini = getMin();
        List<Integer> bestSol = null;

        for (int i = 0; i < solutions.size(); i++) {
            if (costs.get(i) == mini) {
                bestSol = solutions.get(i);
                break;
            }
        }

        if (bestSol == null) return;

        for (int i = 0; i < bestSol.size() - 1; i++) {
            Node curr_node = nodes.get(bestSol.get(i));
            Node next_node = nodes.get(bestSol.get(i + 1));

            double length = Math.sqrt(Math.pow(curr_node.getX() - next_node.getX(), 2) +
                    Math.pow(curr_node.getY() - next_node.getY(), 2));

            System.out.println("Id: " + bestSol.get(i) +
                    ", X: " + curr_node.getX() +
                    ", Y: " + curr_node.getY() +
                    ", length: " + length +
                    ", cost: " + curr_node.getCost());
        }
    }
}