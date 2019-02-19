package GeneticAlgorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of pixels
 */
public class Segment {
    private List<Pixel> pixels = new ArrayList<>();

    Segment() {

    }

    void addPixel(Pixel pixel) {
        pixels.add(pixel);
    }

    public List<Pixel> getPixels() {
        return pixels;
    }
}
