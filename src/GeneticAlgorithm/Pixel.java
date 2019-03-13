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
    private List<Edge> edges = new ArrayList<>(); // List of neighboring genes (based on Moore neighborhood) {E, W, N, S, NE, SE, NW, SW}

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

    List<Edge> getEdges() {
        return edges;
    }

    void addPixelNeighbor(Pixel neighbor, Direction direction) {
        double colorDistance = Utils.getEuclideanColorDistance(color, neighbor.getColor());
        Edge edge = new Edge(this, neighbor, colorDistance, direction);
        edges.add(edge);
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    static void resetIdentification() {
        identification = 0;
    }
}
