package GeneticAlgorithm;

import Utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents all individuals
 */
class Population {
    private List<Individual> individuals = new ArrayList<>();

    // Lists
    private List<Pixel> pixels;
    private List<Integer> initialChromosome;

    // Parameters
    private int populationSize; // Number of Solutions in population
    private double crossOverRate;
    private double mutationRate;
    private int tournamentSize;
    private int splits;
    private double initialColorDistanceThreshold;

    private List<Individual> paretoFront;


    Population(List<Pixel> pixels,
               List<Integer> initialChromosome,
               double initialColorDistanceThreshold,
               int populationSize,
               double crossOverRate,
               double mutationRate,
               int tournamentSize,
               int splits) throws InterruptedException {
        this.pixels = pixels;
        this.initialChromosome = initialChromosome;
        this.initialColorDistanceThreshold = initialColorDistanceThreshold;
        this.populationSize = populationSize;
        this.crossOverRate = crossOverRate;
        this.mutationRate = mutationRate;
        this.tournamentSize = tournamentSize;
        this.splits = splits;

        generateInitialPopulation(pixels, initialChromosome, initialColorDistanceThreshold);
    }

    private void generateInitialPopulation(List<Pixel> pixels, List<Integer> initialChromosome, double initialColorDistanceThreshold) throws InterruptedException {
        System.out.println("Generating Initial Population");
        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < populationSize; i++) {
            Individual individual = new Individual(pixels, initialChromosome, initialColorDistanceThreshold);
            individual.start(); // Start thread by calling run method
            individuals.add(individual);
        }

        for (Individual individual : individuals) {
            individual.join(); // Wait for thread to terminate
        }
        System.out.println(individuals.size() + " individuals created in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
    }

    /**
     * NSGA-II
     */
    void tick() throws InterruptedException {
        fastNonDominatedSort();
        calculateCrowdingDistances();

//        int numberOfParentsToKeep = (populationSize - (int) (populationSize * crossOverRate));
//        List<Individual> newIndividuals = new ArrayList<>(individuals.subList(0, numberOfParentsToKeep));

        List<Individual> newIndividuals = paretoFront;

        System.out.println("Number of pareto optimal solutions: " + paretoFront.size());

        while (newIndividuals.size() != populationSize) {
            // Selection
            Individual parent = tournament();
            Individual otherParent = tournament();

            // Crossover
            List<List<Integer>> newChromosomes = crossOver(parent, otherParent, splits);

            // Mutation
            for (List<Integer> newChromosome : newChromosomes) {
                if (newIndividuals.size() != populationSize) {
                    double random = Utils.randomDouble();
                    if (random < mutationRate) {
                        swapMutate(newChromosome);
                    }

                    // Add offspring
                    newIndividuals.add(new Individual(pixels, newChromosome));
                }
            }
        }

        individuals = newIndividuals;
//        individuals.sort(Comparator.comparingDouble(Individual::getRank));
    }

    /**
     * Ranking each individual based on how many other individuals dominates it
     * Based on page 3 in NSGA-II paper by Kalyanmoy Deb, Amrit Pratap, Sameer Agarwal, and T. Meyarivan
     */
    private void fastNonDominatedSort() {
        System.out.println("Non-dominated sorting");
        final long startTime = System.currentTimeMillis();
        List<Individual> front = new ArrayList<>(); // F

        int rank = 1;

        for (Individual individual : individuals) { // p in P
            for (Individual individualToCompare : individuals) { // q in P
                if (individual != individualToCompare) {
                    // A(x1, y1) dominates B(x2, y2) when: (x1 <= x2 and y1 <= y2) and (x1 < x2 or y1 < y2)
                    if ((individual.getOverallDeviation() <= individualToCompare.getOverallDeviation() &&
                            individual.getConnectivity() <= individualToCompare.getConnectivity()) &&
                            (individual.getOverallDeviation() < individualToCompare.getOverallDeviation() ||
                                    individual.getConnectivity() < individualToCompare.getConnectivity())) {
                        individual.addToDominatedIndividuals(individualToCompare); // Add to the set of solutions dominated (S)

                        // A(x1, y1) is dominated by B(x2, y2) when: (x1 >= x2 and y1 >= y2) and (x1 > x2 or y1 > y2)
                    } else if ((individual.getOverallDeviation() >= individualToCompare.getOverallDeviation() &&
                            individual.getConnectivity() >= individualToCompare.getConnectivity()) &&
                            (individual.getOverallDeviation() > individualToCompare.getOverallDeviation() ||
                                    individual.getConnectivity() > individualToCompare.getConnectivity())) {
                        individual.setDominatedCount(individual.getDominatedCount() + 1); // Increment the domination counter (n)
                    }
                }
            }

            if (individual.getDominatedCount() == 0) {
                individual.setRank(rank);
                front.add(individual);
            }
        }

        paretoFront = new ArrayList<>(front);

        rank++;
        while (front.size() != 0) {
            List<Individual> newFront = new ArrayList<>(); // Q
            for (Individual individual : front) { // p in F
                for (Individual dominatedIndividual : individual.getDominatedIndividuals()) { // q in S
                    dominatedIndividual.setDominatedCount(dominatedIndividual.getDominatedCount() - 1);

                    if (dominatedIndividual.getDominatedCount() == 0) {
                        dominatedIndividual.setRank(rank);
                        newFront.add(dominatedIndividual);
                    }
                }
            }

            front = newFront;
            rank++;
        }

        System.out.println("Non dominated sorting finished after " + ((System.currentTimeMillis() - startTime)) + "ms");
    }

