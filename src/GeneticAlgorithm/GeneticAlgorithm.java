package GeneticAlgorithm;

import Main.GuiController;
import javafx.scene.canvas.GraphicsContext;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Controller for Genetic Algorithm
 */
public class GeneticAlgorithm {

    // Parameters
    final static int POPULATION_SIZE = 10; // 20-100 dependent on problem
    final static double MUTATION_RATE = 0.2; // 0.5%-1%.
    final static int TOURNAMENT_SIZE = 3; // Number of members in tournament selection

    final static int MIN_SEGMENTS = 3;
    final static int MAX_SEGMENTS = 15;

    final static int NUMBER_OF_SPLITS = 3;

    static final boolean AVERAGE_COLOR = true;

    // True = Multi objective GA
    // False = Weighted sum GA
    final static boolean MULTI_OBJECTIVE = false;

    // Initial lists (read only)
    static List<Pixel> pixels;

    private int generation = 0;
    private Population population;


    public GeneticAlgorithm(Color[][] colorArr) {
        pixels = new ArrayList<>();
        Pixel.resetIdentification(); // Resets IDs in pixels, so it can be used for list retrieving
        Pixel[][] pixelArr = generateGenes(colorArr);
        findAndAddAllPixelNeighbors(pixelArr);
    }

    public void tick() throws InterruptedException {
        if (generation == 0) {
            population = new Population();
        } else {
            population.tick(generation);
        }
        generation++;
    }

    public void render(GraphicsContext gc, GraphicsContext gc2, GraphicsContext gc3) {
        final long startTime = System.currentTimeMillis();

        List<Segment> segments = population.getRandomParetoSegments();
        gc2.setFill(javafx.scene.paint.Color.rgb(0, 255, 0));
        gc3.setFill(javafx.scene.paint.Color.BLACK);

        for (Segment segment : segments) {
            Color awtColor = segment.getAverageColor();
            javafx.scene.paint.Color fxColor = javafx.scene.paint.Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
            gc.setFill(fxColor);
            for (Pixel segmentPixel : segment.getSegmentPixels().values()) {
                gc.fillRect(segmentPixel.getX(), segmentPixel.getY(), 1, 1);
            }


            for (Pixel segmentPixel : segment.getBoundaryPixels()) {
                gc2.fillRect(segmentPixel.getX(), segmentPixel.getY(), 1, 1);
                gc3.fillRect(segmentPixel.getX(), segmentPixel.getY(), 1, 1);
            }
        }

        System.out.println("Pareto optimal solution rendered in " + ((System.currentTimeMillis() - startTime)) + "ms");
    }

    private Pixel[][] generateGenes(Color[][] colorArr) {
        final long startTime = System.currentTimeMillis();
        Pixel[][] pixelArr = new Pixel[GuiController.imageHeight][GuiController.imageWidth];

        for (int y = 0; y < GuiController.imageHeight; y++) {
            for (int x = 0; x < GuiController.imageWidth; x++) {
                Pixel pixel = new Pixel(x, y, colorArr[y][x]);
                pixelArr[y][x] = pixel;
                pixels.add(pixel);
            }
        }

        System.out.println("Genes generated in " + ((System.currentTimeMillis() - startTime)) + "ms");
        return pixelArr;
    }

