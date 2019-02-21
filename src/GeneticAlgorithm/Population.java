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

    void tick() {
        for (Individual individual : individuals) {
            /**
             * NSGA-II
             */

            individual.calculateObjectiveFunctions();
        }

        individuals.sort(Comparator.comparingDouble(Individual::getFitness));
    }

    public List<Segment> getAlphaSegments() {
        return individuals.get(0).getSegments();
    }
}
