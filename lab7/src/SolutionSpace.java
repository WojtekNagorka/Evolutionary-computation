import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

public class SolutionSpace {
    private List<List<Integer>> solutions;
    private List<Double> costs;
    public SolutionSpace(){
        solutions = new ArrayList<>();
        costs = new ArrayList<>();
    }

    public void addSolution(Result sol){
        solutions.add(sol.getRoute());
        costs.add(sol.getTotalCost());
    }

    public double getMin(){
        return Collections.min(costs);
    }

    public double getMax(){
        return Collections.max(costs);
    }

    public double getSd(){
        double sum = 0;
        double avg = getAvg();
        for (double n : costs){
            sum += Math.pow((n - avg), 2);
        }
        return Math.sqrt(sum/costs.size());
    }

    public double getAvg(){
        double sum = 0;
        for (double n : costs){
            sum += n;
        }
        return sum/costs.size();
    }

    public double roundToTwoDecimals(double number){
        return (double) Math.round(number * 100) /100;
    }

    public List<Double> getAllStats(){
        return Arrays.asList(getMin(), getMax(), roundToTwoDecimals(getAvg()), roundToTwoDecimals(getSd()));
    }

    public String statsToStr(){
        List<Double> stats = getAllStats();
        return(STR."Min: \{stats.get(0)}\n Max: \{stats.get(1)}\n Avg: \{stats.get(2)}\n Sd: \{stats.get(3)}");
    }

    public void bestSolutionToCsv(String filePath){
        double mini = getMin();
        List<Integer> bestSol = null;

        for(int i = 0; i < solutions.size(); i++){
            if (costs.get(i) == mini){
                bestSol = solutions.get(i);
            }
        }
        try (FileWriter writer = new FileWriter(filePath)) {
            assert bestSol != null;
            for (Integer number : bestSol) {
                writer.write(number.toString());
                writer.write("\n");
            }
            System.out.println("CSV written as one row.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printBestSolution(List<Node> nodes){
        double mini = getMin();
        List<Integer> bestSol = null;

        for(int i = 0; i < solutions.size(); i++){
            if (costs.get(i) == mini){
                bestSol = solutions.get(i);
            }
        }

        assert bestSol != null;
        for (int i=0; i < bestSol.size()-1; i++){
            Node curr_node = nodes.get(bestSol.get(i));
            Node next_node = nodes.get(bestSol.get(i+1));
            System.out.println(STR."Id: \{bestSol.get(i)}, X: \{curr_node.getX()}, Y: \{curr_node.getY()}, " +
                    STR."length: \{Math.sqrt(Math.pow(curr_node.getX() - next_node.getX(), 2) + Math.pow(curr_node.getY() - next_node.getY(), 2))}, " +
                    STR."cost: \{curr_node.getCost()}");
        }
    }
}
