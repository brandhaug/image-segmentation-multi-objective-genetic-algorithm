package GeneticAlgorithm;

import Main.GuiController;
import Utils.Utils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a set of Pixels
 */
class Segment {
    private HashMap<Integer, Pixel> segmentPixels;
    private Color averageColor;
    private Color centroidColor;
    private double overallDeviation;
    private double connectivity;

    Segment() {
        segmentPixels = new HashMap<>();
    }

    void addSegmentPixel(Pixel pixel) {
        segmentPixels.put(pixel.getId(), pixel);
    }

    HashMap<Integer, Pixel> getSegmentPixels() {
        return segmentPixels;
    }

    /**
     * Calculates overallDeviation and connectivity
     */
    void calculateObjectiveFunctions() {
        overallDeviation = 0.0;
        calculateCentroidCoordinate();

        connectivity = 0.0;

        for (Map.Entry<Integer, Pixel> entry : segmentPixels.entrySet()) {
            Pixel segmentPixel = entry.getValue();

            for (int j = 0; j < segmentPixel.getPixelNeighbors().size(); j++) {
                Pixel neighbor = segmentPixel.getPixelNeighbors().get(j).getNeighbor();

                if (!segmentPixels.containsKey(neighbor.getId())) {
                    connectivity += (1 / (double) (j + 1));
                }
            }

            overallDeviation += Utils.getEuclideanColorDistance(segmentPixel.getColor(), centroidColor); // dist(i, μ)
        }
    }

    double getOverallDeviation() {
        return overallDeviation;
    }

    double getConnectivity() {
        return connectivity;
    }

    /**
     * The Centroid is the average position of all the points of an object.
     * Used in overall deviation (μ)
     * Also calculates the average color in segment used in drawing on canvas
     */
    private void calculateCentroidCoordinate() {
        int averageX = 0;
        int averageY = 0;

        int averageRed = 0;
        int averageGreen = 0;
        int averageBlue = 0;

        for (Map.Entry<Integer, Pixel> entry : segmentPixels.entrySet()) {
            Pixel segmentPixel = entry.getValue();

            averageX += segmentPixel.getX();
            averageY += segmentPixel.getY();

            averageRed += segmentPixel.getColor().getRed();
            averageGreen += segmentPixel.getColor().getGreen();
            averageBlue += segmentPixel.getColor().getBlue();
        }

        averageX = averageX / segmentPixels.size();
        averageY = averageY / segmentPixels.size();

        averageRed = averageRed / segmentPixels.size();
        averageGreen = averageGreen / segmentPixels.size();
        averageBlue = averageBlue / segmentPixels.size();


        // Calculating index in list based on imageWidth
        int centroidPixelId = (GuiController.imageWidth * averageY) + averageX;
        Pixel centroidPixel = GeneticAlgorithm.pixels.get(centroidPixelId);

        if (centroidPixel == null) {
            throw new NullPointerException("CentroidPixel is null");
        }

        centroidColor = centroidPixel.getColor();
        averageColor = new Color(averageRed, averageGreen, averageBlue);
    }

    Color getCentroidColor() {
        return centroidColor;
    }

    Color getAverageColor() {
        return averageColor;
    }
}
