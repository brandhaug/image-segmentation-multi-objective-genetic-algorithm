package GeneticAlgorithm;

import java.util.List;

public class Population {
    private List<Individual> individuals;

    // Parameters
    private int populationSize; // Number of Solutions in population
    private double crossOverRate;
    private double mutationRate;
    private int tournamentSize;

    public Population(int populationSize, double crossOverRate, double mutationRate, int tournamentSize) {
    }

    public void tick() {
    }

    /**
     * Minimum Spanning Tree (MST)
     */
    private void generateInitialPopulation() {
        for (int i = 0; i < populationSize; i++) {
            Individual individual = new Individual();
            individuals.add(individual);
        }
    }
}
