package GeneticAlgorithm;

import Utils.Utils;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
                Individual individual = new Individual();
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
    void tick() throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        List<Individual> offspringIndividuals = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


        int loops = GeneticAlgorithm.populationSize / 2; // Because crossover produces 2 children
        for (int i = 0; i < loops; i++) {
            // Selection
            Individual parent = tournament();
            Individual otherParent = tournament();

            // Crossover
            List<List<Integer>> newChromosomes = crossOver(parent, otherParent, GeneticAlgorithm.numberOfSplits);

            // Mutation
            for (List<Integer> newChromosome : newChromosomes) {
                if (offspringIndividuals.size() != GeneticAlgorithm.populationSize) {
                    double random = Utils.randomDouble();
                    if (random < GeneticAlgorithm.mutationRate) {
                        swapMutate(newChromosome);
                    }

                    // Add offspring
                    executorService.execute(() -> {
                        Individual newIndividual = new Individual(newChromosome);
                        offspringIndividuals.add(newIndividual);
                    });
                }
            }
        }

        final long startTime2 = System.currentTimeMillis();
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        System.out.println("Segments in " + offspringIndividuals.size() + " offspring individuals calculated in " + ((System.currentTimeMillis() - startTime2) / 1000) + "s");

        int averageSegmentsSize = 0;
        for (Individual offspringIndividual : offspringIndividuals) {
            averageSegmentsSize += offspringIndividual.getSegments().size();
        }

        averageSegmentsSize = averageSegmentsSize / offspringIndividuals.size();
        System.out.println("Average segment size in offspring: " + averageSegmentsSize);

        individuals.addAll(offspringIndividuals);

        fastNonDominatedSort();
        calculateCrowdingDistances();

        individuals.sort(Comparator.comparingDouble(Individual::getRank).thenComparing(Individual::getCrowdingDistance, Collections.reverseOrder()));
        individuals = individuals.subList(0, GeneticAlgorithm.populationSize);

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

    private Individual tournament() {
        List<Individual> tournamentCompetitors = new ArrayList<>();
        List<Individual> bestRankedCompetitors = new ArrayList<>();
        int minRank = Integer.MAX_VALUE;

        for (int i = 0; i < GeneticAlgorithm.tournamentSize; i++) {
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
                minRank = competitor.getRank();
            } else if (competitor.getRank() == minRank) {
                bestRankedCompetitors.add(competitor);
            }
        }

        if (bestRankedCompetitors.size() == 0) {
            throw new Error("No competitors");
        }

        if (bestRankedCompetitors.size() == 1) {
            return bestRankedCompetitors.get(0);
        }

        double maxCrowdingDistance = -Double.MAX_VALUE;
        Individual bestCompetitor = null;

        for (Individual competitor : bestRankedCompetitors) {
            if (competitor.getCrowdingDistance() > maxCrowdingDistance) {
                bestCompetitor = competitor;
                maxCrowdingDistance = competitor.getCrowdingDistance();
            }
        }

        if (bestCompetitor == null) {
            throw new NullPointerException("BestCompetitor is null");
        }

        return bestCompetitor;
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

        return newChromosomes;
    }

    private void swapMutate(List<Integer> chromosome) {
        int indexA = Utils.randomIndex(chromosome.size());
        int randomNeighborIndex = Utils.randomIndex(GeneticAlgorithm.pixels.get(indexA).getPixelNeighbors().size());
        int indexB = GeneticAlgorithm.pixels.get(indexA).getPixelNeighbors().get(randomNeighborIndex).getPixel().getId();
        Collections.swap(chromosome, indexA, indexB);
    }

    List<Segment> getRandomParetoSegments() {
        int randomIndex;
        do {
            randomIndex = Utils.randomIndex(individuals.size());
        } while (individuals.get(randomIndex).getRank() != 1);

        return individuals.get(randomIndex).getSegments();
    }

    List<Individual> getIndividuals() {
        return individuals;
    }
}
