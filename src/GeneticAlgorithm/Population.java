package GeneticAlgorithm;

import Utils.Utils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Represents all individuals
 */
class Population {
    private List<Individual> individuals;
    private List<Individual> paretoFront;

    Population() throws InterruptedException {
        individuals = new ArrayList<>();
        generateInitialPopulation();
    }

    private void generateInitialPopulation() throws InterruptedException {
        System.out.println("Generating Initial Population");
        final long startTime = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < GeneticAlgorithm.POPULATION_SIZE; i++) {
            executorService.execute(() -> {
                Individual individual = new Individual(0);
                individuals.add(individual);
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        fastNonDominatedSort();
        calculateCrowdingDistances();

        System.out.println("Number of pareto optimal solutions: " + paretoFront.size());
        System.out.println(individuals.size() + " individuals created in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
    }

    /**
     * NSGA-II
     */
    void tick(int generation) throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        List<Individual> offspringIndividuals = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < GeneticAlgorithm.POPULATION_SIZE; i++) {
            executorService.execute(() -> {
                // Selection
                Individual[] parents = selection();

                // Crossover
                List<Segment> newSegments = crossover(parents[0], parents[1]);

                // Mutation
                double random = Utils.randomDouble();
                if (random < GeneticAlgorithm.MUTATION_RATE) {
//                    swapMutate(newSegments);
                }

                Individual offspring = new Individual(newSegments, generation);
                offspringIndividuals.add(offspring);
            });
        }

        // Wait for offspring to finish construction
        final long startTime2 = System.currentTimeMillis();
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        System.out.println("Segments in " + offspringIndividuals.size() + " offspring individuals calculated in " + ((System.currentTimeMillis() - startTime2) / 1000) + "s");

        int averageSegmentsSize = 0;
        for (Individual offspringIndividual : offspringIndividuals) {
            averageSegmentsSize += offspringIndividual.getSegments().size();
        }

        averageSegmentsSize = averageSegmentsSize / offspringIndividuals.size();
        System.out.println(offspringIndividuals.size() + " feasible offspring");
        System.out.println("Average segment size in offspring: " + averageSegmentsSize);

        // Add offspring to population
        individuals.addAll(offspringIndividuals);

        fastNonDominatedSort();
        calculateCrowdingDistances();

        individuals.sort(Comparator.comparingDouble(Individual::getRank).thenComparing(Individual::getCrowdingDistance, Collections.reverseOrder()));
        individuals = new ArrayList<>(individuals.subList(0, GeneticAlgorithm.POPULATION_SIZE));

        System.out.println("Number of pareto optimal solutions: " + paretoFront.size());
        System.out.println("New generation generated in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
    }

    /**
     * Ranking each individual based on how many other individuals dominates it
     * Based on page 3 in NSGA-II paper by Kalyanmoy Deb, Amrit Pratap, Sameer Agarwal, and T. Meyarivan
     */
    private void fastNonDominatedSort() {
        List<Individual> front = new ArrayList<>(); // F
        Map<Individual, Integer> dominatedCounts = new HashMap<>();
        Map<Individual, List<Individual>> dominatedIndividuals = new HashMap<>();

        int rank = 1;

        for (Individual individual : individuals) { // p in P
            dominatedIndividuals.put(individual, new ArrayList<>());
            dominatedCounts.put(individual, 0);

            for (Individual individualToCompare : individuals) { // q in P
                if (individual != individualToCompare) {
                    if (individual.dominates(individualToCompare)) { // Add to the set of solutions dominated (S)
                        List<Individual> dominates = dominatedIndividuals.get(individual);
                        dominates.add(individualToCompare);
                    } else if (individualToCompare.dominates(individual)) {
                        int dominatedCount = dominatedCounts.get(individual) + 1;
                        dominatedCounts.put(individual, dominatedCount);
                    }
                }
            }

            if (dominatedCounts.get(individual) == 0) {
                individual.setRank(rank);
                front.add(individual);
            }
        }

        paretoFront = new ArrayList<>(front);

        rank++;
        while (front.size() != 0) {
            List<Individual> newFront = new ArrayList<>(); // Q
            for (Individual individual : front) { // p in F
                for (Individual dominatedIndividual : dominatedIndividuals.get(individual)) { // q in S
                    int dominatedCount = dominatedCounts.get(dominatedIndividual) - 1;
                    dominatedCounts.put(dominatedIndividual, dominatedCount);

                    if (dominatedCount == 0) {
                        dominatedIndividual.setRank(rank);
                        newFront.add(dominatedIndividual);
                    }
                }
            }

            front = newFront;
            rank++;
        }
    }

    private void calculateCrowdingDistances() {
        // Reset distances
        for (Individual individual : individuals) {
            individual.setCrowdingDistance(0);
        }

        // Objective function 1: Overall deviation
        individuals.sort(Comparator.comparingDouble(Individual::getOverallDeviation));
        double minOverallDeviation = individuals.get(0).getOverallDeviation();
        double maxOverallDeviation = individuals.get(individuals.size() - 1).getOverallDeviation();
        individuals.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
        for (int k = 1; k < paretoFront.size() - 1; k++) {
            individuals.get(k).setCrowdingDistance(individuals.get(k).getCrowdingDistance() + (individuals.get(k + 1).getOverallDeviation() - individuals.get(k - 1).getOverallDeviation()) / (maxOverallDeviation - minOverallDeviation));
        }

        // Objective function 2: Connectivity
        individuals.sort(Comparator.comparingDouble(Individual::getConnectivity));
        double minConnectivity = individuals.get(0).getConnectivity();
        double maxConnectivity = individuals.get(individuals.size() - 1).getConnectivity();
        individuals.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
        for (int k = 1; k < paretoFront.size() - 1; k++) {
            individuals.get(k).setCrowdingDistance(individuals.get(k).getCrowdingDistance() + (individuals.get(k + 1).getConnectivity() - individuals.get(k - 1).getConnectivity()) / (maxConnectivity - minConnectivity));
        }
    }

    /**
     * Select two parents for reproduction. Parent 1 cannot be the same as parent 2
     *
     * @return array of the two parents
     */
    private Individual[] selection() {
        Individual parent1 = tournament();
        Individual parent2;

        do {
            parent2 = tournament();
        } while (parent1 == parent2);

        return new Individual[]{parent1, parent2};
    }

    private Individual tournament() {
        List<Individual> contestants = new ArrayList<>();
        List<Individual> bestRankedContestants = new ArrayList<>();
        int minRank = Integer.MAX_VALUE;

        // Choose contestants
        for (int i = 0; i < GeneticAlgorithm.TOURNAMENT_SIZE; i++) {
            Individual contestant;
            do {
                contestant = individuals.get(Utils.randomIndex(individuals.size()));
            } while (contestants.contains(contestant));

            contestants.add(contestant);

            // Find best contestants
            if (contestant.getRank() < minRank) {
                bestRankedContestants.clear();
                bestRankedContestants.add(contestant);
                minRank = contestant.getRank();
            } else if (contestant.getRank() == minRank) {
                bestRankedContestants.add(contestant);
            }
        }

        if (bestRankedContestants.size() == 0) {
            throw new Error("No competitors");
        }

        bestRankedContestants.sort(Comparator.comparingDouble(Individual::getCrowdingDistance).reversed());
        return bestRankedContestants.get(0);
    }

    /**
     * Create a single offspring from two parents by combining their segments
     */
    private List<Segment> crossover(Individual parent, Individual otherParent) {
        List<Segment> newSegments = new ArrayList<>();

        // Initialize lists and map
        Map<Integer, Segment> pixelSegmentMap = new HashMap<>();
        boolean[] isAdded = new boolean[GeneticAlgorithm.pixels.size()];
        List<Segment> parentSegments = new ArrayList<>();
        parentSegments.addAll(parent.getSegments());
        parentSegments.addAll(otherParent.getSegments());

        // Shuffle list of all segments
        Collections.shuffle(parentSegments);

        // Loop through every segment, and add segment if it does not contain any pixels that is already assigned to offspring
        for (Segment segment : parentSegments) {
            boolean addSegment = true;
            for (Pixel pixel : segment.getSegmentPixels().values()) {
                if (pixelSegmentMap.containsKey(pixel.getId()))
                    addSegment = false;
            }

            if (addSegment) {
                Segment newSegment = new Segment();
                for (Pixel pixel : segment.getSegmentPixels().values()) {
                    newSegment.addSegmentPixel(pixel);
                    pixelSegmentMap.put(pixel.getId(), newSegment);
                    isAdded[pixel.getId()] = true;
                }

                newSegments.add(newSegment);
            }
        }

        // Find what pixels remain to be added
        List<Pixel> remainingPixels = findRemainingPixels(isAdded);

        // Decide how many segments offspring should have
        int numberOfSegments = Utils.randomInt(GeneticAlgorithm.MIN_SEGMENTS, GeneticAlgorithm.MAX_SEGMENTS);

        int remainingSegmentsToCreate = numberOfSegments - newSegments.size();

        if (remainingSegmentsToCreate <= 0) {
            remainingSegmentsToCreate = 1;
        }

        if (remainingPixels.size() > 0) {
            if (remainingSegmentsToCreate > remainingPixels.size()) {
                // Create segments with MST
                newSegments.addAll(multipleMST(remainingPixels.size(), remainingPixels, pixelSegmentMap));

                while (newSegments.size() < numberOfSegments) {
                    List<Segment> segments = splitSegment(newSegments, pixelSegmentMap);
                    newSegments.add(segments.get(0));
                    newSegments.add(segments.get(1));
                }
            } else {
                // Create segments with MST
                newSegments.addAll(multipleMST(remainingSegmentsToCreate, remainingPixels, pixelSegmentMap));

                // Fill in all remaining pixels
                while (pixelSegmentMap.size() != GeneticAlgorithm.pixels.size()) {
                    newSegments.add(multipleMST(1, remainingPixels, pixelSegmentMap).get(0));
                }
            }
        }

        // Combine segments if there are too many
        while (newSegments.size() > numberOfSegments) {
            combineSegments(newSegments, pixelSegmentMap);
        }

        return newSegments;
    }

    /**
     * Return a list of the pixels that remains to be added to an offspring in the crossover method. This is done by
     * adding the pixels which is at index i in isAdded if that element is false
     *
     * @param isAdded a list of length GeneticAlgorithm.pixels, if true pixel is already added to offspring
     * @return list of pixels that remain to be added
     */
    private List<Pixel> findRemainingPixels(boolean[] isAdded) {
        List<Pixel> remainingPixels = new ArrayList<>();

        for (int i = 0; i < isAdded.length; i++) {
            if (!isAdded[i]) {
                remainingPixels.add(GeneticAlgorithm.pixels.get(i));
            }
        }
        return remainingPixels;
    }

    /**
     * Collapse two neighboring segments by finding the two neighboring segments with lowest color distance
     *
     * @param segments        list of segments to check
     * @param pixelSegmentMap mapping of what segment a pixel belongs to
     */
    private void combineSegments(List<Segment> segments, Map<Integer, Segment> pixelSegmentMap) {
        // Update centroid for every segment
        for (Segment segment : segments) {
            segment.calculateAverageColor();
        }

        Segment segment1 = null, segment2 = null;
        int random = Utils.randomInt(0, 1);

        double minDistance = Integer.MAX_VALUE, minSize = Integer.MAX_VALUE;

        for (Segment segment : segments) {
            List<Segment> neighboringSegments = getNeighborSegments(segment, pixelSegmentMap);

            for (Segment segmentNeighbor : neighboringSegments) {
                if (random == 1) {
                    double colorDistance = Utils.getEuclideanColorDistance(segment.getAverageColor(), segmentNeighbor.getAverageColor());

                    if (colorDistance < minDistance) {
                        segment1 = segment;
                        segment2 = segmentNeighbor;
                        minDistance = colorDistance;
                    }
                } else {
                    int segmentSize = segmentNeighbor.getSegmentPixels().size();
                    if (segmentSize < minSize) {
                        segment1 = segment;
                        segment2 = segmentNeighbor;
                        minSize = segmentSize;
                    }
                }
            }
        }

        Objects.requireNonNull(segment1).addSegmentPixels(segment2.getSegmentPixels().values());
        segments.remove(segment2);

        for (Pixel pixel : segment2.getSegmentPixels().values()) {
            pixelSegmentMap.put(pixel.getId(), segment1);
        }
    }

    /**
     * Split a segment by finding the segment with largest color difference
     *
     * @param segments        segments to check
     * @param pixelSegmentMap map of what segment a pixel belongs to
     * @return Two new segments
     */
    private List<Segment> splitSegment(List<Segment> segments, Map<Integer, Segment> pixelSegmentMap) {
        Segment segmentToSplit = findSegmentToSplit(segments);

        pixelSegmentMap.values().removeAll(Collections.singleton(segmentToSplit));
        segments.remove(segmentToSplit);

        List<Pixel> pixels = new ArrayList<>(segmentToSplit.getSegmentPixels().values());
        return new ArrayList<>(multipleMST(2, pixels, pixelSegmentMap));
    }

    /**
     * Finds segment with highest overall deviations
     */
    private Segment findSegmentToSplit(List<Segment> segments) {
        double maxOverallDeviation = Double.MIN_VALUE;

        Segment segmentToSplit = null;

        for (Segment segment : segments) {
            segment.calculateObjectiveFunctions();
            double overallDeviation = segment.getOverallDeviation();

            if (overallDeviation > maxOverallDeviation) {
                segmentToSplit = segment;
                maxOverallDeviation = overallDeviation;
            }
        }

        return segmentToSplit;
    }

    /**
     * Find all neighboring segments of a specific segment
     *
     * @param segment             segment to locate its neighbors
     * @param pixelSegmentHashMap mapping of what segment a pixel belongs to
     * @return all neighboring segments of Segment segment
     */
    private List<Segment> getNeighborSegments(Segment segment, Map<Integer, Segment> pixelSegmentHashMap) {
        ArrayList<Segment> neighborSegments = new ArrayList<>();

        List<Pixel> pixels = new ArrayList<>(segment.getSegmentPixels().values());
        for (Pixel pixel : pixels) {
            List<Edge> edges = pixel.getEdges();
            for (Edge edge : edges) {
                Segment neighborSegment = pixelSegmentHashMap.get(edge.getNeighbor().getId());
                if (neighborSegment != null && segment != neighborSegment && !neighborSegments.contains(neighborSegment)) {
                    neighborSegments.add(neighborSegment);
                }
            }
        }

        return neighborSegments;
    }

    /**
     * Build multiple MSTs (Minimum spanning tree) from the pixels in list pixels.
     *
     * @param numberOfTrees   Number of trees to be made
     * @param pixels          The list of pixels to build MSTs around
     * @param pixelSegmentMap Map of what segment a pixel belongs to already
     * @return multiple new segments aka MSTs with regard to color distance
     */
    private List<Segment> multipleMST(int numberOfTrees, List<Pixel> pixels, Map<Integer, Segment> pixelSegmentMap) {
        Queue<Edge> availableNeighbors = new PriorityQueue<>();
        List<Segment> newSegments = new ArrayList<>();
        for (int i = 0; i < numberOfTrees; i++) {
            Pixel pixel;
            do {
                pixel = pixels.get(Utils.randomIndex(pixels.size()));
            } while (pixelSegmentMap.containsKey(pixel.getId()));

            Segment segment = new Segment();
            segment.addSegmentPixel(pixel);
            pixelSegmentMap.put(pixel.getId(), segment);
            newSegments.add(segment);
            availableNeighbors.addAll(pixel.getEdges());
        }

        while (!availableNeighbors.isEmpty()) {
            Edge bestCandidate = availableNeighbors.remove();
            Pixel p = bestCandidate.getPixel();
            Pixel n = bestCandidate.getNeighbor();
            Segment s = pixelSegmentMap.get(p.getId());

            if (!pixelSegmentMap.containsKey(n.getId())) {
                availableNeighbors.addAll(n.getEdges());
                pixelSegmentMap.put(n.getId(), s);
                s.addSegmentPixel(n);
            }
        }

        return newSegments;
    }

    private void swapMutate(List<Integer> chromosome) {
        int indexA = Utils.randomIndex(chromosome.size());
        int randomNeighborIndex = Utils.randomIndex(GeneticAlgorithm.pixels.get(indexA).getEdges().size());
        int indexB = GeneticAlgorithm.pixels.get(indexA).getEdges().get(randomNeighborIndex).getNeighbor().getId();
        Collections.swap(chromosome, indexA, indexB);
    }

    List<Segment> getRandomParetoSegments() {
        int randomIndex;
        Individual individual;
        do {
            randomIndex = Utils.randomIndex(individuals.size());
            individual = individuals.get(randomIndex);
        } while (individual.getRank() != 1);

        individual.calculateConvexHulls();

        return individuals.get(randomIndex).getSegments();
    }

    List<Individual> getIndividuals() {
        return individuals;
    }
}
