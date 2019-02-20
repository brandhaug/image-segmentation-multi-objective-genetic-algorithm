package GeneticAlgorithm;


import Utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class Individual {
    private List<Integer> chromosome; // List of genes (pixels)
    private List<Segment> segments = new ArrayList<>(); // List of segments (set of pixels)
    private double overallDeviation; // Objective function 1
    private double connectivity; // Objective function 2
    private double fitness;

    Individual(List<Pixel> pixels, List<Integer> initialChromosome, double initialColorDistanceThreshold) {
        this.chromosome = new ArrayList<>(initialChromosome);
        generateInitialIndividual(pixels, initialColorDistanceThreshold);
    }

    /**
     * Minimum Spanning Tree (MST)
     * TODO: Optional Instead of just checking if pixelsLeft contains neighbor in L36, wait with removing pixel in L3 and check if this neighbor relation is better than current neighbor relation in list
     */
    private void generateInitialIndividual(List<Pixel> pixels, double initialColorDistanceThreshold) {
        System.out.println("Generating Initial Individual");
        final long startTime = System.currentTimeMillis();
        List<PixelNeighbor> possibleNeighbors = new ArrayList<>();
        List<Pixel> pixelsLeft = new ArrayList<>(pixels); // Remove chosen vertices to make randomIndex effective

        while (pixelsLeft.size() != 0) {
            int randomIndex = Utils.randomIndex(pixelsLeft.size());
            Pixel randomPixel = pixelsLeft.get(randomIndex); // Random first best pixel
            pixelsLeft.remove(randomPixel);

            for (PixelNeighbor neighbor : randomPixel.getNeighbors()) { // TODO: replace contains with boolean variable on pixel
                if (neighbor.getColorDistance() < initialColorDistanceThreshold && pixelsLeft.contains(neighbor.getNeighbor())) {
                    possibleNeighbors.add(neighbor);
                    pixelsLeft.remove(neighbor.getNeighbor());
                }
            }

            Segment segment = new Segment();
            segment.addPixel(randomPixel);
            randomPixel.setSegment(segment);


            while (possibleNeighbors.size() != 0) {
                possibleNeighbors.sort(Comparator.comparingDouble(PixelNeighbor::getColorDistance)); // Sort by colorDistance // TODO: Can this be done any other way? Maybe add in sorted orderIndi

                PixelNeighbor bestPixelNeighbor = possibleNeighbors.get(0);
                Pixel bestPixel = bestPixelNeighbor.getPixel();
                Pixel bestNeighbor = bestPixelNeighbor.getNeighbor(); // Best neighbor of best pixel

                possibleNeighbors.remove(bestPixelNeighbor);
//                chromosome.set(bestPixel.getId(), bestNeighbor.getId());// Update chromosome: ID == Index
                chromosome.set(bestNeighbor.getId(), bestPixel.getId());// Update chromosome: ID == Index
                segment.addPixel(bestNeighbor);
                bestNeighbor.setSegment(segment);

                for (PixelNeighbor neighbor : bestNeighbor.getNeighbors()) { // Make Neighbors of bestNeighbor available for selection
                    if (neighbor.getColorDistance() < initialColorDistanceThreshold && pixelsLeft.contains(neighbor.getNeighbor())) { // TODO: replace contains with boolean variable on pixel
                        possibleNeighbors.add(neighbor);
                        pixelsLeft.remove(neighbor.getNeighbor());
                    }
                }
            } // Possible neighbors empty (one segmentation finished)

            segments.add(segment);
        }
        System.out.println(segments.size() + " segments created in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
    }

    List<Segment> getSegments() {
        return segments;
    }

    void calculateObjectiveFunctions() {
        overallDeviation = 0.0;
        connectivity = 0.0;

        for (Segment segment : segments) {
            segment.calculateObjectiveFunctions();
            overallDeviation += segment.getOverallDeviation();
            connectivity +=  segment.getConnectivity();
        }
    }

    double getOverallDeviation() {
        return overallDeviation;
    }

    double getConnectivity() {
        return connectivity;
    }

    public double getFitness() {
        return fitness;
    }
}
