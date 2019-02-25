package GeneticAlgorithm;

import Main.GuiController;
import javafx.scene.canvas.GraphicsContext;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Controller for Genetic Algorithm
 */
public class GeneticAlgorithm {

    // Parameters
    final static int populationSize = 50; // 20-100 dependent on problem
    //        private final double crossOverRate = 0.7; // 80%-95%
    final static double mutationRate = 0.2; // 0.5%-1%.
    final static int tournamentSize = 3; // Number of members in tournament selection

    final static int minSegments = 3;
    final static int maxSegments = 50;

    final static int numberOfSplits = 3;

    private Population population;

    // Initial lists (read only)
    static List<Pixel> pixels;
    static List<Integer> initialChromosome; // All pixels pointing to self as default

    private Pixel[][] pixelArr;

    private int generation = 0;

    public GeneticAlgorithm(Color[][] colorArr) {
        pixels = new ArrayList<>();
        initialChromosome = new ArrayList<>();
        Pixel.resetIdentification(); // Resets IDs in pixels, so it can be used for list retrieving
        pixelArr = generateGenes(colorArr);
    }

    public void tick() throws InterruptedException {
        if (generation == 0) {
            findAndAddAllPixelNeighbors(pixelArr);
            population = new Population();
        } else {
            population.tick();
        }
        generation++;
    }

    public void render(GraphicsContext gc) {
        final long startTime = System.currentTimeMillis();

        List<Segment> segments = population.getRandomParetoSegments();
        for (Segment segment : segments) {
            Color awtColor = segment.getAverageColor();
            javafx.scene.paint.Color fxColor = javafx.scene.paint.Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
            gc.setFill(fxColor);
            for (Map.Entry<Integer, Pixel> entry : segment.getSegmentPixels().entrySet()) {
                Pixel segmentPixel = entry.getValue();
                gc.fillRect(segmentPixel.getX(), segmentPixel.getY(), 1, 1);
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
                initialChromosome.add(pixel.getId());
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

    public void saveParetoOptimalIndividualsToFile(String fileName, Timestamp timestamp) throws InterruptedException {
        List<Individual> individuals = population.getIndividuals();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (Individual individual : individuals) {
            if (individual.getRank() == 1) {
                executorService.execute(() -> {
                    try {
                        saveParetoOptimalIndividualToFile(individual, individuals.indexOf(individual), fileName, timestamp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    private void saveParetoOptimalIndividualToFile(Individual individual, int individualIndex, String fileName, Timestamp timestamp) throws IOException {
        byte[] segmentLists = new byte[pixels.size()];
        Arrays.fill(segmentLists, (byte) 255);

        for (Segment segment : individual.getSegments()) {
            for (Map.Entry<Integer, Pixel> entry : segment.getSegmentPixels().entrySet()) {
                Pixel segmentPixel = entry.getValue();
                boolean mostEast = true;
                boolean mostWest = true;
                boolean mostSouth = true;
                boolean mostNorth = true;
                boolean add = true;

                for (Map.Entry<Integer, Pixel> entry2 : segment.getSegmentPixels().entrySet()) {
                    Pixel segmentPixelToCompare = entry2.getValue();
                    if (segmentPixel != segmentPixelToCompare) { // Not same pixel
                        if (segmentPixel.getY() == segmentPixelToCompare.getY()) { // Same y
                            if (segmentPixel.getX() < segmentPixelToCompare.getX()) { // Pixel is west of pixelToCompare
                                mostEast = false;
                            } else if (segmentPixel.getX() > segmentPixelToCompare.getX()) { // Pixel is east of pixelToCompare
                                mostWest = false;
                            }
                        } else if (segmentPixel.getX() == segmentPixelToCompare.getX()) { // Same x
                            if (segmentPixel.getY() < segmentPixelToCompare.getY()) { // Pixel is north of pixelToCompare
                                mostSouth = false;
                            } else if (segmentPixel.getY() > segmentPixelToCompare.getY()) { // Pixel is south of pixelToCompare
                                mostNorth = false;
                            }
                        }

                        if (!mostEast && !mostWest && !mostNorth && !mostSouth) {
                            add = false;
                            break;
                        }
                    }
                } // PixelToCompare finished

                if (add) {
                    segmentLists[segmentPixel.getId()] = 0;
                }
            } // Segment finished
        } // Pareto individual finished

        saveIndividualToImageFile(fileName, timestamp, individualIndex + 1, segmentLists);
    }

    private void saveIndividualToImageFile(String fileName, Timestamp timestamp, int individualIndex, byte[] segmentLists) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(GuiController.imageWidth, GuiController.imageHeight, BufferedImage.TYPE_BYTE_GRAY);
        final byte[] dataBuffer = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        System.arraycopy(segmentLists, 0, dataBuffer, 0, segmentLists.length);

        File jpegFile = new File("solution-" + fileName + "-" + timestamp.getTime() + "-" + individualIndex + ".jpg");
        ImageIO.write(bufferedImage, "jpg", jpegFile);
    }

    private void saveIndividualToTextFile(String fileName, Timestamp timestamp, int individualIndex, byte[] segmentLists) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("solution-" + fileName + "-" + timestamp.getTime() + "-" + individualIndex + ".txt"));
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
