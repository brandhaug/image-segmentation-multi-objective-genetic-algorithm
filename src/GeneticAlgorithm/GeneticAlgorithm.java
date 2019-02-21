package GeneticAlgorithm;

import Main.GuiController;
import javafx.scene.canvas.GraphicsContext;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Genetic Algorithm
 */
public class GeneticAlgorithm {

    // Parameters
    private final int populationSize = 5; // 20-100 dependent on problem
    private final double crossOverRate = 0.7; // 80%-95%
    private final double mutationRate = 0.01; // 0.5%-1%.
    private final int tournamentSize = 3; // Number of members in tournament selection
    private final double initialColorDistanceThreshold = 15.0; // Color Distance Threshold for initial population

    private Population population;
    private List<Pixel> pixels = new ArrayList<>();
    private List<Integer> initialChromosome = new ArrayList<>(); // All pixels pointing to self as default

    private Pixel[][] pixelArr;

    private int generation = 0;

    public GeneticAlgorithm(Color[][] colorArr) {
        Pixel.resetIdentification(); // Resets IDs in pixels, so it can be used for list retrieving
        pixelArr = generateGenes(colorArr); // TODO: Maybe not allowed to generate genes before pressing start? If so, move to tick where generation == 0
    }

    public void tick() {
        if (generation == 0) {
            findAndAddAllPixelNeighbors(pixelArr);
            population = new Population(pixels,
                    initialChromosome,
                    initialColorDistanceThreshold,
                    populationSize,
                    crossOverRate,
                    mutationRate,
                    tournamentSize);
        }

        population.tick();
        generation++;
    }

    public void save() {
    }

    public int getGeneration() {
        return generation;
    }

    public void render(GraphicsContext gc) {
        // javafx.scene.paint.Color is set in front of each color because of overlapping Color class from java.awt used in Pixel
        List<Segment> segments = population.getAlphaSegments();
        javafx.scene.paint.Color[] colors = {javafx.scene.paint.Color.RED, javafx.scene.paint.Color.ORANGE, javafx.scene.paint.Color.GOLD, javafx.scene.paint.Color.GREENYELLOW, javafx.scene.paint.Color.GREEN, javafx.scene.paint.Color.AQUA, javafx.scene.paint.Color.BLUE, javafx.scene.paint.Color.INDIGO, javafx.scene.paint.Color.VIOLET}; // Possible depot colors
        int colorIndex = 0;
        for (Segment segment : segments) {
            gc.setFill(colors[colorIndex]);
            for (Pixel pixel : segment.getPixels()) {
                gc.fillRect(pixel.getX(), pixel.getY(), 1, 1);
            }

            if (colorIndex == colors.length - 1) {
                colorIndex = 0;
            } else {
                colorIndex++;
            }
        }
    }

    private Pixel[][] generateGenes(Color[][] colorArr) {
        System.out.println("Generating genes");
        Pixel[][] pixelArr = new Pixel[GuiController.imageHeight][GuiController.imageWidth];

        for (int y = 0; y < GuiController.imageHeight; y++) {
            for (int x = 0; x < GuiController.imageWidth; x++) {
                Pixel pixel = new Pixel(x, y, colorArr[y][x]);
                pixelArr[y][x] = pixel;
                pixels.add(pixel);
                initialChromosome.add(pixel.getId());
            }
        }
        return pixelArr;
    }

    private void findAndAddAllPixelNeighbors(Pixel[][] pixelArr) {
        System.out.println("Finding and adding all PixelNeighbors");
        for (int y = 0; y < GuiController.imageHeight; y++) {
            for (int x = 0; x < GuiController.imageWidth; x++) {
                Pixel pixel = pixelArr[y][x];

                if (x + 1 < GuiController.imageWidth) { // 1. East
                    pixel.addPixelNeighbor(pixelArr[y][x + 1]);
                }

                if (x - 1 >= 0) { // 2. West
                    pixel.addPixelNeighbor(pixelArr[y][x - 1]);
                }

                if (y - 1 >= 0) { // 3. North
                    pixel.addPixelNeighbor(pixelArr[y - 1][x]);
                }

                if (y + 1 < GuiController.imageHeight) { // 4. South
                    pixel.addPixelNeighbor(pixelArr[y + 1][x]);
                }

                if (y - 1 >= 0 && x + 1 < GuiController.imageWidth) { // 5. North East
                    pixel.addPixelNeighbor(pixelArr[y - 1][x + 1]);
                }

                if (y + 1 < GuiController.imageHeight && x + 1 < GuiController.imageWidth) { // 6. South East
                    pixel.addPixelNeighbor(pixelArr[y + 1][x + 1]);
                }

                if (y - 1 >= 0 && x - 1 >= 0) { // 7. North West
                    pixel.addPixelNeighbor(pixelArr[y - 1][x - 1]);
                }

                if (y + 1 < GuiController.imageHeight && x - 1 >= 0) { // 8. South West
                    pixel.addPixelNeighbor(pixelArr[y + 1][x - 1]);
                }
            }
        }
    }
}
