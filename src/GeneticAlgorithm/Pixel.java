package GeneticAlgorithm;


import Utils.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one pixel
 */
class Pixel {
    private static int identification = 0;
    private int id;
    private int x;
    private int y;
    private Color color; // RGB value
    private List<PixelNeighbor> pixelNeighbors = new ArrayList<>(); // List of neighboring genes (based on Moore neighborhood) {E, W, N, S, NE, SE, NW, SW}
    private Segment segment;

    Pixel(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.id = identification++;
        this.color = color;
    }

    int getId() {
        return id;
    }

    Color getColor() {
        return color;
    }

    List<PixelNeighbor> getPixelNeighbors() {
        return pixelNeighbors;
    }

    void addPixelNeighbor(Pixel neighbor) {
        double colorDistance = Utils.getEuclideanColorDistance(color, neighbor.getColor());
        PixelNeighbor pixelNeighbor = new PixelNeighbor(this, neighbor, colorDistance);
        pixelNeighbors.add(pixelNeighbor);
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    void setSegment(Segment segment) {
        this.segment = segment;
    }

    public Segment getSegment() {
        return segment;
    }

    static void resetIdentification() {
        identification = 0;
    }
}
