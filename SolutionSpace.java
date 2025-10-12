import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SolutionSpace {
    private List<List<Integer>> solutions;
    private List<Double> costs;
    public SolutionSpace(){
        solutions = new ArrayList<>();
        costs = new ArrayList<>();
    }

    public void addSolution(TSPSolver.Result sol){
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
        return sum/costs.size();
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
}
