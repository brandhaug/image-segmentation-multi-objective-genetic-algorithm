package Utils;

import java.awt.*;
import java.util.Random;

public class Utils {

    public static double getEuclideanColorDistance(Color color1, Color color2) {
        return Math.sqrt(Math.pow(color2.getRed() - color1.getRed(), 2)
                + Math.pow(color2.getGreen() - color1.getGreen(), 2)
                + Math.pow(color2.getBlue() - color1.getBlue(), 2));
    }

    /**
     * @param limit
     * @return A random int [0, limit>
     */
    public static int randomIndex(int limit) {
        Random random = new Random();
        return limit == 0 ? 0 : random.nextInt(limit);
    }

}
