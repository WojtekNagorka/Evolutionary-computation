import java.util.List;

public class CandidateMovesLocalSearch extends TSPSolver{
    public CandidateMovesLocalSearch(double[][] distanceMatrix, List<Node> nodes){
        super(distanceMatrix, nodes);
    }

    public Result solve(List<Integer> route){
        // TODO: to samo co ostatnio tylko bierzemy TOP10 najblizszych node'ow, a nie wszystkie
        // TODO: ale jestem zbyt zmeczony zeby to gowno teraz robic
        System.out.println("lol");
        double totalCost = computeTotalCost(route);
        return new Result(route, totalCost);
    }
}
