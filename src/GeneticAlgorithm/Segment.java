package GeneticAlgorithm;

import Utils.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of pixels
 */
public class Segment {
    private List<Pixel> pixels = new ArrayList<>();
    private Color centroidColor;
    private double overallDeviation;
    private double connectivity;

    Segment() {

    }

    void addPixel(Pixel pixel) {
        pixels.add(pixel);
    }

    public List<Pixel> getPixels() {
        return pixels;
    }

    void calculateObjectiveFunctions() {
        overallDeviation = 0.0;
        calculateCentroidOfPixels();

        connectivity = 0.0;
        for (Pixel pixel : pixels) {
            for (int j = 0; j < pixel.getPixelNeighbors().size(); j++) {
                if (pixels.contains(pixel.getPixelNeighbors().get(j).getNeighbor())) { // TODO: Optimize performance by replacing this .contains() statement
                    connectivity += (1 / (double) (j + 1));
                }
            }

            overallDeviation += Utils.getEuclideanColorDistance(pixel.getColor(), centroidColor); // dist(i, μ)
        }
    }

    double getOverallDeviation() {
        return overallDeviation;
    }

    double getConnectivity() {
        return connectivity;
    }

    /**
     * Calculates the average color in segment
     * Used in overall deviation (μ)
     */
    private void calculateCentroidOfPixels() {
        int centroidRed = 0;
        int centroidGreen = 0;
        int centroidBlue = 0;

        for (Pixel pixel : pixels) {
            centroidRed += pixel.getColor().getRed();
            centroidGreen += pixel.getColor().getGreen();
            centroidBlue += pixel.getColor().getBlue();
        }

        centroidRed = centroidRed / pixels.size();
        centroidGreen = centroidGreen / pixels.size();
        centroidBlue = centroidBlue / pixels.size();

        centroidColor = new Color(centroidRed, centroidGreen, centroidBlue);
    }
}
