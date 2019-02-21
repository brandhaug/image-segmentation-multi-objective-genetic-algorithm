package GeneticAlgorithm;

import Utils.Utils;

import java.util.*;

/**
 * Represents one chromosome
 */
class Individual extends Thread {
    // Lists
    private List<Integer> chromosome; // List of genes (pixels)
    private List<Segment> segments = new ArrayList<>(); // List of segments (set of pixels)

    // Objective functions
    private double overallDeviation; // Objective function 1
    private double connectivity; // Objective function 2
    private double fitness;

    // Initial lists (read only)
    private final List<Pixel> pixels;
    private final List<Integer> initialChromosome;

    // Initial parameters
    private double initialColorDistanceThreshold;

    // Non-dominated sorting
    private int dominatedCount = 0; // n: number of solutions which dominates individual
    private List<Individual> dominatedIndividuals = new ArrayList<>(); // S: set of solutions which individual dominates
    private int rank;

    private double crowdingDistance;

    Individual(List<Pixel> pixels, List<Integer> initialChromosome, double initialColorDistanceThreshold) {
        this.pixels = pixels;
        this.initialChromosome = initialChromosome;
        this.initialColorDistanceThreshold = initialColorDistanceThreshold;
    }

    @Override
    public void run() {
        this.chromosome = new ArrayList<>(initialChromosome);
        generateInitialIndividual(pixels, initialColorDistanceThreshold);
        calculateObjectiveFunctions();
    }

    /**
     * Minimum Spanning Tree (MST)
     * TODO: (Probably not necessary) Instead of just checking if pixelsLeft contains neighbor, wait with removing pixel in and check if this neighbor relation is better than current neighbor relation in list. This could potentially remove stochastic selection.
     */
    private void generateInitialIndividual(List<Pixel> pixels, double initialColorDistanceThreshold) {
        System.out.println("Generating Initial Individual");
        final long startTime = System.currentTimeMillis();

        // Possible neighbors is
        List<PixelNeighbor> possibleNeighbors = new ArrayList<>(); // Support array for all possible visits. Sorted by colorDistance
        List<Pixel> pixelsLeft = new ArrayList<>(pixels); // Support array for removing added pixels to make randomIndex effective


        boolean[] addedIds = new boolean[pixels.size()]; // Support array for seeing which pixels is already added. It removes the need for using the ineffective list.contains()
        Arrays.fill(addedIds, false);

        while (pixelsLeft.size() != 0) {
            Segment segment = new Segment();

            int randomIndex = Utils.randomIndex(pixelsLeft.size());
            Pixel randomPixel = pixelsLeft.get(randomIndex); // Random first best pixel

            // Update lists
            segment.addPixel(randomPixel);
            randomPixel.setSegment(segment);
            pixelsLeft.remove(randomPixel);
            addedIds[randomIndex] = true;

            for (PixelNeighbor neighbor : randomPixel.getPixelNeighbors()) { // Make Neighbors of randomPixel available for selection
                if (neighbor.getColorDistance() < initialColorDistanceThreshold && !addedIds[neighbor.getNeighbor().getId()]) {
                    possibleNeighbors.add(neighbor);
                    pixelsLeft.remove(neighbor.getNeighbor());
                    addedIds[neighbor.getNeighbor().getId()] = true;
                }
            }

            while (possibleNeighbors.size() != 0) {
                // Sort and get best neighbor
                possibleNeighbors.sort(Comparator.comparingDouble(PixelNeighbor::getColorDistance)); // Sort by colorDistance // TODO: Is sorted add more effective?
                PixelNeighbor bestPixelNeighbor = possibleNeighbors.get(0);
                Pixel bestPixel = bestPixelNeighbor.getPixel();
                Pixel bestNeighbor = bestPixelNeighbor.getNeighbor(); // Best neighbor of best pixel

                // Update lists
                possibleNeighbors.remove(bestPixelNeighbor);
                chromosome.set(bestNeighbor.getId(), bestPixel.getId()); // Update chromosome: ID == Index
                segment.addPixel(bestNeighbor);
                bestNeighbor.setSegment(segment);

                for (PixelNeighbor neighbor : bestNeighbor.getPixelNeighbors()) { // Make Neighbors of bestNeighbor available for selection
                    if (neighbor.getColorDistance() < initialColorDistanceThreshold && !addedIds[neighbor.getNeighbor().getId()]) {
                        possibleNeighbors.add(neighbor);
                        pixelsLeft.remove(neighbor.getNeighbor());
                        addedIds[neighbor.getNeighbor().getId()] = true;
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
            connectivity += segment.getConnectivity();
        }
    }

    double getOverallDeviation() {
        return overallDeviation;
    }

    double getConnectivity() {
        return connectivity;
    }

    double getFitness() {
        return fitness;
    }

    int getDominatedCount() {
        return dominatedCount;
    }

    List<Individual> getDominatedIndividuals() {
        return dominatedIndividuals;
    }

    void addToDominatedIndividuals(Individual individual) {
        dominatedIndividuals.add(individual);
    }

    void setDominatedCount(int dominatedCount) {
        this.dominatedCount = dominatedCount;
    }

    void setRank(int rank) {
        this.rank = rank;
    }

    int getRank() {
        return rank;
    }

    double getCrowdingDistance() {
        return crowdingDistance;
    }
}
