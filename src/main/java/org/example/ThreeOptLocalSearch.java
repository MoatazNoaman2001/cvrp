package org.example;

import org.example.models.*;

import java.util.*;

/**
 * ThreeOptLocalSearch class implements the 3-opt local search algorithm
 * for improving routes by reconnecting route segments.
 */
public class ThreeOptLocalSearch {
    private final List<WeatherCondition> weatherConditions;
    private final List<RoadEvent> roadEvents;
    private final List<TrafficCongestion> trafficCongestions;
    private final List<Location> refuelingStations;

    public ThreeOptLocalSearch(
            List<WeatherCondition> weatherConditions,
            List<RoadEvent> roadEvents,
            List<TrafficCongestion> trafficCongestions,
            List<Location> refuelingStations) {
        this.weatherConditions = weatherConditions;
        this.roadEvents = roadEvents;
        this.trafficCongestions = trafficCongestions;
        this.refuelingStations = refuelingStations;
    }

    /**
     * Apply 3-opt local search to improve the route
     */
    public List<Route> apply3OptLocalSearch(List<Route> solution) {
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