package Utils;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    public static int randomInt(int min, int max) {
        Random random = new Random();
        return min + random.nextInt(max - min + 1);
    }

    /**
     * @return A random double [0, 1]
     */
    public static double randomDouble() {
        Random random = new Random();
        return random.nextDouble();
    }

    /**
     * @return A random double between [min, max]
     */
    public static double randomDouble(double min, double max) {
        Random random = new Random();
        return min + (max - min) * random.nextDouble();
    }

    /**
     * Generates k - 1 partition indices used for splitting lists
     */
    public static int[] generatePartitionIndices(int size, int k) {
        int[] partitionIndices = new int[k - 1];

        for (int i = 0; i < k - 1; i++) {
            partitionIndices[i] = Utils.randomIndex(size);
        }

        Arrays.sort(partitionIndices);

        return partitionIndices;
    }

    /**
     * Rounds a double value to n decimals
     * https://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
     */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
