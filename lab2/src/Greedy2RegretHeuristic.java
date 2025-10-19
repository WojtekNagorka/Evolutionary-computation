import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Greedy2RegretHeuristic extends TSPSolver{
    public Greedy2RegretHeuristic(double[][] distanceMatrix, List<Node> nodes){
        super(distanceMatrix, nodes);
    }

    public Result solve(int startIndex){
        int n = nodes.size();
        List<Integer> route = new ArrayList<>();
        boolean[] used = new boolean[n];
        route.add(startIndex);
        route.add(startIndex);
        used[startIndex] = true;

        while (route.size() < targetCount) {
            List<List<Integer>> insertions = new ArrayList<>(); // sorted list of 2 best node insertions with costs
            List<List<Integer>> regrets = new ArrayList<>(); // <(node, value_of_regret, place_to_insert)>
            for (int i=0; i < n; i++) {
                if (!used[i]) {
                    Node curr = nodes.get(i);
                    for (int j = 0; j < route.size() - 1; j++) {
                        int prev = route.get(j);
                        int next = route.get(j + 1);
                        int cost = roundToInt(distanceMatrix[i][prev]) + roundToInt(distanceMatrix[i][next]) + curr.getCost();
                        insertions.add(Arrays.asList(j+1, cost));
                    }
                    insertions.sort(Comparator.comparingInt(a -> a.get(1)));
                    int regret;
                    if (insertions.size() < 2){
                        regret = insertions.getFirst().get(1);
                    }
                    else{
                        regret = insertions.get(1).get(1) - insertions.getFirst().get(1);
                    }
                    regrets.add(Arrays.asList(i, regret, insertions.getFirst().getFirst()));
                }
            }
            int nodeIndex = regrets.getFirst().getFirst();
            int insertPosition = regrets.getFirst().get(2);
            regrets.sort(Comparator.comparingInt(a -> a.get(1)));
            route.add(insertPosition, nodeIndex);
            used[regrets.getFirst().getFirst()] = true;
        }
        double totalCost = computeTotalCost(route);
        return new Result(route, totalCost);
    }
}
