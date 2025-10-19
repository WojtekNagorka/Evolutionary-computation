import java.util.ArrayList;
import java.util.List;

public class GreedyHeuristicWithWeightedSumCriterion extends TSPSolver{
    public GreedyHeuristicWithWeightedSumCriterion(double[][] distanceMatrix, List<Node> nodes){
        super(distanceMatrix, nodes);
    }

    public Result solve(int startIndex){ // change void to Result
        List<Integer> route = new ArrayList<>();
        for (int i=0; i< 100; i++){
            route.add(i);
        }
        return new Result(route, 0);
    }
}
