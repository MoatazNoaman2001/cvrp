package org.example;

import org.example.models.*;

import java.util.*;
import java.util.stream.Collectors;


public class TabuSearch {
    private final List<WeatherCondition> weatherConditions;
    private final List<RoadEvent> roadEvents;
    private final List<TrafficCongestion> trafficCongestions;
    private final List<Location> refuelingStations;

    private final int tabuSize;
    private final List<List<Integer>> tabuList = new ArrayList<>();
    private final int maxIterations;

    public TabuSearch(List<WeatherCondition> weatherConditions,
                      List<RoadEvent> roadEvents,
                      List<TrafficCongestion> trafficCongestions,
                      List<Location> refuelingStations,
                      int tabuSize,
                      int maxIterations) {
        this.weatherConditions = weatherConditions;
        this.roadEvents = roadEvents;
        this.trafficCongestions = trafficCongestions;
        this.refuelingStations = refuelingStations;
        this.tabuSize = tabuSize;
        this.maxIterations = maxIterations;
    }

    /**
     * Apply tabu search to improve the solution
     */
    public List<Route> applyTabuSearch(List<Route> individual) {
        List<Route> current = deepCopy(individual);
        List<Route> bestSolution = deepCopy(individual);
        double bestFitness = calculateFitness(bestSolution);

        // Clear tabu list if it gets too large
        while (tabuList.size() > tabuSize) {
            tabuList.remove(0);
        }

        // Perform iterations of tabu search
        for (int i = 0; i < maxIterations; i++) {
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
     * Calculate fitness (total cost) of a solution
     */
    private double calculateFitness(List<Route> solution) {
        return solution.stream()
                .mapToDouble(Route::getTotalCost)
                .sum();
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
     * Get TabuSize
     */
    public int getTabuSize() {
        return tabuSize;
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