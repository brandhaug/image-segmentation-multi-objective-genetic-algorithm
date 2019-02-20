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
            double tempConnectivity = 0.0;
            int L = pixel.getNeighbors().size();
            for (int x = 0; x < L; x++) {
                if (x == L - 1) {
                    tempConnectivity += (L * (1 / (double) x));  // L * x
                }
            }

            connectivity += pixels.size() * tempConnectivity; // pixels.size() = N
            overallDeviation += Utils.getEuclideanColorDistance(pixel.getColor(), centroidColor);
        }
    }

    double getOverallDeviation() {
        return overallDeviation;
    }

    double getConnectivity() {
        return connectivity;
    }

    /**
     * Calculates the average color
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
