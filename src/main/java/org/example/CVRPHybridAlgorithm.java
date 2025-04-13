package org.example;

import org.example.models.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CVRPHybridAlgorithm implements Algorithm 7 from the documentation.
 * It combines genetic algorithm, tabu search, and 3-opt local search
 * for solving the Capacitated Vehicle Routing Problem with IoT constraints.
 */
public class CVRPHybridAlgorithm {
    // Population parameters
    private final int populationSize;
    private final int maxIterations;
    private final int noImprovementThreshold;

    // Genetic algorithm parameters
    private final double crossoverRate;
    private final double mutationRate;

    // Add early termination flag and timeout parameters
    private volatile boolean terminateEarly = false;
    private final long maxExecutionTimeMs;

    // Problem data
    private final List<Vehicle> vehicles;
    private final List<Location> locations;
    private final List<Location> refuelingStations;
    private final List<WeatherCondition> weatherConditions;
    private final List<RoadEvent> roadEvents;
    private final List<TrafficCongestion> trafficCongestions;

    // Helper classes for search algorithms
    private final TabuSearch tabuSearch;
    private final ThreeOptLocalSearch threeOptLocalSearch;

    /**
     * Constructor with default genetic algorithm parameters
     */
    public CVRPHybridAlgorithm(
            List<Vehicle> vehicles,
            List<Location> locations,
            List<WeatherCondition> weatherConditions,
            List<RoadEvent> roadEvents,
            List<TrafficCongestion> trafficCongestions,
            long maxExecutionTimeMs) {
        this(vehicles, locations, weatherConditions, roadEvents, trafficCongestions,
                maxExecutionTimeMs, 0.8, 0.2);
    }

    /**
     * Constructor with customizable genetic algorithm parameters
     */
    public CVRPHybridAlgorithm(
            List<Vehicle> vehicles,
            List<Location> locations,
            List<WeatherCondition> weatherConditions,
            List<RoadEvent> roadEvents,
            List<TrafficCongestion> trafficCongestions,
            long maxExecutionTimeMs,
            double crossoverRate,
            double mutationRate) {

        this.vehicles = vehicles;
        this.locations = locations;
        this.weatherConditions = weatherConditions;
        this.roadEvents = roadEvents;
        this.trafficCongestions = trafficCongestions;
        this.maxExecutionTimeMs = maxExecutionTimeMs;
        this.crossoverRate = crossoverRate;
        this.mutationRate = mutationRate;

        this.refuelingStations = locations.stream()
                .filter(loc -> loc.getType().equalsIgnoreCase("refueling_station"))
                .collect(Collectors.toList());

        // Default population parameters
        this.populationSize = 50;
        this.maxIterations = 100;
        this.noImprovementThreshold = 30;

        // Initialize tabu search with tabu size 10 and 20 iterations
        this.tabuSearch = new TabuSearch(
                weatherConditions, roadEvents, trafficCongestions,
                refuelingStations, 50, 20);

        // Initialize 3-opt local search
        this.threeOptLocalSearch = new ThreeOptLocalSearch(
                weatherConditions, roadEvents, trafficCongestions, refuelingStations);

        System.out.println("CVRP Hybrid Algorithm Configuration:");
        System.out.println("- Population Size: " + populationSize);
        System.out.println("- Max Iterations: " + maxIterations);
        System.out.println("- No Improvement Threshold: " + noImprovementThreshold);
        System.out.println("- Crossover Rate: " + crossoverRate);
        System.out.println("- Mutation Rate: " + mutationRate);
        System.out.println("- Max Execution Time: " + maxExecutionTimeMs + "ms");
        System.out.println("- Refueling Stations: " + refuelingStations.size());
        System.out.println("- TabuSize: " + tabuSearch.getTabuSize());
    }

    /**
     * Signal to terminate algorithm
     */
    public void terminate() {
        this.terminateEarly = true;
        System.out.println("Termination signal received. Algorithm will stop at next safe point.");
    }

