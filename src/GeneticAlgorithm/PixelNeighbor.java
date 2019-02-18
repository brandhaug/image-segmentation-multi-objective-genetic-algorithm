package GeneticAlgorithm;

public class PixelNeighbor {
    private Pixel pixel;
    private double colorDistance; // Euclidean Color Distance

    PixelNeighbor(Pixel pixel, double colorDistance) {
        this.pixel = pixel;
        this.colorDistance = colorDistance;
    }

    Pixel getPixel() {
        return pixel;
    }

    double getColorDistance() {
        return colorDistance;
    }
}
