package Utils;

import Main.GuiController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ImageUtils {

    public ImageUtils() {
    }

    public Color[][] readAndParseImage(String fileName) throws IOException {
        BufferedImage image = readImage(fileName);
        return parseImageTo2DArray(image);
    }

    public BufferedImage readImage(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        BufferedImage image = ImageIO.read(new File(Objects.requireNonNull(classLoader.getResource("resources/images/" + fileName)).getFile()));
        GuiController.IMAGE_WIDTH = image.getWidth();
        GuiController.IMAGE_HEIGHT = image.getHeight();
        return image;
    }

    private List<Color> parseImageToList(BufferedImage image) {
        List<Color> pixelList = new ArrayList<>();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                pixelList.add(new Color(image.getRGB(x, y)));
            }
        }

        return pixelList;
    }

    public Color[][] parseImageTo2DArray(BufferedImage image) {
        Color[][] imageArr = new Color[image.getHeight()][image.getWidth()];

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                imageArr[y][x] = new Color(image.getRGB(x, y));
            }
        }

        return imageArr;
    }
}