    /**
     * Main optimization method implementing Algorithm 7
     */
    public List<Route> findOptimalRoutes() {
        System.out.println("Starting optimization with " + vehicles.size() + " vehicles and " +
                locations.size() + " locations");

        // Create initial population
        List<List<Route>> population = initializePopulation();

        if (population.isEmpty()) {
            System.err.println("Population initialization failed");
            return createFallbackSolution();
        }

        // Find initial best solution
        List<Route> bestSolution = getBestIndividual(population);
        double bestFitness = calculateFitness(bestSolution);

        System.out.println("Initial best solution fitness: " + bestFitness);

        int iterations = 0;
        int noImprovements = 0;
        long startTime = System.currentTimeMillis();

        // Main loop (Algorithm 7, lines 5-34)
        while (iterations < maxIterations &&
                noImprovements < noImprovementThreshold &&
                !terminateEarly) {

            // Check for timeout
            if (System.currentTimeMillis() - startTime > maxExecutionTimeMs) {
                System.out.println("Time limit reached. Terminating optimization.");
                break;
            }

            // Process each individual (Algorithm 7, lines 6-19)
            for (int i = 0; i < population.size() && !terminateEarly; i++) {
                List<Route> individual = population.get(i);

                // Better route initialization (lines 7-8)
                List<Route> betterRoute = new ArrayList<>(individual);
                double betterFitness = calculateFitness(betterRoute);

                // Perturbation (line 9)
                List<Route> perturbedRoute = perturbIndividual(betterRoute);

                // Apply mutation with probability - Reverse swap (line 10)
                List<Route> mutatedRoute;
                if (Math.random() < mutationRate) {
                    mutatedRoute = applyReverseSwapMutation(perturbedRoute);
                } else {
                    mutatedRoute = perturbedRoute;
                }

                // Apply crossover with probability - Single point (line 11)
                List<Route> crossedRoute;
                if (Math.random() < crossoverRate) {
                    crossedRoute = applySinglePointCrossover(mutatedRoute,
                            selectRandomIndividual(population, i));
                } else {
                    crossedRoute = mutatedRoute;
                }

                // Apply selection (line 12)
                List<Route> selectedRoute = applySelection(crossedRoute);

                // Apply Tabu Search (line 13)
                List<Route> tabuOptimizedRoute = tabuSearch.applyTabuSearch(selectedRoute);

                // Sort routes by fitness (line 14)
                double currentFitness = calculateFitness(tabuOptimizedRoute);

                // Update better route if improvement found (lines 15-18)
                if (currentFitness < betterFitness) {
                    betterRoute = tabuOptimizedRoute;
                    betterFitness = currentFitness;
                }

                // Update the individual in the population
                population.set(i, betterRoute);
            }

            // Find best route in current iteration (lines 20-21)
            List<Route> currentBestRoute = getBestIndividual(population);
            double currentBestFitness = calculateFitness(currentBestRoute);

            // Apply 3-opt local search to best route (lines 22-23)
            List<Route> localSearchRoute = threeOptLocalSearch.apply3OptLocalSearch(currentBestRoute);
            double localSearchFitness = calculateFitness(localSearchRoute);

            // Update best solution if improvement found (lines 24-28)
            if (localSearchFitness < bestFitness) {
                bestSolution = localSearchRoute;
                bestFitness = localSearchFitness;
                noImprovements = 0;
                System.out.println("New best solution found: " + bestFitness);
            } else {
                noImprovements++;
            }

            iterations++;

            if (iterations % 10 == 0) {
                System.out.println("Iteration " + iterations + ", Best fitness: " + bestFitness);
            }
        }

        long totalTime = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("Optimization completed after " + iterations + " iterations and " + totalTime + " seconds");
        System.out.println("Best solution fitness: " + bestFitness);

        return bestSolution;
    }

