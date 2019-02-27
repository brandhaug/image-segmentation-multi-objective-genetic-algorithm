package Main;

import GeneticAlgorithm.GeneticAlgorithm;
import Utils.ImageUtils;

import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class GuiController {

    // GUI
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private Canvas canvas;
    @FXML
    private VBox vboxRight;
    @FXML
    private VBox vboxLeft;
    @FXML
    private Button startButton; // Toggles between "Start" and "Pause", depending on state
    @FXML
    private Button resetButton;
    @FXML
    private Button saveButton;
    @FXML
    private Label timeLabel; // Shows current time
    @FXML
    private Label generationLabel; // Shows generation in GeneticAlgorithm
    @FXML
    private ComboBox imageSelector; // Shows benchmark fitness for current map
    @FXML
    private HBox hbox;
    @FXML
    private ScatterChart scatterChart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;

    private XYChart.Series paretoSeries;
    private XYChart.Series restOfPopulationSeries;

    // Canvas
    private int canvasWidth;
    private int canvasHeight;

    private final int vboxWidth = 200;

    public static int imageWidth;
    public static int imageHeight;

    private GraphicsContext gc; // Used to draw on canvas

    private BufferedImage bufferedImage;

    // States
    private boolean paused = true;
    private boolean initialized = false;

    private String fileName;
    private GeneticAlgorithm ga;

    @FXML
    private void initialize() {
        if (!initialized) {
            initializeImageSelector();
        }

        ImageUtils imageUtils = new ImageUtils();

        try {
            final long startTime = System.currentTimeMillis();
            bufferedImage = imageUtils.readImage(fileName);
            imageWidth = bufferedImage.getWidth();
            imageHeight = bufferedImage.getHeight();
            initializeGUI();
            Color[][] colorArr = imageUtils.parseBufferedImageTo2DArray(bufferedImage);
            gc = canvas.getGraphicsContext2D();
            renderImage();
            System.out.println("Image read and rendered in " + ((System.currentTimeMillis() - startTime)) + "ms");
            ga = new GeneticAlgorithm(colorArr);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final long startNanoTime = System.nanoTime(); // Time when system starts
        initialized = true;

        new AnimationTimer() {
            public void handle(long currentNanoTime) {
                if (!paused) {
                    tick();
                    render(startNanoTime, currentNanoTime);
                }
            }
        }.start();
    }

    private void initializeGUI() {
        timeLabel.setText("Time: 0");
        generationLabel.setText("Generation: 0");
        startButton.setVisible(true);
        startButton.setText("Start");
        imageSelector.setVisible(true);
        canvasHeight = imageHeight;
        canvasWidth = imageWidth;
        canvas.setHeight(imageHeight);
        canvas.setWidth(imageWidth);
        vboxRight.setLayoutX(canvasWidth);
        vboxRight.setPrefHeight(canvasHeight * 2);
        vboxRight.setMaxWidth(vboxWidth);
        initializeScatterChart();
    }

    private void initializeImageSelector() {
        ClassLoader classLoader = getClass().getClassLoader();
        File folder = new File(Objects.requireNonNull(classLoader.getResource("resources/images")).getFile());
        File[] imageFiles = folder.listFiles();
        Arrays.sort(Objects.requireNonNull(imageFiles));

        if (imageFiles.length == 0) {
            throw new IllegalStateException("Map folder is empty");
        }

        List<String> mapNames = new ArrayList<>();

        for (File file : imageFiles) {
            if (file.isFile()) {
                mapNames.add(file.getName());
            }
        }

        fileName = mapNames.get(0);
        imageSelector.setItems(FXCollections.observableArrayList(mapNames));
        imageSelector.getSelectionModel().selectFirst();
    }

    private void initializeScatterChart() {
        vboxLeft.setPrefHeight(canvasHeight * 2);
        vboxRight.setPrefWidth(canvasWidth);
        xAxis.setLabel("Overall Deviation");
        yAxis.setLabel("Connectivity");
        paretoSeries = new XYChart.Series();
        restOfPopulationSeries = new XYChart.Series();
        scatterChart.getData().clear();
        scatterChart.getData().addAll(paretoSeries, restOfPopulationSeries);
    }

    private void renderImage() {
        WritableImage image = SwingFXUtils.toFXImage(this.bufferedImage, null);
        gc.drawImage(image, 0, 0);
    }

    private void tick() {
        try {
            ga.tick();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void render(long startNanoTime, long currentNanoTime) {
        gc.clearRect(0, 0, canvasWidth, canvasHeight); // Clear canvas
        ga.render(gc); // Renders a optimal solution of Population in Genetic Algorithm
        updateGUI(startNanoTime, currentNanoTime);
    }

    private void addToScartChart() {
        double[][] populationData = ga.getPopulationData();

        final int RANK = 0;
        final int OVERALL_DEVIATION = 1;
        final int CONNECTIVITY = 2;

        paretoSeries.getData().clear();
        restOfPopulationSeries.getData().clear();

        for (double[] data : populationData) {
            if (data[RANK] == 1) { // Pareto optimal
                paretoSeries.getData().add(new XYChart.Data(data[OVERALL_DEVIATION], data[CONNECTIVITY]));
            } else { // Rest of population
                restOfPopulationSeries.getData().add(new XYChart.Data(data[OVERALL_DEVIATION], data[CONNECTIVITY]));
            }
        }
    }

    private void updateGUI(long startNanoTime, long currentNanoTime) {
        double time = (currentNanoTime - startNanoTime) / 1000000000.0;
        generationLabel.setText("Generation: " + ga.getGeneration());
        timeLabel.setText("Time: " + (int) time);
    }

    @FXML
    private void togglePaused() {
        paused = !paused;

        if (paused) {
            startButton.setText("Start");
            saveButton.setVisible(true);
            imageSelector.setVisible(true);
            addToScartChart();
        } else {
            startButton.setText("Pause");
            saveButton.setVisible(false);
            imageSelector.setVisible(false);
        }
    }

    @FXML
    private void selectImage() {
        fileName = imageSelector.getValue().toString();
        reset();
    }

    @FXML
    public void reset() {
        paused = true;
        ga = null;
        initialize();
    }

    @FXML
    private void save() {
        final long startTime = System.currentTimeMillis();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String fileNameNoExtension = fileName.substring(0, fileName.lastIndexOf('.'));

        try {
            ga.saveParetoOptimalIndividualsToFile(fileNameNoExtension, timestamp);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Pareto optimal solutions saved in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
    }
}
