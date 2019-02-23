package Utils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ImageUtils {

    public ImageUtils() {
    }

    public BufferedImage readImage(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        return ImageIO.read(new File(Objects.requireNonNull(classLoader.getResource("resources/images/" + fileName)).getFile()));
    }

    public Color[][] parseBufferedImageTo2DArray(BufferedImage bufferedImage) {
        Color[][] colorArr = new Color[bufferedImage.getHeight()][bufferedImage.getWidth()];

        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                colorArr[y][x] = new Color(bufferedImage.getRGB(x, y));
            }
        }

        return colorArr;
    }
}
