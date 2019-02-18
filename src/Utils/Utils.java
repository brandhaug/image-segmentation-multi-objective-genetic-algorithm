package Utils;

import java.awt.*;

public class Utils {

    public static double getEuclideanColorDistance(Color color1, Color color2) {
        return Math.sqrt(Math.pow(color2.getRed() - color1.getRed(), 2)
                + Math.pow(color2.getGreen() - color1.getGreen(), 2)
                + Math.pow(color2.getBlue() - color1.getBlue(), 2));
    }
}
