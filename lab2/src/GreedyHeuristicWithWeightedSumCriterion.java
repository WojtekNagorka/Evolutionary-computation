import java.util.List;

public class GreedyHeuristicWithWeightedSumCriterion extends TSPSolver{
    public GreedyHeuristicWithWeightedSumCriterion(double[][] distanceMatrix, List<Node> nodes){
        super(distanceMatrix, nodes);
    }

    public Result solve(int startIndex){ // change void to Result
        return Result();
    }
}
