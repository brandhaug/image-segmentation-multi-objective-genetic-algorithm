package Utils;

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

    public List<Color> readAndParseImage(String fileName) throws IOException {
        BufferedImage image = readImage(fileName);
        List<Color> pixelList = parseImage(image);
        return pixelList;
    }

    private BufferedImage readImage(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        return ImageIO.read(new File(Objects.requireNonNull(classLoader.getResource("resources/images/" + fileName)).getFile()));
    }

    private List<Color> parseImage(BufferedImage image) {
        List<Color> pixelList = new ArrayList<>();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                pixelList.add(new Color(image.getRGB(x, y)));
            }
        }

        return pixelList;
    }
}
