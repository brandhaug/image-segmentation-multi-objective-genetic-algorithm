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
    private List<Individual> paretoFront;

    Population() throws InterruptedException {
        generateInitialPopulation();
    }

    private void generateInitialPopulation() throws InterruptedException {
        System.out.println("Generating Initial Population");
        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < GeneticAlgorithm.populationSize; i++) {
            Individual individual = new Individual();
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
        System.out.println("Starting new generation");
        final long startTime = System.currentTimeMillis();

        fastNonDominatedSort();
        calculateCrowdingDistances();

//        int numberOfParentsToKeep = (populationSize - (int) (populationSize * crossOverRate));
//        List<Individual> newIndividuals = new ArrayList<>(individuals.subList(0, numberOfParentsToKeep));

        List<Individual> newIndividuals = paretoFront;

        System.out.println("Number of pareto optimal solutions: " + paretoFront.size());

        while (newIndividuals.size() != GeneticAlgorithm.populationSize) {
            int randomIndex = Utils.randomIndex(individuals.size());
            Individual i = new Individual(individuals.get(randomIndex).getChromosome());
            i.start();
            newIndividuals.add(i);
            // Selection
            Individual parent = tournament();
            Individual otherParent = tournament();

            // Crossover
            List<List<Integer>> newChromosomes = crossOver(parent, otherParent, GeneticAlgorithm.numberOfSplits);

            // Mutation
            for (List<Integer> newChromosome : newChromosomes) {
                if (newIndividuals.size() != GeneticAlgorithm.populationSize) {
                    double random = Utils.randomDouble();
                    if (random < GeneticAlgorithm.mutationRate) {
//                        swapMutate(newChromosome);
                    }

                    // Add offspring
                    Individual newIndividual = new Individual(newChromosome);
                    newIndividual.start();
                    newIndividuals.add(newIndividual);
                }
            }
        }

        System.out.println("Starting new generation");
        final long startTime2 = System.currentTimeMillis();
        for (Individual newIndividual : newIndividuals) {
            newIndividual.join(); // Wait for thread to terminate
        }
        System.out.println("Segments in offspring calculated in " + ((System.currentTimeMillis() - startTime2) / 1000) + "s");

        individuals = newIndividuals;

        individuals.sort(Comparator.comparingDouble(Individual::getRank));
        System.out.println("New generation generated in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
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
                    if (individual.dominates(individualToCompare)) { // Add to the set of solutions dominated (S)
                        individual.addToDominatedIndividuals(individualToCompare);
                    } else if (individualToCompare.dominates(individual)) {
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

        if (newChromosome.size() != newChromosome2.size()) {
            throw new Error("Chromosomes are different size");
        }

        System.out.println("Crossover executed in " + ((System.currentTimeMillis() - startTime)) + "ms");

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

    List<Segment> getAlphaSegments() {
        return individuals.get(0).getSegments();
    }
}
