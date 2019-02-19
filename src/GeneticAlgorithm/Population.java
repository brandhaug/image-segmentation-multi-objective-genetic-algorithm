package GeneticAlgorithm;

import java.util.ArrayList;
import java.util.List;

public class Population {
    private List<Individual> individuals = new ArrayList<>();

    // Parameters
    private int populationSize; // Number of Solutions in population
    private double crossOverRate;
    private double mutationRate;
    private int tournamentSize;


    Population(List<Pixel> pixels, List<Integer> initialChromosome, int populationSize, double crossOverRate, double mutationRate, int tournamentSize) {
        this.populationSize = populationSize;
        this.crossOverRate = crossOverRate;
        this.mutationRate = mutationRate;
        this.tournamentSize = tournamentSize;

        generateInitialPopulation(pixels, initialChromosome);
    }

    private void generateInitialPopulation(List<Pixel> pixels, List<Integer> initialChromosome) {
        System.out.println("Generating Initial Population");
        for (int i = 0; i < populationSize; i++) {
            Individual individual = new Individual(pixels, initialChromosome);
            individuals.add(individual);
        }
    }

    void tick() {

    }
}