    /**
     * Initialize population with random individuals
     */
    private List<List<Route>> initializePopulation() {
        List<List<Route>> population = new ArrayList<>();

        try {
            // Find depot
            Location depot = locations.stream()
                    .filter(loc -> loc.getType().equalsIgnoreCase("depot"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No depot found"));

            // Get customer locations
            List<Location> customers = locations.stream()
                    .filter(loc -> !loc.getType().equalsIgnoreCase("depot") &&
                            !loc.getType().equalsIgnoreCase("refueling_station"))
                    .collect(Collectors.toList());

            for (int i = 0; i < populationSize; i++) {
                // Create a random solution
                List<Route> individual = createRandomSolution(depot, customers);
                population.add(individual);
            }
        } catch (Exception e) {
            System.err.println("Error initializing population: " + e.getMessage());
            e.printStackTrace();
        }

        return population;
    }

    /**
     * Create a random solution by assigning customers to vehicles
     */
    private List<Route> createRandomSolution(Location depot, List<Location> customers) {
        List<Route> solution = new ArrayList<>();
        List<Location> unassignedCustomers = new ArrayList<>(customers);
        Collections.shuffle(unassignedCustomers); // Randomize customer order

        for (Vehicle vehicle : vehicles) {
            if (unassignedCustomers.isEmpty()) {
                break;
            }

            List<Location> routeLocations = new ArrayList<>();
            routeLocations.add(depot); // Start at depot

            double remainingCapacity = vehicle.getPayloadCapacity();
            Iterator<Location> iterator = unassignedCustomers.iterator();

            while (iterator.hasNext()) {
                Location customer = iterator.next();
                if (customer.getDemand() <= remainingCapacity) {
                    routeLocations.add(customer);
                    remainingCapacity -= customer.getDemand();
                    iterator.remove();
                }
            }

            if (routeLocations.size() > 1) { // If at least one customer assigned
                routeLocations.add(depot); // End at depot
                Route route = new Route(vehicle, routeLocations, weatherConditions,
                        roadEvents, trafficCongestions, refuelingStations);
                solution.add(route);
            }
        }

        // If there are still unassigned customers, create additional routes
        if (!unassignedCustomers.isEmpty()) {
            int vehicleIndex = 0;

            while (!unassignedCustomers.isEmpty() && vehicleIndex < vehicles.size()) {
                Vehicle vehicle = vehicles.get(vehicleIndex);

                List<Location> routeLocations = new ArrayList<>();
                routeLocations.add(depot);

                double remainingCapacity = vehicle.getPayloadCapacity();
                Iterator<Location> iterator = unassignedCustomers.iterator();

                while (iterator.hasNext()) {
                    Location customer = iterator.next();
                    if (customer.getDemand() <= remainingCapacity) {
                        routeLocations.add(customer);
                        remainingCapacity -= customer.getDemand();
                        iterator.remove();
                    }
                }

                if (routeLocations.size() > 1) {
                    routeLocations.add(depot);
                    Route route = new Route(vehicle, routeLocations, weatherConditions,
                            roadEvents, trafficCongestions, refuelingStations);
                    solution.add(route);
                }

                vehicleIndex++;
            }
        }

        return solution;
    }

    /**
     * Create a simple fallback solution when optimization fails
     */
    private List<Route> createFallbackSolution() {
        System.out.println("Creating fallback solution");
        List<Route> fallbackSolution = new ArrayList<>();

        // Find depot
        Location depot = locations.stream()
                .filter(loc -> loc.getType().equalsIgnoreCase("depot"))
                .findFirst()
                .orElse(null);

        if (depot == null) {
            System.err.println("No depot found. Cannot create fallback solution.");
            return new ArrayList<>();
        }

        // Find customers
        List<Location> customers = locations.stream()
                .filter(loc -> !loc.getType().equalsIgnoreCase("depot") &&
                        !loc.getType().equalsIgnoreCase("refueling_station"))
                .collect(Collectors.toList());

        // Create a route for each vehicle
        int vehicleIndex = 0;

        while (!customers.isEmpty() && vehicleIndex < vehicles.size()) {
            Vehicle vehicle = vehicles.get(vehicleIndex);
            List<Location> routeLocations = new ArrayList<>();
            routeLocations.add(depot);

            double remainingCapacity = vehicle.getPayloadCapacity();
            Iterator<Location> iterator = customers.iterator();

            while (iterator.hasNext()) {
                Location customer = iterator.next();
                if (customer.getDemand() <= remainingCapacity) {
                    routeLocations.add(customer);
                    remainingCapacity -= customer.getDemand();
                    iterator.remove();
                }
            }

            if (routeLocations.size() > 1) {
                routeLocations.add(depot);
                Route route = new Route(vehicle, routeLocations, weatherConditions,
                        roadEvents, trafficCongestions, refuelingStations);
                fallbackSolution.add(route);
            }

            vehicleIndex++;
        }

        return fallbackSolution;
    }

    /**
     * Calculate fitness (total cost) of a solution
     */
    private double calculateFitness(List<Route> solution) {
        return solution.stream()
                .mapToDouble(Route::getTotalCost)
                .sum();
    }

    /**
     * Get the best individual from population
     */
    private List<Route> getBestIndividual(List<List<Route>> population) {
        return population.stream()
                .min(Comparator.comparingDouble(this::calculateFitness))
                .orElse(new ArrayList<>());
    }

    /**
     * Select a random individual from population
     */
    private List<Route> selectRandomIndividual(List<List<Route>> population, int excludeIndex) {
        Random rand = new Random();
        int index;

        do {
            index = rand.nextInt(population.size());
        } while (index == excludeIndex);

        return new ArrayList<>(population.get(index));
    }

    /**
     * Perturb individual by swapping random customers between routes
     */
    /**
     * Perturb individual by swapping random customers between routes
     */
    private List<Route> perturbIndividual(List<Route> individual) {
        if (individual.size() < 2) {
            return individual;
        }

        List<Route> perturbed = deepCopy(individual);
        Random rand = new Random();

        // Select two random routes
        int route1Index = rand.nextInt(perturbed.size());
        int route2Index = rand.nextInt(perturbed.size());

        if (route1Index == route2Index) {
            route2Index = (route1Index + 1) % perturbed.size();
        }

        Route route1 = perturbed.get(route1Index);
        Route route2 = perturbed.get(route2Index);

        // Get customers from both routes
        List<Location> customers1 = getCustomers(route1.getLocations());
        List<Location> customers2 = getCustomers(route2.getLocations());

        if (customers1.isEmpty() || customers2.isEmpty()) {
            return perturbed;
        }

        // Select a random customer from each route
        Location customer1 = customers1.get(rand.nextInt(customers1.size()));
        Location customer2 = customers2.get(rand.nextInt(customers2.size()));

        // Check capacity constraints for the swap
        Vehicle vehicle1 = route1.getVehicle();
        Vehicle vehicle2 = route2.getVehicle();

        double demand1 = route1.getTotalDemand() - customer1.getDemand() + customer2.getDemand();
        double demand2 = route2.getTotalDemand() - customer2.getDemand() + customer1.getDemand();

        if (demand1 <= vehicle1.getPayloadCapacity() && demand2 <= vehicle2.getPayloadCapacity()) {
            // Swap the customers
            List<Location> locations1 = new ArrayList<>(route1.getLocations());
            List<Location> locations2 = new ArrayList<>(route2.getLocations());

            int pos1 = locations1.indexOf(customer1);
            int pos2 = locations2.indexOf(customer2);

            locations1.set(pos1, customer2);
            locations2.set(pos2, customer1);

            route1.setLocations(locations1);
            route2.setLocations(locations2);
        }

        return perturbed;
    }

    /**
     * Apply reverse swap mutation: reverse a segment of a route
     */
    private List<Route> applyReverseSwapMutation(List<Route> individual) {
        if (individual.isEmpty()) {
            return individual;
        }

        List<Route> mutated = deepCopy(individual);
        Random rand = new Random();

        // Select a random route
        int routeIndex = rand.nextInt(mutated.size());
        Route route = mutated.get(routeIndex);
        List<Location> locations = new ArrayList<>(route.getLocations());

        // Only consider customer locations (not depot or refueling stations)
        List<Integer> customerIndices = new ArrayList<>();
        for (int i = 0; i < locations.size(); i++) {
            Location loc = locations.get(i);
            if (!loc.getType().equalsIgnoreCase("depot") &&
                    !loc.getType().equalsIgnoreCase("refueling_station")) {
                customerIndices.add(i);
            }
        }

        if (customerIndices.size() < 2) {
            return mutated;
        }

        // Select two random indices to define segment to reverse
        Collections.sort(customerIndices);
        int startIdx = rand.nextInt(customerIndices.size() - 1);
        int endIdx = startIdx + 1 + rand.nextInt(customerIndices.size() - startIdx - 1);

        int actualStartIdx = customerIndices.get(startIdx);
        int actualEndIdx = customerIndices.get(endIdx);

        // Reverse the segment
        List<Location> segment = new ArrayList<>(locations.subList(actualStartIdx, actualEndIdx + 1));
        Collections.reverse(segment);

        for (int i = 0; i <= actualEndIdx - actualStartIdx; i++) {
            locations.set(actualStartIdx + i, segment.get(i));
        }

        route.setLocations(locations);
        return mutated;
    }

    /**
     * Apply single point crossover between two individuals
     */
    private List<Route> applySinglePointCrossover(List<Route> parent1, List<Route> parent2) {
        // Create offspring by combining routes from both parents
        List<Route> offspring = new ArrayList<>();
        Set<Location> assignedCustomers = new HashSet<>();

        // Find depot
        Location depot = locations.stream()
                .filter(loc -> loc.getType().equalsIgnoreCase("depot"))
                .findFirst()
                .orElse(null);

        if (depot == null) {
            return parent1; // Cannot perform crossover without depot
        }

        // Add routes from parent1
        for (Route route : parent1) {
            Vehicle vehicle = route.getVehicle();
            List<Location> routeLocations = new ArrayList<>();
            routeLocations.add(depot);

            double remainingCapacity = vehicle.getPayloadCapacity();

            // Add customers that aren't already assigned
            for (Location location : route.getLocations()) {
                if (!location.getType().equalsIgnoreCase("depot") &&
                        !location.getType().equalsIgnoreCase("refueling_station") &&
                        !assignedCustomers.contains(location)) {

                    if (location.getDemand() <= remainingCapacity) {
                        routeLocations.add(location);
                        assignedCustomers.add(location);
                        remainingCapacity -= location.getDemand();
                    }
                }
            }

            if (routeLocations.size() > 1) {
                routeLocations.add(depot);
                Route newRoute = new Route(vehicle, routeLocations, weatherConditions,
                        roadEvents, trafficCongestions, refuelingStations);
                offspring.add(newRoute);
            }
        }

        // Add unassigned customers from parent2
        for (Route route : parent2) {
            for (Location location : route.getLocations()) {
                if (!location.getType().equalsIgnoreCase("depot") &&
                        !location.getType().equalsIgnoreCase("refueling_station") &&
                        !assignedCustomers.contains(location)) {

                    // Find a vehicle with available capacity
                    for (int i = 0; i < offspring.size(); i++) {
                        Route offspringRoute = offspring.get(i);
                        Vehicle vehicle = offspringRoute.getVehicle();
                        double remainingCapacity = vehicle.getPayloadCapacity() - offspringRoute.getTotalDemand();

                        if (location.getDemand() <= remainingCapacity) {
                            // Add customer to this route
                            List<Location> routeLocations = new ArrayList<>(offspringRoute.getLocations());
                            // Insert before the final depot
                            routeLocations.add(routeLocations.size() - 1, location);
                            offspringRoute.setLocations(routeLocations);
                            assignedCustomers.add(location);
                            break;
                        }
                    }
                }
            }
        }

        // Check if we have unassigned customers
        Set<Location> allCustomers = getAllCustomers();
        Set<Location> unassignedCustomers = new HashSet<>(allCustomers);
        unassignedCustomers.removeAll(assignedCustomers);

        if (!unassignedCustomers.isEmpty()) {
            // Create additional routes for unassigned customers
            int vehicleIndex = 0;

            for (Vehicle vehicle : vehicles) {
                if (unassignedCustomers.isEmpty()) {
                    break;
                }

                List<Location> routeLocations = new ArrayList<>();
                routeLocations.add(depot);

                double remainingCapacity = vehicle.getPayloadCapacity();
                Iterator<Location> iterator = unassignedCustomers.iterator();

                while (iterator.hasNext()) {
                    Location customer = iterator.next();
                    if (customer.getDemand() <= remainingCapacity) {
                        routeLocations.add(customer);
                        remainingCapacity -= customer.getDemand();
                        iterator.remove();
                    }
                }

                if (routeLocations.size() > 1) {
                    routeLocations.add(depot);
                    Route route = new Route(vehicle, routeLocations, weatherConditions,
                            roadEvents, trafficCongestions, refuelingStations);
                    offspring.add(route);
                }

                vehicleIndex++;
            }
        }

        return offspring;
    }

    /**
     * Apply selection by choosing the better individual
     */
    private List<Route> applySelection(List<Route> individual) {
        // In this simple implementation, we just return the individual
        // Could be expanded to compare with other individuals
        return individual;
    }

    /**
     * Get all customer locations from the problem
     */
    private Set<Location> getAllCustomers() {
        return locations.stream()
                .filter(loc -> !loc.getType().equalsIgnoreCase("depot") &&
                        !loc.getType().equalsIgnoreCase("refueling_station"))
                .collect(Collectors.toSet());
    }

    /**
     * Extract customer locations from a route
     */
    private List<Location> getCustomers(List<Location> routeLocations) {
        return routeLocations.stream()
                .filter(loc -> !loc.getType().equalsIgnoreCase("depot") &&
                        !loc.getType().equalsIgnoreCase("refueling_station"))
                .collect(Collectors.toList());
    }

    /**
     * Create a deep copy of a solution
     */
    private List<Route> deepCopy(List<Route> solution) {
        List<Route> copy = new ArrayList<>();

        for (Route route : solution) {
            Route newRoute = new Route(
                    route.getVehicle(),
                    new ArrayList<>(route.getLocations()),
                    weatherConditions,
                    roadEvents,
                    trafficCongestions,
                    refuelingStations
            );
            copy.add(newRoute);
        }

        return copy;
    }
}