package Utils;

import java.awt.Color;
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

    /**
     * @return A random double [0, 1]
     */
    public static double randomDouble() {
        Random random = new Random();
        return random.nextDouble();
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
     * Splits Chromosome in k parts
     */
    public static List<List<Integer>> splitRoute(List<Integer> chromosome, int[] partitionIndices, int k) {
        List<List<Integer>> parts = new ArrayList<>();

        for (int i = 0; i < k; i++) {
            parts.add(new ArrayList<>());
        }

        for (int i = 0; i < chromosome.size(); i++) {
            for (int j = 0; j < partitionIndices.length; j++) {
                if (i < partitionIndices[j]) {
                    parts.get(j).add(chromosome.get(i));
                    break;
                } else if (i >= partitionIndices[partitionIndices.length - 1]) {
                    parts.get(parts.size() - 1).add(chromosome.get(i));
                    break;
                }
            }
        }

        return parts;
    }

}
