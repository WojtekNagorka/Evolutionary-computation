public class DistanceMatrix {

    private double[][] matrix; // distance matrix

    /**
     * Constructor: generates the distance matrix
     * @param x array of x-coordinates
     * @param y array of y-coordinates
     */
    public DistanceMatrix(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("x and y arrays must have the same length");
        }
        int n = x.length;
        matrix = new double[n][n];

        // compute Euclidean distances
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double dx = x[i] - x[j];
                double dy = y[i] - y[j];
                double distance = Math.sqrt(dx * dx + dy * dy);
                matrix[i][j] = Math.round(distance);
            }
        }
    }

    /**
     * Returns the computed distance matrix
     */
    public double[][] getMatrix() {
        return matrix;
    }

    /**
     * Returns distance between two points i and j
     */
    public double getDistance(int i, int j) {
        return matrix[i][j];
    }

    /**
     * Prints the distance matrix in readable form
     */
    public void printMatrix() {
        System.out.println("Distance Matrix:");
        for (double[] row : matrix) {
            for (double val : row) {
                System.out.printf("%.2f\t", val);
            }
            System.out.println();
        }
    }
}