package GeneticAlgorithm;

/**
 * Represents the neighborhood relationship between two pixels
 */
class Edge implements Comparable<Edge> {
    private Pixel pixel;
    private Pixel neighbor;
    private double colorDistance; // Euclidean Color Distance
    private Direction direction;

    Edge(Pixel pixel, Pixel neighbor, double colorDistance, Direction direction) {
        this.pixel = pixel;
        this.neighbor = neighbor;
        this.colorDistance = colorDistance;
        this.direction = direction;
    }

    @Override
    public int compareTo(Edge o) {
        if (this.colorDistance > o.colorDistance) {
            return 1;
        } else if (this.colorDistance < o.colorDistance) {
            return -1;
        }
        return 0;
    }

    Pixel getPixel() {
        return pixel;
    }

    Pixel getNeighbor() {
        return neighbor;
    }

    double getColorDistance() {
        return colorDistance;
    }

    Direction getDirection() {
        return direction;
    }
}
