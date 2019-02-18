package GeneticAlgorithm;


import java.awt.*;
import java.util.List;

/**
 * Represents one pixel
 */
public class Gene {
    private Color color; // RGB value
    private List<Gene> neighbors; // List of neighboring genes (based on Moore neighborhood) {E, W, N, S, NE, SE, NW, SW}
    private Gene pointsTo; // Can point to neighbor or self

}
