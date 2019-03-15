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
        for (int i = 0; i < GeneticAlgorithm.populationSize; i++) {
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

        for (int i = 0; i < GeneticAlgorithm.populationSize; i++) {
            executorService.execute(() -> {
                // Selection
                Individual[] parents = selection();

                // Crossover
                Individual offspring = crossover2(parents[0], parents[1]);

                // Mutation
                double random = Utils.randomDouble();
                if (random < GeneticAlgorithm.mutationRate) {
                    swapMutate(offspring.getChromosome());
                }
                offspring.calculateObjectiveFunctions();
                offspringIndividuals.add(offspring);
            });
        }

        // Wait for offspring to finish construction
        final long startTime2 = System.currentTimeMillis();
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        System.out.println("Segments in " + offspringIndividuals.size() + " offspring individuals calculated in " + ((System.currentTimeMillis() - startTime2) / 1000) + "s");

        // Filter out infeasible offspring
        offspringIndividuals.removeIf(offspringIndividual -> !offspringIndividual.isFeasible());

        int averageSegmentsSize = 0;
        for (Individual offspringIndividual : offspringIndividuals) {
            averageSegmentsSize += offspringIndividual.getSegments().size();
        }

        if (offspringIndividuals.size() != 0) {
            averageSegmentsSize = averageSegmentsSize / offspringIndividuals.size();
            System.out.println(offspringIndividuals.size() + " feasible offspring");
            System.out.println("Average segment size in offspring: " + averageSegmentsSize);

            // Add offspring to population
            individuals.addAll(offspringIndividuals);

            fastNonDominatedSort();
            calculateCrowdingDistances();

            individuals.sort(Comparator.comparingDouble(Individual::getRank).thenComparing(Individual::getCrowdingDistance, Collections.reverseOrder()));
            individuals = new ArrayList<>(individuals.subList(0, GeneticAlgorithm.populationSize));
        } else {
            System.out.println("No feasible offspring");
        }

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
        for (Individual individual : paretoFront) {
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
     * @return array of the two parents
     */
    public Individual[] selection() {
        Individual parent1 = tournament();
        Individual parent2 = tournament();

        while (parent1 == parent2)
            parent2 = tournament();

        return new Individual[]{parent1, parent2};
    }

    public Individual tournament() {
        List<Individual> contestants = new ArrayList<>();

        // Choose contestants
        for (int i = 0; i < GeneticAlgorithm.tournamentSize; i++) {
            Individual contestant;
            do {
                contestant = individuals.get(Utils.randomIndex(individuals.size()));
            } while (contestants.contains(contestant));
            contestants.add(contestant);
        }

        // Use the first individual as reference by adding it to the tournament list
        Individual contestant = contestants.get(0);
        int currentBestRank = contestant.getRank();
        List<Individual> tournament = new ArrayList<>();
        tournament.add(contestant);

        // Loop through list of contestants, and remove the individuals with the highest rank
        for (int i = 1; i < contestants.size(); i++) {
            contestant = contestants.get(i);

            // If current contestant has better (lower) rank than reference, remove all contestants with lesser rank, and make it the new reference
            if (contestant.getRank() < currentBestRank) {
                tournament.clear();
                tournament.add(contestant);
                currentBestRank = contestant.getRank();
            }
            // If current contestant has equal rank as reference, add it to the next "round" of the tournament
            else if (contestant.getRank() == currentBestRank) {
                tournament.add(contestant);
            }
        }

        // If there is one individual with better rank than all other contestants, choose that individual
        if (tournament.size() == 1) {
            return tournament.get(0);
        }
        // Else choose individual with highest crowding distance
        else {
            // Sort descending by crowding distance
            Collections.sort(tournament, (a,b) -> Double.compare(b.getCrowdingDistance(), a.getCrowdingDistance()));
            return tournament.get(0);
        }
    }



    private List<List<Integer>> crossOver(Individual parent, Individual otherParent, int splits) {
        List<List<Integer>> newChromosomes = new ArrayList<>();
        int[] partitionIndices = Utils.generatePartitionIndices(parent.getChromosome().size(), splits);
        List<List<Integer>> partsFromParent = Utils.splitRoute(parent.getChromosome(), partitionIndices, splits);
        List<List<Integer>> partsFromOtherParent = Utils.splitRoute(otherParent.getChromosome(), partitionIndices, splits);

        List<Integer> newChromosome = new ArrayList<>();
        List<Integer> newChromosome2 = new ArrayList<>();

        for (int i = 0; i < splits; i++) {
            if (i % 2 == 0) {
                newChromosome.addAll(partsFromParent.get(i));
                newChromosome2.addAll(partsFromOtherParent.get(i));
            } else {
                newChromosome.addAll(partsFromOtherParent.get(i));
                newChromosome2.addAll(partsFromParent.get(i));
            }
        }

        if (newChromosome.size() != parent.getChromosome().size() || newChromosome.size() != newChromosome2.size()) {
            throw new Error("Chromosomes are different size");
        }

        newChromosomes.add(newChromosome);
        newChromosomes.add(newChromosome2);

        for (List<Integer> chromosome : newChromosomes) {
            repairChromosome(chromosome);
        }

        return newChromosomes;
    }

    /**
     * Create a single offspring from two parents by combining their segments
     * @return
     */
    private Individual crossover2(Individual parent1, Individual parent2) {
        Individual offspring = new Individual(); // TODO: Problem here with creating an empty offspring?

        // Initialize lists and map
        Map<Integer, Segment> pixelSegmentMap = new HashMap<>();
        boolean[] isAdded = new boolean[GeneticAlgorithm.pixels.size()];
        List<Segment> allSegments = new ArrayList<>(parent1.getSegments());
        allSegments.addAll(parent2.getSegments());

        // Shuffle list of all segments
        Collections.shuffle(allSegments);

        // Loop through every segment, and add segment if it does not contain any pixels that is already assigned to offspring
        for (Segment s : allSegments) {
            boolean canAddSegment = true;
            for (Pixel p : s.getSegmentPixels().values()) {
                if (pixelSegmentMap.containsKey(p.getId()))
                    canAddSegment = false;
            }

            if (canAddSegment) {
                Segment segmentClone = new Segment();
                for (Pixel p : s.getSegmentPixels().values()) {
                    segmentClone.addSegmentPixel(p);
                    pixelSegmentMap.put(p.getId(), segmentClone);
                    isAdded[p.getId()] = true;
                }

                offspring.addSegment(segmentClone);
            }
        }

        // Find what pixels remain to be added
        List<Pixel> remainingPixels = findRemainingPixels(isAdded);

        // Decide how many segments offspring should have
        int numberOfSegments = Utils.randomInt(GeneticAlgorithm.minSegments, GeneticAlgorithm.maxSegments);
        int remaingSegmentsToCreate = numberOfSegments - offspring.getSegments().size() > 0 ?
                numberOfSegments - offspring.getSegments().size() : 1;

        if (remainingPixels.size() > 0) {
            if (remaingSegmentsToCreate > remainingPixels.size()) {
                // Create segments with MST
                for (Segment s : multipleMST(remainingPixels.size(), remainingPixels, pixelSegmentMap)) {
                    offspring.addSegment(s);
                }
                while (offspring.getSegments().size() < numberOfSegments) {
                    split(offspring.getSegments(), pixelSegmentMap);
                }
            } else {
                // Create segments with MST
                for (Segment s : multipleMST(remaingSegmentsToCreate, remainingPixels, pixelSegmentMap)) {
                    offspring.addSegment(s);
                }
            }
        }

        // Combine segments if there are too many
        while (offspring.getSegments().size() > numberOfSegments) {
            combine(offspring.getSegments(), pixelSegmentMap);
        }

        return offspring;
    }

    /**
     * Return a list of the pixels that remains to be added to an offspring in the crossover method. This is done by
     * adding the pixels which is at index i in isAdded if that element is false
     * @param isAdded a list of length GeneticAlgorithm.pixels, if true pixel is already added to offspring
     * @return list of pixels that remain to be added
     */
    private List<Pixel> findRemainingPixels(boolean[] isAdded) {
        List<Pixel> remainingPixels = new ArrayList<>();

        for (int i = 0; i < isAdded.length; i++) {
            if (!isAdded[i])
                remainingPixels.add(GeneticAlgorithm.pixels.get(i));
        }
        return remainingPixels;
    }

    /**
     * Collapse two neighboring segments by finding the two neighboring segments with lowest color distance
     * @param segments list of segments to check
     * @param pixelSegmentMap mapping of what segment a pixel belongs to
     */
    private void combine(List<Segment> segments, Map<Integer, Segment> pixelSegmentMap) {
        // Update centroid for every segment
        for (Segment s : segments) {
            s.calculateCentroidCoordinate();
        }

        Segment segment1 = null, segment2 = null;
        int random = Utils.randomInt(0, 1);

        double minDistance = Integer.MAX_VALUE, minSize = Integer.MAX_VALUE;

        for (Segment s : segments) {
            List<Segment> neighboringSegments = getNeighborSegments(s, pixelSegmentMap);

            for (Segment neighboringSegment : neighboringSegments) {
                if (random < 0.5) {
                    double colorDistance = Utils.getEuclideanColorDistance(s.getCentroidPixelColor(), neighboringSegment.getCentroidPixelColor());

                    if (colorDistance < minDistance) {
                        segment1 = s;
                        segment2 = neighboringSegment;
                        minDistance = colorDistance;
                    }
                }
                else {
                    int segmentSize = neighboringSegment.getSegmentPixels().size();
                    if (segmentSize < minSize) {
                        segment1 = s;
                        segment2 = neighboringSegment;
                        minSize = segmentSize;
                    }
                }
            }
        }

        Objects.requireNonNull(segment1).addSegmentPixels(segment2.getSegmentPixels().values());
        segments.remove(segment2);

        for (Pixel p : segment2.getSegmentPixels().values()) {
            pixelSegmentMap.put(p.getId(), segment1);
        }
    }

    /**
     * Split a segment by finding the segment with largest color difference
     * @param segments segments to check
     * @param pixelSegmentMap map of what segment a pixel belongs to
     * @return Two new segments
     */
    private List<Segment> split(List<Segment> segments, Map<Integer, Segment> pixelSegmentMap) {
        int minColor = Integer.MAX_VALUE, maxColor = Integer.MIN_VALUE;

        Segment segmentToSplit = null;

        // Find what segment to split
        for (Segment s : segments) {
            int tempMinColor = Integer.MAX_VALUE, tempMaxColor = Integer.MIN_VALUE;

            for (Pixel p : s.getSegmentPixels().values()) {
                if (p.getColorSum() > tempMaxColor)
                    tempMaxColor = p.getColorSum();
                else if (p.getColorSum() < tempMinColor)
                    tempMinColor = p.getColorSum();
            }
            if (tempMinColor < minColor && tempMaxColor > maxColor) {
                segmentToSplit = s;
                minColor = tempMinColor;
                maxColor = tempMaxColor;
            }
        }

        pixelSegmentMap.values().removeAll(Collections.singleton(segmentToSplit));
        List<Pixel> pixels = new ArrayList<>(segmentToSplit.getSegmentPixels().values());
        return multipleMST(2, pixels, pixelSegmentMap);
    }

    /**
     * Find all neighboring segments of a specific segment
     * @param segment segment to locate its neighbors
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
     * @param numberOfTrees Number of trees to be made
     * @param pixels The list of pixels to build MSTs around
     * @param pixelSegmentMap Map of what segment a pixel belongs to already
     * @return multiple new segments aka MSTs with regard to color distance
     */
    private List<Segment> multipleMST(int numberOfTrees, List<Pixel> pixels, Map<Integer, Segment> pixelSegmentMap) {
        Queue<Edge> availableNeighbors = new PriorityQueue<>();
        List<Segment> newSegments = new ArrayList<>();
        for (int i = 0; i < numberOfTrees; i++) {
            Pixel p;
            do {
                p = pixels.get(Utils.randomIndex(pixels.size()));
            }
            while (pixelSegmentMap.containsKey(p.getId()));

            Segment segment = new Segment();
            segment.addSegmentPixel(p);
            pixelSegmentMap.put(p.getId(), segment);
            newSegments.add(segment);
            availableNeighbors.addAll(p.getEdges());
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

    private void repairChromosome(List<Integer> chromosome) {
        for (int pixelId = 0; pixelId < chromosome.size(); pixelId++) {
            Queue<Edge> priorityQueue = new PriorityQueue<>();
            boolean repaired = true;
            int neighborId = chromosome.get(pixelId);

            if (pixelId != neighborId && chromosome.get(neighborId) == pixelId) {
                repaired = false;
            }

            while (!repaired) {
                // Pixel and neighbor pointing to eachother
                priorityQueue.addAll(GeneticAlgorithm.pixels.get(pixelId).getEdges());

                while (!priorityQueue.isEmpty()) {
                    Pixel potentialNeighbor = priorityQueue.poll().getNeighbor();

                    if (chromosome.get(potentialNeighbor.getId()) != pixelId) {
                        chromosome.set(pixelId, potentialNeighbor.getId());
                        repaired = true;
                        break;
                    }
                }

                // No neighbors possible, pointing to self
                if (!repaired) {
                    System.out.println("No neighbors possible");
                    chromosome.set(pixelId, pixelId);
                    repaired = true;
                }
            }
        }


//        int counter = 0;
//        for (int pixelId = 0; pixelId < chromosome.size(); pixelId++) {
//            int neighborId = chromosome.get(pixelId);
//
//            if (chromosome.get(neighborId) == pixelId) {
//                counter++;
//            }
//        }
//        System.out.println(counter);
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
