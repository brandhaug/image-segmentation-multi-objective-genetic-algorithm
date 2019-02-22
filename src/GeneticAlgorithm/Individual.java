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

    // Non-dominated sorting
    private int dominatedCount = 0; // n: number of solutions which dominates individual
    private List<Individual> dominatedIndividuals = new ArrayList<>(); // S: set of solutions which individual dominates
    private int rank;

    private double crowdingDistance;

    private boolean initialize;

    Individual() {
        this.chromosome = new ArrayList<>(GeneticAlgorithm.initialChromosome);
        this.initialize = true;
    }

    Individual(List<Integer> chromosome) {
        this.chromosome = new ArrayList<>(chromosome);
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
                if (neighbor.getColorDistance() < GeneticAlgorithm.initialColorDistanceThreshold && !addedIds[neighbor.getNeighbor().getId()]) {
                    possibleNeighbors.add(neighbor);
                    addedIds[neighbor.getNeighbor().getId()] = true;
                }
            }

            while (possibleNeighbors.size() != 0) {
                // Sort and get best neighbor
                possibleNeighbors.sort(Comparator.comparingDouble(PixelNeighbor::getColorDistance)); // Sort by colorDistance
                PixelNeighbor bestPixelNeighbor = possibleNeighbors.get(0);
                Pixel bestPixel = bestPixelNeighbor.getPixel();
                Pixel bestNeighbor = bestPixelNeighbor.getNeighbor(); // Best neighbor of best pixel

                // Update lists
                chromosome.set(bestNeighbor.getId(), bestPixel.getId()); // Update chromosome: ID == Index
                possibleNeighbors.remove(bestPixelNeighbor);
                pixelsLeft.remove(bestNeighbor);
                segment.addSegmentPixel(bestNeighbor);
                bestNeighbor.setSegment(segment);

                for (PixelNeighbor neighbor : bestNeighbor.getPixelNeighbors()) { // Make Neighbors of bestNeighbor available for selection
                    if (neighbor.getColorDistance() < GeneticAlgorithm.initialColorDistanceThreshold && !addedIds[neighbor.getNeighbor().getId()]) {
                        possibleNeighbors.add(neighbor);
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
        List<Pixel> pixelsLeft = new ArrayList<>(GeneticAlgorithm.pixels); // Support array for removing added pixels to make randomIndex effective
        boolean[] addedIds = new boolean[GeneticAlgorithm.pixels.size()]; // Support array for seeing which pixels is already added. It removes the need for using the ineffective list.contains()
        Arrays.fill(addedIds, false);

        while (pixelsLeft.size() != 0) {
            Segment segment = new Segment();

            int randomIndex = Utils.randomIndex(pixelsLeft.size());
            Pixel currentPixel = pixelsLeft.get(randomIndex); // Random first pixel

            // Update lists
            segment.addSegmentPixel(currentPixel);
            addedIds[currentPixel.getId()] = true;
            currentPixel.setSegment(segment);
            pixelsLeft.remove(currentPixel);

            for (PixelNeighbor pixelNeighbor : currentPixel.getPixelNeighbors()) { // Make Neighbors of randomPixel available for selection
                if (chromosome.get(pixelNeighbor.getNeighbor().getId()) == currentPixel.getId() && !addedIds[pixelNeighbor.getNeighbor().getId()]) {  // Neighbor pointing to current pixel
                    segment.addSegmentPixel(pixelNeighbor.getNeighbor());
                    addedIds[pixelNeighbor.getNeighbor().getId()] = true;
                    pixelNeighbor.getNeighbor().setSegment(segment);
                    pixelsLeft.remove(pixelNeighbor.getNeighbor());
                }
            }

            Pixel currentTargetPixel = GeneticAlgorithm.pixels.get(chromosome.get(currentPixel.getId()));

            if (!addedIds[currentTargetPixel.getId()]) {
                segment.addSegmentPixel(currentTargetPixel);
                addedIds[currentTargetPixel.getId()] = true;
                currentTargetPixel.setSegment(segment);
                pixelsLeft.remove(currentTargetPixel);
            }

            int currentPixelIndex = 1;
            while (currentPixelIndex < segment.getSegmentPixels().size()) {
                currentPixel = segment.getSegmentPixels().get(currentPixelIndex);

                for (PixelNeighbor pixelNeighbor : currentPixel.getPixelNeighbors()) { // Make Neighbors of randomPixel available for selection
                    if (chromosome.get(pixelNeighbor.getNeighbor().getId()) == currentPixel.getId() && !addedIds[pixelNeighbor.getNeighbor().getId()]) {  // Neighbor pointing to current pixel
                        segment.addSegmentPixel(pixelNeighbor.getNeighbor());
                        addedIds[pixelNeighbor.getNeighbor().getId()] = true;
                        pixelNeighbor.getNeighbor().setSegment(segment);
                        pixelsLeft.remove(pixelNeighbor.getNeighbor());
                    }
                }

                currentTargetPixel = GeneticAlgorithm.pixels.get(chromosome.get(currentPixel.getId()));

                if (!addedIds[currentTargetPixel.getId()]) {
                    segment.addSegmentPixel(currentTargetPixel);
                    addedIds[currentTargetPixel.getId()] = true;
                    currentTargetPixel.setSegment(segment);
                    pixelsLeft.remove(currentTargetPixel);
                }

                currentPixelIndex++;
            }

            segments.add(segment);
        }
    }

    private void calculateObjectiveFunctions() {
        overallDeviation = 0.0;
        connectivity = 0.0;

        for (Segment segment : segments) {
            segment.calculateObjectiveFunctions();
            overallDeviation += segment.getOverallDeviation();
            connectivity += segment.getConnectivity();
        }
    }

    /**
     * A(x1, y1) dominates B(x2, y2) when: (x1 <= x2 and y1 <= y2) and (x1 < x2 or y1 < y2)
     */
    boolean dominates(Individual otherIndividual) {
        return (overallDeviation <= otherIndividual.getOverallDeviation() && connectivity <= otherIndividual.getConnectivity()) &&
                (overallDeviation < otherIndividual.getOverallDeviation() || connectivity < otherIndividual.getConnectivity());
    }

    List<Segment> getSegments() {
        return segments;
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
