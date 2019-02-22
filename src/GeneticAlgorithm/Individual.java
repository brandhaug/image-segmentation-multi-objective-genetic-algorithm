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

    // Initial parameters
    private double initialColorDistanceThreshold;

    // Non-dominated sorting
    private int dominatedCount = 0; // n: number of solutions which dominates individual
    private List<Individual> dominatedIndividuals = new ArrayList<>(); // S: set of solutions which individual dominates
    private int rank;

    private double crowdingDistance;

    private boolean initialize;

    Individual(double initialColorDistanceThreshold) {
        this.chromosome = new ArrayList<>(GeneticAlgorithm.initialChromosome);
        this.initialColorDistanceThreshold = initialColorDistanceThreshold;
        this.initialize = true;
    }

    Individual(List<Integer> chromosome) {
        this.chromosome = chromosome;
        this.initialize = false;
    }

    @Override
    public void run() {
        if (initialize) {
            generateInitialIndividual();
        } else {
            calculateSegments();
        }

        calculateObjectiveFunctions();
    }

    /**
     * Minimum Spanning Tree (MST)
     * TODO: (Probably not necessary) Instead of just checking if pixelsLeft contains neighbor, wait with removing pixel in and check if this neighbor relation is better than current neighbor relation in list. This could potentially remove stochastic selection.
     */
    private void generateInitialIndividual() {
        List<PixelNeighbor> possibleNeighbors = new ArrayList<>(); // Support array for all possible visits. Sorted by colorDistance
        List<Pixel> pixelsLeft = new ArrayList<>(GeneticAlgorithm.pixels); // Support array for removing added pixels to make randomIndex effective


        boolean[] addedIds = new boolean[GeneticAlgorithm.pixels.size()]; // Support array for seeing which pixels is already added. It removes the need for using the ineffective list.contains()
        Arrays.fill(addedIds, false);

        while (pixelsLeft.size() != 0) {
            Segment segment = new Segment();

            int randomIndex = Utils.randomIndex(pixelsLeft.size());
            Pixel randomPixel = pixelsLeft.get(randomIndex); // Random first best pixel

            // Update lists
            segment.addSegmentPixel(randomPixel);
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
                segment.addSegmentPixel(bestNeighbor);
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
    }

    /**
     * Creates segments based on chromosome
     */
    private void calculateSegments() {
        System.out.println("Calculating segments");
        final long startTime = System.currentTimeMillis();

        List<Pixel> pixelsInSegment = new ArrayList<>(); // Support array for all possible visits. Sorted by colorDistance
        List<Pixel> pixelsLeft = new ArrayList<>(GeneticAlgorithm.pixels); // Support array for removing added pixels to make randomIndex effective

        while (pixelsLeft.size() != 0) {
            Segment segment = new Segment();

            int randomIndex = Utils.randomIndex(pixelsLeft.size());
            Pixel randomPixel = pixelsLeft.get(randomIndex); // Random first best pixel

            // Update lists
            segment.addSegmentPixel(randomPixel);
            randomPixel.setSegment(segment);
            pixelsLeft.remove(randomPixel);

            for (PixelNeighbor neighbor : randomPixel.getPixelNeighbors()) { // Make Neighbors of randomPixel available for selection
                if (chromosome.get(neighbor.getNeighbor().getId()) == randomPixel.getId()) {
                    segment.addSegmentPixel(neighbor.getNeighbor());
                    pixelsInSegment.add(neighbor.getNeighbor());
                    pixelsLeft.remove(neighbor.getNeighbor());
                }
            }

            while (pixelsInSegment.size() != 0) {
                for (Pixel pixel : pixelsInSegment) {
                    for (PixelNeighbor neighbor : randomPixel.getPixelNeighbors()) {
                        if (chromosome.get(neighbor.getNeighbor().getId()) == pixel.getId()) {
                            segment.addSegmentPixel(neighbor.getNeighbor());
                            pixelsInSegment.add(neighbor.getNeighbor());
                            pixelsLeft.remove(neighbor.getNeighbor());
                        }
                    }
                }
            } // Segment finished

            for (int i = 0; i < segment.getSegmentPixels().size(); i++) {
                for (int j = 0; j < segment.getSegmentPixels().size(); j++) {
                    if (i != j && segment.getSegmentPixels().get(i) == segment.getSegmentPixels().get(j)) {
                        throw new Error("Duplicates in Segment");
                    }
                }
            }
        }

        System.out.println(segments.size() + " segments calcuated in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
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

    void setCrowdingDistance(double crowdingDistance) {
        this.crowdingDistance = crowdingDistance;
    }

    double getCrowdingDistance() {
        return crowdingDistance;
    }

    List<Integer> getChromosome() {
        return chromosome;
    }
}
