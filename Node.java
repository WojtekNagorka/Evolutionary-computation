public class Node {
    private int x;
    private int y;
    private int cost;

    // 🧱 Constructors
    public Node() {
    }

    public Node(int x, int y, int cost) {
        this.x = x;
        this.y = y;
        this.cost = cost;
    }

    // 🧭 Getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getCost() {
        return cost;
    }

    // 🛠️ Setters
    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    // 🔍 For debugging and printing
    @Override
    public String toString() {
        return "Node{" +
                "x=" + x +
                ", y=" + y +
                ", cost=" + cost +
                '}';
    }
}
