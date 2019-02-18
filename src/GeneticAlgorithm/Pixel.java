package GeneticAlgorithm;


import Utils.Utils;

import java.awt.*;
import java.util.List;

/**
 * Represents one pixel
 */
class Pixel {
    private int id;
    private static int identification = 0;
    private Color color; // RGB value
    private List<PixelNeighbor> neighbors; // List of neighboring genes (based on Moore neighborhood) {E, W, N, S, NE, SE, NW, SW}
    private Pixel pointsTo; // Can point to neighbor or self


    Pixel(Color color) {
        this.id = identification++;
        this.color = color;
    }

    Pixel(Color color, List<PixelNeighbor> neighbors, Pixel pointsTo) {
        this.color = color;
        this.neighbors = neighbors;
        this.pointsTo = pointsTo;
    }

    int getId() {
        return id;
    }

    Color getColor() {
        return color;
    }

    List<PixelNeighbor> getNeighbors() {
        return neighbors;
    }

    void addPixelNeighbor(Pixel neighbor) {
        double colorDistance = Utils.getEuclideanColorDistance(color, neighbor.getColor());
        System.out.println(colorDistance);
        PixelNeighbor pixelNeighbor = new PixelNeighbor(neighbor, colorDistance);
        neighbors.add(pixelNeighbor);
    }
}
