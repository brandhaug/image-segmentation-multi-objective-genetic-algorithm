package GeneticAlgorithm;

import Utils.Utils;

import java.awt.Color;
import java.util.*;

/**
 * Represents a set of Pixels
 */
class Segment {
    private Map<Integer, Pixel> segmentPixels;
    private Color averageColor;
    private double overallDeviation;
    private double connectivity;
    private List<Pixel> boundaryPixels;

    Segment() {
        segmentPixels = new HashMap<>();
    }

    /**
     * Calculates overallDeviation and connectivity
     */
    void calculateObjectiveFunctions() {
        overallDeviation = 0.0;
        calculateAverageColor();

        connectivity = 0.0;

        for (Pixel segmentPixel : segmentPixels.values()) {
            for (Edge edge : segmentPixel.getEdges()) {
                Pixel neighbor = edge.getNeighbor();

                if (!segmentPixels.containsKey(neighbor.getId())) {
                    connectivity += (double) 1 / segmentPixel.getEdges().size();
                }
            }

            overallDeviation += Utils.getEuclideanColorDistance(segmentPixel.getColor(), averageColor); // dist(i, μ)
        }
    }

    /**
     * The Centroid is the average position of all the points of an object.
     * Used in overall deviation (μ)
     * Also calculates the average color in segment used in drawing on canvas
     */
    void calculateAverageColor() {
        int averageRed = 0;
        int averageGreen = 0;
        int averageBlue = 0;

        for (Pixel segmentPixel : segmentPixels.values()) {
            if (GeneticAlgorithm.AVERAGE_COLOR) {
                averageRed += Math.pow(segmentPixel.getColor().getRed(), 2);
                averageGreen += Math.pow(segmentPixel.getColor().getGreen(), 2);
                averageBlue += Math.pow(segmentPixel.getColor().getBlue(), 2);
            } else {
                averageRed += segmentPixel.getColor().getRed();
                averageGreen += segmentPixel.getColor().getGreen();
                averageBlue += segmentPixel.getColor().getBlue();
            }
        }

        if (GeneticAlgorithm.AVERAGE_COLOR) {
            averageRed = (int) Math.sqrt((double) averageRed / segmentPixels.size());
            averageGreen = (int) Math.sqrt((double) averageGreen / segmentPixels.size());
            averageBlue = (int) Math.sqrt((double) averageBlue / segmentPixels.size());
        } else {
            averageRed = averageRed / segmentPixels.size();
            averageGreen = averageGreen / segmentPixels.size();
            averageBlue = averageBlue / segmentPixels.size();
        }
        averageColor = new Color(averageRed, averageGreen, averageBlue);
    }

    void calculateConvexHull() {
        boundaryPixels = new ArrayList<>();

        for (Pixel segmentPixel : segmentPixels.values()) {
            for (Edge edge : segmentPixel.getEdges()) {
                if ((edge.getDirection() == Direction.EAST || edge.getDirection() == Direction.SOUTH) &&
                        !segmentPixels.containsKey(edge.getNeighbor().getId())) {
                    boundaryPixels.add(segmentPixel);
                    break;
                }
            }
        }
    }

    void addSegmentPixel(Pixel pixel) {
        segmentPixels.put(pixel.getId(), pixel);
    }

    void addSegmentPixels(Collection<Pixel> pixels) {
        for (Pixel pixel : pixels) {
            addSegmentPixel(pixel);
        }
    }

    double getOverallDeviation() {
        return overallDeviation;
    }

    double getConnectivity() {
        return connectivity;
    }

    List<Pixel> getBoundaryPixels() {
        return boundaryPixels;
    }

    Color getAverageColor() {
        return averageColor;
    }

    Map<Integer, Pixel> getSegmentPixels() {
        return segmentPixels;
    }
}
