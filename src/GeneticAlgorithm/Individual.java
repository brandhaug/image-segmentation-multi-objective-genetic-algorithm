package GeneticAlgorithm;

import Utils.Utils;

import java.util.*;

/**
 * Represents one chromosome
 */
class Individual {
    // Lists
    private List<Integer> chromosome; // List of genes (pixels)
    private List<Segment> segments; // List of segments (set of pixels)

    // Objective functions
    private double overallDeviation; // Objective function 1
    private double connectivity; // Objective function 2
    private double fitness;

    // Non-dominated sorting
    private int rank;

    // Dominated sorting
    private double crowdingDistance;

    Individual() {
        this.chromosome = new ArrayList<>(GeneticAlgorithm.initialChromosome);
        segments = new ArrayList<>();
        generateInitialIndividual();
        calculateObjectiveFunctions();
    }

    Individual(List<Integer> chromosome) {
        this.chromosome = new ArrayList<>(chromosome);
        segments = new ArrayList<>();
        calculateSegments();
        calculateObjectiveFunctions();
    }

    /**
     * Baed on Minimum Spanning Tree (MST)
     */
    private void generateInitialIndividual() {
        HashMap<Integer, Segment> visitedPixels = new HashMap<>();
        PriorityQueue<PixelNeighbor> possibleNeighbors = new PriorityQueue<>(); // Support array for all possible visits. Sorted by colorDistance

        int numberOfSegments = Utils.randomInt(GeneticAlgorithm.minSegments, GeneticAlgorithm.maxSegments);

        // Initialize segments, add neighbors of root pixels
        while (segments.size() != numberOfSegments) {
            Pixel rootPixel;

            do {
                int randomIndex = Utils.randomIndex(GeneticAlgorithm.pixels.size());
                rootPixel = GeneticAlgorithm.pixels.get(randomIndex); // Random first best pixel
            } while (visitedPixels.containsKey(rootPixel.getId()));

            Segment newSegment = new Segment();
            newSegment.addSegmentPixel(rootPixel);
            segments.add(newSegment);

            visitedPixels.put(rootPixel.getId(), newSegment);
            possibleNeighbors.addAll(rootPixel.getPixelNeighbors());
        }

        // Add all neighbors
        while (!possibleNeighbors.isEmpty()) {
            PixelNeighbor bestPixelNeighbor = possibleNeighbors.remove();
            Pixel bestNeighbor = bestPixelNeighbor.getNeighbor();
            Pixel bestPixel = bestPixelNeighbor.getPixel();
            Segment segment = visitedPixels.get(bestPixel.getId());

            if (!visitedPixels.containsKey(bestNeighbor.getId())) {
                possibleNeighbors.addAll(bestNeighbor.getPixelNeighbors());
                visitedPixels.put(bestNeighbor.getId(), segment);
                chromosome.set(bestNeighbor.getId(), bestPixel.getId());
                segment.addSegmentPixel(bestNeighbor);
            }
        }
    }

    /**
     * Creates segments based on chromosome
     */
    private void calculateSegments() {
        HashMap<Pixel, Segment> visitedPixels = new HashMap<>();
        Queue<PixelNeighbor> possiblePixelNeighbors = new LinkedList<>();

        for (int pixelId = 0; pixelId < chromosome.size(); pixelId++) {
            Pixel pixel = GeneticAlgorithm.pixels.get(pixelId);

            if (!visitedPixels.containsKey(pixel)) {
                Segment newSegment = new Segment();
                newSegment.addSegmentPixel(pixel);
                segments.add(newSegment);

                visitedPixels.put(pixel, newSegment);

                List<PixelNeighbor> pixelNeighbors = GeneticAlgorithm.pixels.get(pixelId).getPixelNeighbors();
                possiblePixelNeighbors.addAll(pixelNeighbors);

                while (!possiblePixelNeighbors.isEmpty()) {
                    PixelNeighbor pixelNeighbor = possiblePixelNeighbors.poll();
                    Pixel selectedNeighbor = pixelNeighbor.getNeighbor();
                    Pixel selectedPixel = pixelNeighbor.getPixel();

                    if (!visitedPixels.containsKey(selectedNeighbor) && (chromosome.get(selectedNeighbor.getId()) == selectedPixel.getId() || chromosome.get(selectedPixel.getId()) == selectedNeighbor.getId())) {
                        Segment segment = visitedPixels.get(selectedPixel);
                        segment.addSegmentPixel(selectedNeighbor);
                        possiblePixelNeighbors.addAll(selectedNeighbor.getPixelNeighbors());
                        visitedPixels.put(selectedNeighbor, segment);
                    }
                }
            }
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

    @Override
    public String toString() {
        return "Individual{" +
                "overallDeviation=" + overallDeviation +
                ", connectivity=" + connectivity +
                ", rank=" + rank +
                ", crowdingDistance=" + crowdingDistance +
                '}';
    }
}