    private void findAndAddAllPixelNeighbors(Pixel[][] pixelArr) {
        final long startTime = System.currentTimeMillis();

        for (int y = 0; y < GuiController.imageHeight; y++) {
            for (int x = 0; x < GuiController.imageWidth; x++) {
                Pixel pixel = pixelArr[y][x];

                if (x + 1 < GuiController.imageWidth) { // 1. East
                    pixel.addPixelNeighbor(pixelArr[y][x + 1], Direction.EAST);
                }

                if (x - 1 >= 0) { // 2. West
                    pixel.addPixelNeighbor(pixelArr[y][x - 1], Direction.WEST);
                }

                if (y - 1 >= 0) { // 3. North
                    pixel.addPixelNeighbor(pixelArr[y - 1][x], Direction.NORTH);
                }

                if (y + 1 < GuiController.imageHeight) { // 4. South
                    pixel.addPixelNeighbor(pixelArr[y + 1][x], Direction.SOUTH);
                }

                if (y - 1 >= 0 && x + 1 < GuiController.imageWidth) { // 5. North East
                    pixel.addPixelNeighbor(pixelArr[y - 1][x + 1], Direction.NORTH_EAST);
                }

                if (y + 1 < GuiController.imageHeight && x + 1 < GuiController.imageWidth) { // 6. South East
                    pixel.addPixelNeighbor(pixelArr[y + 1][x + 1], Direction.SOUTH_EAST);
                }

                if (y - 1 >= 0 && x - 1 >= 0) { // 7. North West
                    pixel.addPixelNeighbor(pixelArr[y - 1][x - 1], Direction.NORTH_WEST);
                }

                if (y + 1 < GuiController.imageHeight && x - 1 >= 0) { // 8. South West
                    pixel.addPixelNeighbor(pixelArr[y + 1][x - 1], Direction.SOUTH_WEST);
                }
            }
        }

        System.out.println("Neighbors added in " + ((System.currentTimeMillis() - startTime)) + "ms");
    }

    public double[][] getPopulationData() {
        List<Individual> individuals = population.getIndividuals();
        double[][] paretoData = new double[individuals.size()][3];

        final int RANK = 0;
        final int OVERALL_DEVIATION = 1;
        final int CONNECTIVITY = 2;

        for (int i = 0; i < individuals.size(); i++) {
            paretoData[i][RANK] = individuals.get(i).getRank();
            paretoData[i][OVERALL_DEVIATION] = individuals.get(i).getOverallDeviation();
            paretoData[i][CONNECTIVITY] = individuals.get(i).getConnectivity();
        }

        return paretoData;
    }

    public void saveParetoOptimalIndividualsToFile(String fileName, Timestamp timestamp) throws
            InterruptedException {
        List<Individual> individuals = population.getIndividuals();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        individuals.sort(Comparator.comparingDouble(Individual::getOverallDeviation));

        for (Individual individual : individuals) {
            if (individual.getRank() == 1) {
                executorService.execute(() -> {
                    try {
                        saveIndividualToImageFile(individual, individuals.indexOf(individual), fileName, timestamp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    private void saveIndividualToImageFile(Individual individual, int individualIndex, String fileName, Timestamp
            timestamp) throws IOException {
        BufferedImage image = new BufferedImage(GuiController.imageWidth, GuiController.imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(1, 1, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);

        for (Segment segment : individual.getSegments()) {
            segment.calculateConvexHull();
            for (Pixel segmentPixel : segment.getBoundaryPixels()) {
                graphics.fillRect(segmentPixel.getX(), segmentPixel.getY(), 1, 1);
            }
        }

        File jpegFile = new File("solution=" + fileName + "_time=" + timestamp.getTime() + "_gen=" + individual.getGeneration() + "_seg=" + individual.getSegments().size() + "_i=" + individualIndex + ".jpg");
        ImageIO.write(image, "jpg", jpegFile);
    }

    private void saveIndividualToTextFile(String fileName, Timestamp timestamp, int individualIndex,
                                          byte[] segmentLists) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("solution-file=" + fileName + "-time=" + timestamp.getTime() + "-index=" + individualIndex + ".txt"));
        for (int i = 0; i < segmentLists.length; i++) {
            writer.write(segmentLists[i]);

            if (i != 0 && i % GuiController.imageWidth == 0) {
                writer.newLine();
            } else if (i != segmentLists.length - 1) {
                writer.write(",");
            }
        }

        writer.close();
    }

    public int getGeneration() {
        return generation;
    }
}
