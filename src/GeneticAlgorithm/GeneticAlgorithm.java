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
    private final int populationSize = 10; // 20-100 dependent on problem
    private final double crossOverRate = 0.7; // 80%-95%
    private final double mutationRate = 0.01; // 0.5%-1%.
    private final int tournamentSize = 3; // Number of members in tournament selection
    private final double initialColorDistanceThreshold = 15.0; // Color Distance Threshold for initial population
    private final int splits = 2;

    private Population population;

    // Initial lists (read only)
    private final List<Pixel> pixels = new ArrayList<>();
    private final List<Integer> initialChromosome = new ArrayList<>(); // All pixels pointing to self as default

    private Pixel[][] pixelArr;

    private int generation = 0;

    public GeneticAlgorithm(Color[][] colorArr) {
        Pixel.resetIdentification(); // Resets IDs in pixels, so it can be used for list retrieving
        pixelArr = generateGenes(colorArr); // TODO: Maybe not allowed to generate genes before pressing start? If so, move to tick where generation == 0
    }

    public void tick() throws InterruptedException {
        if (generation == 0) {
            findAndAddAllPixelNeighbors(pixelArr);
            population = new Population(pixels,
                    initialChromosome,
                    initialColorDistanceThreshold,
                    populationSize,
                    crossOverRate,
                    mutationRate,
                    tournamentSize,
                    splits);
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
        List<Segment> segments = population.getAlphaSegments();
        for (Segment segment : segments) {
            Color awtColor = segment.getCentroidColor();
            javafx.scene.paint.Color fxColor = javafx.scene.paint.Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
            gc.setFill(fxColor);
            for (Pixel pixel : segment.getSegmentPixels()) {
                gc.fillRect(pixel.getX(), pixel.getY(), 1, 1);
            }
        }
    }

    private Pixel[][] generateGenes(Color[][] colorArr) {
        System.out.println("Generating genes");
        final long startTime = System.currentTimeMillis();
        Pixel[][] pixelArr = new Pixel[GuiController.imageHeight][GuiController.imageWidth];

        for (int y = 0; y < GuiController.imageHeight; y++) {
            for (int x = 0; x < GuiController.imageWidth; x++) {
                Pixel pixel = new Pixel(x, y, colorArr[y][x]);
                pixelArr[y][x] = pixel;
                pixels.add(pixel);
                initialChromosome.add(pixel.getId());
            }
        }

        System.out.println("Genes generated in " + ((System.currentTimeMillis() - startTime)) + "ms");
        return pixelArr;
    }

    private void findAndAddAllPixelNeighbors(Pixel[][] pixelArr) {
        System.out.println("Finding and adding all PixelNeighbors");
        final long startTime = System.currentTimeMillis();

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

        System.out.println("Neighbors added in " + ((System.currentTimeMillis() - startTime)) + "ms");
    }
}
