package Main;

import GeneticAlgorithm.GeneticAlgorithm;
import Utils.ImageUtils;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GuiController {

    // GUI
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private Canvas canvas;
    @FXML
    private VBox vbox;
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

    // Canvas
    private int canvasWidth;
    private int canvasHeight;

    private int vboxWidth = 200;

    public static int imageWidth;
    public static int imageHeight;

    private GraphicsContext gc; // Used to draw on canvas

    private BufferedImage bufferedImage;

    // States
    private boolean paused = true;
    private boolean initialized = false;

    // Settings
    public final static boolean RENDER = true;

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
                    tick(startNanoTime, currentNanoTime);
                    render();
                }
            }
        }.start();
    }

    private void tick(long startNanoTime, long currentNanoTime) {
        try {
            ga.tick();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        updateGUI(startNanoTime, currentNanoTime);
    }

    private void render() {
        if (RENDER) {
            gc.clearRect(0, 0, canvasWidth, canvasHeight); // Clear canvas
            ga.render(gc); // Renders alphaSolution of Population in GeneticAlgorithm
        }
    }

    private void renderImage() {
        Image image = SwingFXUtils.toFXImage(this.bufferedImage, null);
        gc.drawImage(image, 0, 0);
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
        vbox.setLayoutX(canvasWidth);
        vbox.setPrefHeight(canvasHeight);
        vbox.setPrefWidth(vboxWidth);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Pareto optimal solutions saved in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
    }
}
