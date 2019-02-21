## Image Segmentation solved with a Multi-Objective Genetic Algorithm
### Image Segmentation
Image segmentation is the process of partitioning a digital image into multiple segments (sets of pixels). 
The goal of segmentation is to simplify and/or change the representation of an image into something that is more 
meaningful and easier to analyze. More precisely, image segmentation is the process of assigning a label to every pixel 
in an image such that pixels with the same label share certain characteristics.
                                                       
### Genetic Algorithms
The Genetic Algorithm (GA) is a _metaheuristic_ (a higher-level procedure or heuristic designed to find, generate, 
or select a heuristic). It is is based on a parallel search mechanism, which makes it more efficient than other                                                                                                                               classical optimization techniques such as branch and bound, tabu search method and simulated annealing

The algorithm is inspired by the process of natural selection that belongs to the larger class of evolutionary algorithms (EA). 

GAs are commonly used to generate high-quality solutions to optimization and search problems by relying on bio-inspired operators such as mutation, crossover and selection.

#### Idea
Survival of the fittest through natural selection
* Generate a set of random solutions
* Repeat the following until best individual is good enough:
  * Test each individual in the set (rank them)
  * Remove some bad solutions from set
  * Duplicate some good solutions
  * Make small changes to some of them

### Multi-Objective Optimization
Multi-objective optimization is an area of multiple criteria decision making that is concerned with mathematical 
optimization problems involving more than one objective function to be optimized simultaneously. 

Example: Minimizing cost while maximizing comfort while buying a car, and maximizing performance whilst minimizing 
fuel consumption and emission of pollutants of a vehicle.