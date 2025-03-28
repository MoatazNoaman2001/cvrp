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

    // Tabu Search parameters
    private final int tabuSize = 10; // Size of tabu list
    private final List<List<Integer>> tabuList = new ArrayList<>(); // Tabu list of recent moves

    public CVRPHybridAlgorithm(
            List<Vehicle> vehicles,
            List<Location> locations,
            List<WeatherCondition> weatherConditions,
            List<RoadEvent> roadEvents,
            List<TrafficCongestion> trafficCongestions,
            long maxExecutionTimeMs) {

        this.vehicles = vehicles;
        this.locations = locations;
        this.weatherConditions = weatherConditions;
        this.roadEvents = roadEvents;
        this.trafficCongestions = trafficCongestions;
        this.maxExecutionTimeMs = maxExecutionTimeMs;

        this.refuelingStations = locations.stream()
                .filter(loc -> loc.getType().equalsIgnoreCase("refueling_station"))
                .collect(Collectors.toList());

        // Default population parameters
        this.populationSize = 50;
        this.maxIterations = 100;
        this.noImprovementThreshold = 30;

        System.out.println("CVRP Hybrid Algorithm Configuration:");
        System.out.println("- Population Size: " + populationSize);
        System.out.println("- Max Iterations: " + maxIterations);
        System.out.println("- No Improvement Threshold: " + noImprovementThreshold);
        System.out.println("- Max Execution Time: " + maxExecutionTimeMs + "ms");
        System.out.println("- Refueling Stations: " + refuelingStations.size());
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

                // Apply mutation - Reverse swap (line 10)
                List<Route> mutatedRoute = applyReverseSwapMutation(perturbedRoute);

                // Apply crossover - Single point (line 11)
                List<Route> crossedRoute = applySinglePointCrossover(mutatedRoute,
                        selectRandomIndividual(population, i));

                // Apply selection (line 12)
                List<Route> selectedRoute = applySelection(crossedRoute);

                // Apply Tabu Search (line 13)
                List<Route> tabuOptimizedRoute = applyTabuSearch(selectedRoute);

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
            List<Route> localSearchRoute = apply3OptLocalSearch(currentBestRoute);
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
     * Apply tabu search to improve the solution
     */
    private List<Route> applyTabuSearch(List<Route> individual) {
        List<Route> current = deepCopy(individual);
        List<Route> bestSolution = deepCopy(individual);
        double bestFitness = calculateFitness(bestSolution);

        // Clear tabu list if it gets too large
        while (tabuList.size() > tabuSize) {
            tabuList.remove(0);
        }

        // Perform a few iterations of tabu search
        int iterations = 20;

        for (int i = 0; i < iterations; i++) {
            List<List<Route>> neighbors = generateTabuNeighbors(current);
            boolean foundImprovement = false;

            for (List<Route> neighbor : neighbors) {
                // Skip if the move is in the tabu list
                if (isTabu(neighbor)) {
                    continue;
                }

                double neighborFitness = calculateFitness(neighbor);

                if (neighborFitness < bestFitness) {
                    bestSolution = neighbor;
                    bestFitness = neighborFitness;
                    current = neighbor;
                    foundImprovement = true;

                    // Add the move to the tabu list
                    addToTabuList(neighbor);
                    break;
                }
            }

            if (!foundImprovement) {
                break;
            }
        }

        return bestSolution;
    }

    /**
     * Generate neighbors for tabu search by swapping customers between routes
     */
    private List<List<Route>> generateTabuNeighbors(List<Route> solution) {
        List<List<Route>> neighbors = new ArrayList<>();

        if (solution.size() < 2) {
            return neighbors;
        }

        for (int i = 0; i < solution.size(); i++) {
            for (int j = i + 1; j < solution.size(); j++) {
                Route route1 = solution.get(i);
                Route route2 = solution.get(j);

                List<Location> customers1 = getCustomers(route1.getLocations());
                List<Location> customers2 = getCustomers(route2.getLocations());

                if (customers1.isEmpty() || customers2.isEmpty()) {
                    continue;
                }

                // Try swapping each pair of customers
                for (Location customer1 : customers1) {
                    for (Location customer2 : customers2) {
                        Vehicle vehicle1 = route1.getVehicle();
                        Vehicle vehicle2 = route2.getVehicle();

                        double demand1 = route1.getTotalDemand() - customer1.getDemand() + customer2.getDemand();
                        double demand2 = route2.getTotalDemand() - customer2.getDemand() + customer1.getDemand();

                        if (demand1 <= vehicle1.getPayloadCapacity() && demand2 <= vehicle2.getPayloadCapacity()) {
                            // Create a new neighbor by swapping
                            List<Route> neighbor = deepCopy(solution);
                            Route newRoute1 = neighbor.get(i);
                            Route newRoute2 = neighbor.get(j);

                            List<Location> locations1 = new ArrayList<>(newRoute1.getLocations());
                            List<Location> locations2 = new ArrayList<>(newRoute2.getLocations());

                            int pos1 = locations1.indexOf(customer1);
                            int pos2 = locations2.indexOf(customer2);

                            locations1.set(pos1, customer2);
                            locations2.set(pos2, customer1);

                            newRoute1.setLocations(locations1);
                            newRoute2.setLocations(locations2);

                            neighbors.add(neighbor);
                        }
                    }
                }
            }
        }

        return neighbors;
    }

    /**
     * Check if a move is in the tabu list
     */
    private boolean isTabu(List<Route> solution) {
        List<Integer> routeSizes = solution.stream()
                .map(route -> route.getLocations().size())
                .collect(Collectors.toList());

        for (List<Integer> tabuMove : tabuList) {
            if (routeSizes.equals(tabuMove)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Add a move to the tabu list
     */
    private void addToTabuList(List<Route> solution) {
        List<Integer> routeSizes = solution.stream()
                .map(route -> route.getLocations().size())
                .collect(Collectors.toList());

        tabuList.add(routeSizes);

        if (tabuList.size() > tabuSize) {
            tabuList.remove(0); // Remove oldest entry
        }
    }

    /**
     * Apply 3-opt local search to improve the route
     */
    private List<Route> apply3OptLocalSearch(List<Route> solution) {
        List<Route> improved = deepCopy(solution);

        for (int routeIndex = 0; routeIndex < improved.size(); routeIndex++) {
            Route route = improved.get(routeIndex);
            List<Location> locations = route.getLocations();

            // We only need to optimize routes with at least 5 locations
            // (depot + 3 customers + depot)
            if (locations.size() < 5) {
                continue;
            }

            // Apply 3-opt to this route
            List<Location> optimizedLocations = apply3OptToRoute(locations);
            route.setLocations(optimizedLocations);
        }

        return improved;
    }

    /**
     * Apply 3-opt local search to a single route
     */
    private List<Location> apply3OptToRoute(List<Location> route) {
        List<Location> bestRoute = new ArrayList<>(route);
        double bestDistance = calculateRouteDistance(bestRoute);
        boolean improved = true;

        while (improved) {
            improved = false;

            // Consider all possible 3-opt moves
            for (int i = 1; i < route.size() - 3; i++) {
                for (int j = i + 1; j < route.size() - 2; j++) {
                    for (int k = j + 1; k < route.size() - 1; k++) {
                        // Generate all possible 3-opt configurations
                        List<List<Location>> candidates = generate3OptCandidates(bestRoute, i, j, k);

                        for (List<Location> candidate : candidates) {
                            double distance = calculateRouteDistance(candidate);

                            if (distance < bestDistance) {
                                bestRoute = candidate;
                                bestDistance = distance;
                                improved = true;
                            }
                        }
                    }
                }
            }
        }

        return bestRoute;
    }

    /**
     * Generate all possible 3-opt candidates by breaking and reconnecting segments
     */
    private List<List<Location>> generate3OptCandidates(List<Location> route, int i, int j, int k) {
        List<List<Location>> candidates = new ArrayList<>();

        // Break the route into 4 segments
        List<Location> segment1 = new ArrayList<>(route.subList(0, i));
        List<Location> segment2 = new ArrayList<>(route.subList(i, j));
        List<Location> segment3 = new ArrayList<>(route.subList(j, k));
        List<Location> segment4 = new ArrayList<>(route.subList(k, route.size()));

        // Create possible reconnections
        // Original: 1-2-3-4
        candidates.add(new ArrayList<>(route)); // Keep original

        // 1-3-2-4
        List<Location> candidate1 = new ArrayList<>();
        candidate1.addAll(segment1);
        candidate1.addAll(segment3);
        candidate1.addAll(segment2);
        candidate1.addAll(segment4);
        candidates.add(candidate1);

        // 1-3-2R-4
        List<Location> candidate2 = new ArrayList<>();
        candidate2.addAll(segment1);
        candidate2.addAll(segment3);
        List<Location> segment2R = new ArrayList<>(segment2);
        Collections.reverse(segment2R);
        candidate2.addAll(segment2R);
        candidate2.addAll(segment4);
        candidates.add(candidate2);

        // 1-2R-3-4
        List<Location> candidate3 = new ArrayList<>();
        candidate3.addAll(segment1);
        candidate3.addAll(segment2R);
        candidate3.addAll(segment3);
        candidate3.addAll(segment4);
        candidates.add(candidate3);

        // 1-2-3R-4
        List<Location> candidate4 = new ArrayList<>();
        candidate4.addAll(segment1);
        candidate4.addAll(segment2);
        List<Location> segment3R = new ArrayList<>(segment3);
        Collections.reverse(segment3R);
        candidate4.addAll(segment3R);
        candidate4.addAll(segment4);
        candidates.add(candidate4);

        return candidates;
    }

    /**
     * Calculate distance of a route
     */
    private double calculateRouteDistance(List<Location> route) {
        double distance = 0;

        for (int i = 0; i < route.size() - 1; i++) {
            Location current = route.get(i);
            Location next = route.get(i + 1);

            double dx = current.getX() - next.getX();
            double dy = current.getY() - next.getY();
            distance += Math.sqrt(dx * dx + dy * dy);
        }

        return distance;
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