package GeneticAlgorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents all individuals
 */
public class Population {
    private List<Individual> individuals = new ArrayList<>();

    // Parameters
    private int populationSize; // Number of Solutions in population
    private double crossOverRate;
    private double mutationRate;
    private int tournamentSize;


    Population(List<Pixel> pixels, List<Integer> initialChromosome, double initialColorDistanceThreshold, int populationSize, double crossOverRate, double mutationRate, int tournamentSize) throws InterruptedException {
        this.populationSize = populationSize;
        this.crossOverRate = crossOverRate;
        this.mutationRate = mutationRate;
        this.tournamentSize = tournamentSize;

        generateInitialPopulation(pixels, initialChromosome, initialColorDistanceThreshold);
    }

    private void generateInitialPopulation(List<Pixel> pixels, List<Integer> initialChromosome, double initialColorDistanceThreshold) throws InterruptedException {
        System.out.println("Generating Initial Population");
        for (int i = 0; i < populationSize; i++) {
            Individual individual = new Individual(pixels, initialChromosome, initialColorDistanceThreshold);
            individual.start(); // Start thread by calling run method
            individuals.add(individual);
        }

        for (Individual individual : individuals) {
            individual.join(); // Wait for thread to terminate
        }
    }

    /**
     * NSGA-II
     */
    void tick() {
        fastNonDominatedSort();
        for (Individual individual : individuals) {
            individual.calculateObjectiveFunctions();
        }

//        individuals.sort(Comparator.comparingDouble(Individual::getRank));
    }

    /**
     * Ranking individuals based on how many individuals dominates it
     * Based on page 3 in NSGA-II paper by Kalyanmoy Deb, Amrit Pratap, Sameer Agarwal, and T. Meyarivan
     */
    private void fastNonDominatedSort() {
        List<Individual> front = new ArrayList<>(); // F

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
                individual.setRank(1);
                front.add(individual);
            }
        }

        int rank = 2;
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
    }

    List<Segment> getAlphaSegments() {
        return individuals.get(0).getSegments();
    }
}
