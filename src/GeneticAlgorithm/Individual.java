package GeneticAlgorithm;


import Utils.Utils;

import java.util.ArrayList;
import java.util.List;

class Individual {
    private List<Integer> chromosome; // List of genes (pixels)
    private List<Segment> segments; // List of segments (set of pixels)
    private double fitness;

    Individual(List<Pixel> pixels, List<Integer> initialChromosome) {
        this.chromosome = new ArrayList<>(initialChromosome);
        generateInitialIndividual(pixels);
    }

    /**
     * Minimum Spanning Tree (MST)
     */
    private void generateInitialIndividual(List<Pixel> pixels) {
        List<Integer> visitedPixelIndices = new ArrayList<>();
        List<Pixel> pixelsCopy = new ArrayList<Pixel>(pixels); // Remove chosen vertices to make randomIndex effective

        while (pixelsCopy.size() != 0) { // Breaks when
            // Random initial vertex
            int randomIndex = Utils.randomIndex(pixelsCopy.size());
            Pixel randomPixel = pixelsCopy.get(randomIndex);
            visitedPixelIndices.add(randomPixel.getId());

            double minDistance = Double.MAX_VALUE;
            List<Integer> bestPixelIndices = new ArrayList<>();
            List<Integer> bestNeighborIndices = new ArrayList<>(); // Select neighbor stochastic


            for (int visitedPixelIndex : visitedPixelIndices) { // Finding best neighbors in visited pixels
                Pixel visitedPixel = pixels.get(visitedPixelIndex);
                for (PixelNeighbor neighbor : visitedPixel.getNeighbors()) { // TODO: Null pointer here
                    if (!visitedPixelIndices.contains(neighbor.getPixel().getId())) { // Check if already visited
                        if (chromosome.get(visitedPixelIndex) == pixels.get(visitedPixelIndex).getId()) { // Check if points to self

                            // Neighbor is valid: Not visited and points to self (default value)

                            if (neighbor.getColorDistance() < minDistance) { // Better neighbor: clear and add
                                bestPixelIndices.clear();
                                bestNeighborIndices.clear();
                                bestPixelIndices.add(visitedPixelIndex);
                                bestNeighborIndices.add(neighbor.getPixel().getId());
                            } else if (neighbor.getColorDistance() == minDistance) { // Equal good neighbor: add
                                bestPixelIndices.add(visitedPixelIndex);
                                bestNeighborIndices.add(neighbor.getPixel().getId());
                            }
                        }
                    }
                }
            }

            if (bestNeighborIndices.size() != bestPixelIndices.size()) {
                throw new Error("BestNeighborIndices size is not equal to BestGeneIndices size");
            }

            if (bestNeighborIndices.isEmpty()) {
                System.out.println("Best Neighbors is empty");
                break;
            } else {
                randomIndex = Utils.randomIndex(bestNeighborIndices.size()); // Random best neighbor index
                int bestGeneId = bestPixelIndices.get(randomIndex);
                int bestNeighborIndex = bestNeighborIndices.get(randomIndex);
                pixelsCopy.remove(pixels.get(bestGeneId));

                chromosome.set(bestGeneId, bestNeighborIndex);
                visitedPixelIndices.add(bestNeighborIndex);
            }

            if (pixelsCopy.size() % 100 == 0) {
                System.out.println("End of while loop - PixelsCopy size: " + pixelsCopy.size());
            }
        }

        // TODO: Change to Map so you can always get with id to avoid index-based extra lists
        // TODO: Can i sort by distance? The order is specified in project task info
        // TODO: Add to segment
        // TODO: Do we need a threshold? ex. If colorDistance > threshold: point pixel to self

        System.out.println("Finished");
    }
}
