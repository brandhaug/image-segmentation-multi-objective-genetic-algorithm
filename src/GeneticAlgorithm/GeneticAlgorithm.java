package GeneticAlgorithm;

import Main.GuiController;
import javafx.scene.canvas.GraphicsContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GeneticAlgorithm {

    // Parameters
    private final int populationSize = 20; // 20-100 dependent on problem
    private final double crossOverRate = 0.7; // 80%-95%
    private final double mutationRate = 0.01; // 0.5%-1%.
    private final int tournamentSize = 3; // Number of members in tournament selection

    private Population population;
    private List<Pixel> pixels = new ArrayList<>();
    private List<Integer> initialChromosome = new ArrayList<>(); // All pixels pointing to self

    private Pixel[][] pixelArr;

    private int generation = 0;

    public GeneticAlgorithm(Color[][] imageArr) {
        pixelArr = generateGenes(imageArr);
        population = new Population(pixels,
                initialChromosome,
                populationSize,
                crossOverRate,
                mutationRate,
                tournamentSize);
    }

    public void tick() {
        if (generation == 0) {
            findAndAddAllPixelNeighbors(pixelArr);
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

    }

    private Pixel[][] generateGenes(Color[][] imageArr) {
        Pixel[][] pixelArr = new Pixel[GuiController.IMAGE_HEIGHT][GuiController.IMAGE_WIDTH];

        for (int y = 0; y < GuiController.IMAGE_HEIGHT; y++) {
            for (int x = 0; x < GuiController.IMAGE_WIDTH; x++) {
                Pixel pixel = new Pixel(imageArr[y][x]);
                pixelArr[y][x] = pixel;
                pixels.add(pixel);
                initialChromosome.add(pixel.getId());
            }
        }
        return pixelArr;
    }

    private void findAndAddAllPixelNeighbors(Pixel[][] pixelArr) {
        for (int y = 0; y < GuiController.IMAGE_HEIGHT; y++) {
            for (int x = 0; x < GuiController.IMAGE_WIDTH; x++) {
                Pixel pixel = pixelArr[y][x];

                if (x + 1 < GuiController.IMAGE_WIDTH) { // 1. East
                    pixel.addPixelNeighbor(pixelArr[y][x + 1]);
                }

                if (x - 1 >= 0) { // 2. West
                    pixel.addPixelNeighbor(pixelArr[y][x - 1]);
                }

                if (y - 1 >= 0) { // 3. North
                    pixel.addPixelNeighbor(pixelArr[y - 1][x]);
                }

                if (y + 1 < GuiController.IMAGE_HEIGHT) { // 4. South
                    pixel.addPixelNeighbor(pixelArr[y + 1][x]);
                }

                if (y - 1 >= 0 && x + 1 < GuiController.IMAGE_WIDTH) { // 5. North East
                    pixel.addPixelNeighbor(pixelArr[y - 1][x + 1]);
                }

                if (y + 1 < GuiController.IMAGE_HEIGHT && x + 1 < GuiController.IMAGE_WIDTH) { // 6. South East
                    pixel.addPixelNeighbor(pixelArr[y + 1][x + 1]);
                }

                if (y - 1 >= 0 && x - 1 >= 0) { // 7. North West
                    pixel.addPixelNeighbor(pixelArr[y - 1][x - 1]);
                }

                if (y + 1 < GuiController.IMAGE_HEIGHT && x - 1 >= 0) { // 8. South West
                    pixel.addPixelNeighbor(pixelArr[y + 1][x - 1]);
                }
            }
        }
    }
}