    private void calculateCrowdingDistances() {
        System.out.println("Calculating crowding distances");
        final long startTime = System.currentTimeMillis();

        // Reset distances
        for (Individual individual : paretoFront) {
            individual.setCrowdingDistance(0);
        }

        // Objective function 1: Overall deviation
        individuals.sort(Comparator.comparingDouble(Individual::getOverallDeviation));
        double minOverallDeviation = individuals.get(individuals.size() - 1).getOverallDeviation();
        double maxOverallDeviation = individuals.get(0).getOverallDeviation();
        individuals.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
        for (int k = 1; k < paretoFront.size() - 1; k++) {
            individuals.get(k).setCrowdingDistance(individuals.get(k).getCrowdingDistance() + (individuals.get(k + 1).getOverallDeviation() - individuals.get(k - 1).getOverallDeviation()) / (maxOverallDeviation - minOverallDeviation));
        }

        // Objective function 2: Connectivity
        individuals.sort(Comparator.comparingDouble(Individual::getConnectivity));
        double minConnectivity = individuals.get(individuals.size() - 1).getConnectivity();
        double maxConnectivity = individuals.get(0).getConnectivity();
        individuals.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
        for (int k = 1; k < paretoFront.size() - 1; k++) {
            individuals.get(k).setCrowdingDistance(individuals.get(k).getCrowdingDistance() + (individuals.get(k + 1).getConnectivity() - individuals.get(k - 1).getConnectivity()) / (maxConnectivity - minConnectivity));
        }

        System.out.println("Crowding distances calculated in " + ((System.currentTimeMillis() - startTime)) + "ms");
    }

    private Individual tournament() {
        System.out.println("Selecting parent in tournament");
        final long startTime = System.currentTimeMillis();

        List<Individual> tournamentCompetitors = new ArrayList<>();
        List<Individual> bestRankedCompetitors = new ArrayList<>();
        int minRank = Integer.MAX_VALUE;

        for (int i = 0; i < tournamentSize; i++) {
            boolean contained = true;
            Individual competitor = null;

            while (contained) {
                int randomIndex = Utils.randomIndex(individuals.size());
                competitor = individuals.get(randomIndex);
                contained = tournamentCompetitors.contains(competitor);
            }

            tournamentCompetitors.add(competitor);

            if (competitor.getRank() < minRank) {
                bestRankedCompetitors.clear();
                bestRankedCompetitors.add(competitor);
            } else if (competitor.getRank() == minRank) {
                bestRankedCompetitors.add(competitor);
            }
        }

        if (bestRankedCompetitors.size() == 1) {
            return bestRankedCompetitors.get(0);
        }

        double minCrowdingDistance = Double.MAX_VALUE;
        Individual bestCompetitor = null;

        for (Individual competitor : bestRankedCompetitors) {
            if (competitor.getCrowdingDistance() < minCrowdingDistance) {
                bestCompetitor = competitor;
                minCrowdingDistance = competitor.getCrowdingDistance();
            }
        }

        System.out.println("Parent selected in tournament in " + ((System.currentTimeMillis() - startTime)) + "ms");

        return bestCompetitor;
    }

    private List<List<Integer>> crossOver(Individual parent, Individual otherParent, int splits) {
        System.out.println("Performing crossOver");
        final long startTime = System.currentTimeMillis();

        if (parent.getChromosome().size() != otherParent.getChromosome().size()) {
            throw new Error("Chromosomes are different size");
        }

        List<List<Integer>> newChromosomes = new ArrayList<>();
        int[] partitionIndices = Utils.generatePartitionIndices(parent.getChromosome().size(), splits);
        List<List<Integer>> partsFromParent = Utils.splitRoute(parent.getChromosome(), partitionIndices, splits);
        List<List<Integer>> partsFromOtherParent = Utils.splitRoute(otherParent.getChromosome(), partitionIndices, splits);

        List<Integer> newChromosome = new ArrayList<>();
        List<Integer> newChromosome2 = new ArrayList<>();

        if (partsFromParent.size() != splits) {
            throw new Error("Legg til +1");
        }

        for (int i = 0; i < splits; i++) {
            if (i % 2 == 0) {
                newChromosome.addAll(partsFromParent.get(i));
                newChromosome2.addAll(partsFromOtherParent.get(i));
            } else {
                newChromosome.addAll(partsFromOtherParent.get(i));
                newChromosome2.addAll(partsFromParent.get(i));
            }
        }

        if (newChromosome.size() != parent.getChromosome().size()) {
            throw new Error("Chromosomes are different size");
        }

        System.out.println("Crossover executed in " + ((System.currentTimeMillis() - startTime)) + "ms");

        newChromosomes.add(newChromosome);
        newChromosomes.add(newChromosome2);

        return newChromosomes;
    }

    private void swapMutate(List<Integer> chromosome) {
        int indexA = Utils.randomIndex(chromosome.size());
        int randomNeighborIndex = Utils.randomIndex(pixels.get(indexA).getPixelNeighbors().size());
        int indexB = pixels.get(indexA).getPixelNeighbors().get(randomNeighborIndex).getPixel().getId();
        Collections.swap(chromosome, indexA, indexB);
    }

    List<Segment> getAlphaSegments() {
        return individuals.get(0).getSegments();
    }
}
