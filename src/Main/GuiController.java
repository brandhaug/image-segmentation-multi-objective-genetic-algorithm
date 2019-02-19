package Main;

import GeneticAlgorithm.GeneticAlgorithm;
import Utils.ImageUtils;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
    public final static int CANVAS_WIDTH = 500; // Canvas width set in View.fxml
    public final static int CANVAS_HEIGHT = 500; // Canvas width set in View.fxml
    public final static int CANVAS_MARGIN = 10; // The margin avoids that extreme points are drawn outside canvas

    public static int IMAGE_WIDTH;
    public static int IMAGE_HEIGHT;

    private GraphicsContext gc; // Used to draw on canvas

    private BufferedImage image;


    // States
    private boolean paused = true;
    private boolean initialized = false;

    private String fileName;
    private GeneticAlgorithm ga;

    @FXML
    private void initialize() {
        initializeGUI();
        gc = canvas.getGraphicsContext2D();
        ImageUtils imageUtils = new ImageUtils();

        try {
            image = imageUtils.readImage(fileName);
            Color[][] imageArr = imageUtils.parseImageTo2DArray(image);
            renderImage();
            ga = new GeneticAlgorithm(imageArr);
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
        ga.tick();
        updateGUI(startNanoTime, currentNanoTime);
    }

    private void render() {
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT); // Clear canvas
        renderImage();
        ga.render(gc); // Renders alphaSolution of Population in GeneticAlgorithm
    }

    private void renderImage() {
//        gc.drawImage(SwingFXUtils.toFXImage(image, null), 0, 0);
    }

    private void initializeGUI() {
        timeLabel.setText("Time: 0");
        generationLabel.setText("Generation: 0");
        startButton.setVisible(true);
        startButton.setText("Start");
        imageSelector.setVisible(true);

        if (!initialized) {
            initializeImageSelector();
        }
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
        ga.save();
    }
}
