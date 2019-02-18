package GeneticAlgorithm;


import java.util.List;

public class Individual {
    private List<Gene> chromosome; // List of genes (pixels)
    private List<Segment> segments; // List of segments (set of pixels)
    private double fitness;

    public Individual() {
        generateInitialIndividual();
    }

    private void generateInitialIndividual() {

    }
}
