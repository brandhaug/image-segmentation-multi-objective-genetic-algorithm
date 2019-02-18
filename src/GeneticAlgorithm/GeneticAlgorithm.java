package GeneticAlgorithm;

import javafx.scene.canvas.GraphicsContext;

public class GeneticAlgorithm {

    // Parameters
    private final int populationSize = 100; // 20-100 dependent on problem
    private final double crossOverRate = 0.7; // 80%-95%
    private final double mutationRate = 0.01; // 0.5%-1%.
    private final int tournamentSize = 3; // Number of members in tournament selection

    private Population population;

    private int generation = 0;

    public GeneticAlgorithm() {
        population = new Population(populationSize,
                crossOverRate,
                mutationRate,
                tournamentSize);
    }

    public void tick() {
        population.tick();
        generation++;
    }

    public void save() {
    }

    public int getGeneration() {
        return generation;
    }

    public void render(GraphicsContext gc) {

    }
}
