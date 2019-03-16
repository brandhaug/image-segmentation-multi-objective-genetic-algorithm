package GeneticAlgorithm;

import Utils.Utils;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Represents one chromosome
 */
class Individual {
    // Lists
    private List<Segment> segments; // List of segments (set of pixels)

    // Objective functions
    private double overallDeviation; // Objective function 1
    private double connectivity; // Objective function 2
    private double fitness;

    // Non-dominated sorting
    private int rank;

    // Dominated sorting
    private double crowdingDistance;

    private int generation;

    Individual(int generation) {
        this.generation = generation;
        segments = new ArrayList<>();
        generateInitialIndividual();
        calculateObjectiveFunctions();

        if (!GeneticAlgorithm.MULTI_OBJECTIVE) {
            calculateFitness();
        }
    }

    Individual(List<Segment> segments, int generation) {
        this.generation = generation;
        this.segments = new ArrayList<>(segments);
        calculateObjectiveFunctions();

        if (!GeneticAlgorithm.MULTI_OBJECTIVE) {
            calculateFitness();
        }
    }

    /**
     * Based on Minimum Spanning Tree (MST)
     */
    private void generateInitialIndividual() {
        Map<Integer, Segment> visitedPixels = new HashMap<>();
        PriorityQueue<Edge> possibleNeighbors = new PriorityQueue<>(); // Support array for all possible visits. Sorted by colorDistance

        int numberOfSegments = Utils.randomInt(GeneticAlgorithm.MIN_SEGMENTS, GeneticAlgorithm.MAX_SEGMENTS);

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
            possibleNeighbors.addAll(rootPixel.getEdges());
        }

        // Add all neighbors
        while (!possibleNeighbors.isEmpty()) {
            Edge bestEdge = possibleNeighbors.remove();
            Pixel bestNeighbor = bestEdge.getNeighbor();
            Pixel bestPixel = bestEdge.getPixel();
            Segment segment = visitedPixels.get(bestPixel.getId());

            if (!visitedPixels.containsKey(bestNeighbor.getId())) {
                possibleNeighbors.addAll(bestNeighbor.getEdges());
                visitedPixels.put(bestNeighbor.getId(), segment);
                segment.addSegmentPixel(bestNeighbor);
            }
        }
    }

    /**
     * Calculate convex hull pixels for each segment
     */
    void calculateConvexHulls() {
        for (Segment segment : segments) {
            segment.calculateConvexHull();
        }
    }

    /**
     * Calculates overall deviation and connectivity
     */
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

    /**
     * Calculates fitness for simple GA
     */
    private void calculateFitness() {
        fitness = overallDeviation + connectivity;
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

    int getGeneration() {
        return generation;
    }

    @Override
    public String toString() {
        return "Individual{" +
                "overallDeviation=" + overallDeviation +
                ", connectivity=" + connectivity +
                ", rank=" + rank +
                ", crowdingDistance=" + crowdingDistance +
                ", generation=" + generation +
                ", segments=" + segments.size() +
                '}';
    }
}
