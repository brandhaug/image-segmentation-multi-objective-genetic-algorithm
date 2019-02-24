package GeneticAlgorithm;

/**
 * Represents the neighborhood relationship between two pixels
 */
class PixelNeighbor implements Comparable<PixelNeighbor> {
    private Pixel pixel;
    private Pixel neighbor;
    private double colorDistance; // Euclidean Color Distance

    PixelNeighbor(Pixel pixel, Pixel neighbor, double colorDistance) {
        this.pixel = pixel;
        this.neighbor = neighbor;
        this.colorDistance = colorDistance;
    }

    @Override
    public int compareTo(PixelNeighbor o) {
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
}
