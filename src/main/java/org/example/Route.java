package org.example;

import org.example.models.*;

import java.util.ArrayList;
import java.util.List;

public class Route {
    private Vehicle vehicle;
    private List<Location> locations;
    private List<WeatherCondition> weatherConditions;
    private List<RoadEvent> roadEvents;
    private List<TrafficCongestion> trafficCongestions;
    private List<Location> refuelingStations;

    // Cache for computed values to improve performance
    private double totalDistance = -1;
    private double totalTravelTime = -1;
    private double totalCost = -1;
    private double totalDemand = -1;
    private int timeWindowViolations = -1;
    private int capacityViolations = -1;
    private int fuelViolations = -1;
    private List<Location> usedRefuelingStations = new ArrayList<>();
    private List<Double> fuelLevels = new ArrayList<>(); // Track fuel at each location

    /**
     * Constructor with all parameters
     */
    public Route(Vehicle vehicle, List<Location> locations,
                 List<WeatherCondition> weatherConditions,
                 List<RoadEvent> roadEvents,
                 List<TrafficCongestion> trafficCongestions,
                 List<Location> refuelingStations) {
        this.vehicle = vehicle;
        this.locations = new ArrayList<>(locations);
        this.weatherConditions = weatherConditions;
        this.roadEvents = roadEvents;
        this.trafficCongestions = trafficCongestions;
        this.refuelingStations = refuelingStations;
    }

    /**
     * Calculate total distance of the route including penalties for obstacles and weather
     */
    public double getTotalDistance() {
        if (totalDistance >= 0) {
            return totalDistance;
        }

        totalDistance = 0;

        for (int i = 0; i < locations.size() - 1; i++) {
            Location current = locations.get(i);
            Location next = locations.get(i + 1);

            // Basic distance
            double distance = calculateDistance(current, next);

            // Add traffic congestion penalties
            for (TrafficCongestion congestion : trafficCongestions) {
                if (intersectsEdge(current, next, congestion.getXBegin(), congestion.getYBegin(),
                        congestion.getXEnd(), congestion.getYEnd())) {
                    // Convert delay from minutes to distance equivalent
                    double delayDistanceEquivalent = congestion.getDelay() * (vehicle.getSpeed() / 60.0);
                    distance += delayDistanceEquivalent;
                }
            }

            // Add road event penalties
            for (RoadEvent event : roadEvents) {
                if (intersectsEdge(current, next, event.getXBegin(), event.getYBegin(),
                        event.getXEnd(), event.getYEnd())) {
                    // Convert delay from minutes to distance equivalent
                    double delayDistanceEquivalent = event.getDelay() * (vehicle.getSpeed() / 60.0);
                    distance += delayDistanceEquivalent;
                }
            }

            // Add weather condition penalties
            for (WeatherCondition weather : weatherConditions) {
                if (intersectsEdge(current, next, weather.getXBegin(), weather.getYBegin(),
                        weather.getXEnd(), weather.getYEnd())) {
                    // Convert delay from minutes to distance equivalent
                    double delayDistanceEquivalent = weather.getDelay() * (vehicle.getSpeed() / 60.0);
                    distance += delayDistanceEquivalent;
                }
            }

            totalDistance += distance;
        }

        return totalDistance;
    }

    /**
     * Calculate total travel time including waiting and service times
     * Modified to track fuel consumption and refueling
     */
    public double getTotalTravelTime() {
        if (totalTravelTime >= 0) {
            return totalTravelTime;
        }

        totalTravelTime = 0;
        double currentTime = 0;
        double currentFuel = vehicle.getMaxFuelTank(); // Start with full tank
        fuelLevels.clear();
        fuelLevels.add(currentFuel); // Initial fuel level

        // Reset violation counts
        timeWindowViolations = 0;
        fuelViolations = 0;
        usedRefuelingStations.clear();

        for (int i = 0; i < locations.size() - 1; i++) {
            Location current = locations.get(i);
            Location next = locations.get(i + 1);

            // Calculate basic distance
            double distance = calculateDistance(current, next);
            double travelTime = distance / vehicle.getSpeed();

            // Calculate additional delays from traffic, road events, and weather
            double additionalDelay = 0;

            // Traffic congestion delay
            for (TrafficCongestion congestion : trafficCongestions) {
                if (intersectsEdge(current, next, congestion.getXBegin(), congestion.getYBegin(),
                        congestion.getXEnd(), congestion.getYEnd())) {
                    additionalDelay += congestion.getDelay();
                }
            }

            // Road event delay
            for (RoadEvent event : roadEvents) {
                if (intersectsEdge(current, next, event.getXBegin(), event.getYBegin(),
                        event.getXEnd(), event.getYEnd())) {
                    additionalDelay += event.getDelay();
                }
            }

            // Weather condition delay
            for (WeatherCondition weather : weatherConditions) {
                if (intersectsEdge(current, next, weather.getXBegin(), weather.getYBegin(),
                        weather.getXEnd(), weather.getYEnd())) {
                    additionalDelay += weather.getDelay();
                }
            }

            // Add delays to travel time (convert minutes to hours)
            travelTime += additionalDelay / 60.0;

            // Calculate energy consumption for this segment
            double payload = getTotalDemand(); // Use the total demand as current payload
            double energyConsumption = calculateEnergyConsumption(distance, payload);

            // Subtract energy consumption from current fuel
            currentFuel -= energyConsumption;

            // Check if fuel would go below minimum
            if (currentFuel < vehicle.getMinFuelTank()) {
                // Fuel violation occurred
                fuelViolations++;

                // If current location is not a refueling station, calculate penalty
                if (!current.getType().equalsIgnoreCase("refueling_station")) {
                    totalTravelTime += 1000; // High penalty for fuel violation
                }

                // If we're at a refueling station, refuel
                if (current.getType().equalsIgnoreCase("refueling_station")) {
                    currentFuel = vehicle.getMaxFuelTank();
                    totalTravelTime += vehicle.getRefuelingTime();
                    usedRefuelingStations.add(current);
                }
            }

            // Add travel time
            currentTime += travelTime;
            totalTravelTime += travelTime;

            // Store fuel level at next location
            fuelLevels.add(currentFuel);

            // Check time windows for next location
            if (!next.getType().equalsIgnoreCase("depot") &&
                    !next.getType().equalsIgnoreCase("refueling_station")) {
                double[] timeWindow = next.getTimeWindow();

                // If too early, wait
                if (currentTime < timeWindow[0]) {
                    double waitTime = timeWindow[0] - currentTime;
                    currentTime = timeWindow[0];
                    totalTravelTime += waitTime;
                }

                // If too late, add violation
                if (currentTime > timeWindow[1]) {
                    timeWindowViolations++;
                    totalTravelTime += (currentTime - timeWindow[1]) * 2; // Penalty for being late
                }

                // Add service time
                currentTime += next.getServiceTime();
                totalTravelTime += next.getServiceTime();
            }

            // If next is a refueling station, add refueling time and mark as used
            if (next.getType().equalsIgnoreCase("refueling_station")) {
                totalTravelTime += vehicle.getRefuelingTime();
                currentFuel = vehicle.getMaxFuelTank(); // Fully refuel
                usedRefuelingStations.add(next);
            }
        }

        return totalTravelTime;
    }

    /**
     * Calculate energy consumption for a route segment
     * @param distance Distance traveled
     * @param payload Current payload weight
     * @return Energy consumed
     */
    private double calculateEnergyConsumption(double distance, double payload) {
        // Base energy consumption plus payload-dependent consumption
        return distance * (vehicle.getBaseEnergyConsumption() +
                payload * vehicle.getPayloadEnergyCoefficient());
    }

    /**
     * Calculate total cost of the route, combining distance, time, and refueling costs
     * Modified to include fuel violation penalties
     */
    public double getTotalCost() {
        if (totalCost >= 0) {
            return totalCost;
        }

        // Recompute cached values if needed
        double distance = getTotalDistance();
        double travelTime = getTotalTravelTime();

        // Calculate refueling costs
        double refuelingCost = usedRefuelingStations.size() * vehicle.getRefuelingCost();

        // Calculate violation penalties
        double timeWindowPenalty = getTimeWindowViolations() * 500;
        double capacityPenalty = getCapacityViolations() * 1000;
        double fuelPenalty = getFuelViolations() * 2000; // Higher penalty for fuel violations

        // Combine all costs
        totalCost = distance + travelTime + refuelingCost + timeWindowPenalty + capacityPenalty + fuelPenalty;

        return totalCost;
    }

    /**
     * Get total demand for this route
     */
    public double getTotalDemand() {
        if (totalDemand >= 0) {
            return totalDemand;
        }

        totalDemand = 0;
        capacityViolations = 0;

        for (Location location : locations) {
            if (!location.getType().equalsIgnoreCase("depot") &&
                    !location.getType().equalsIgnoreCase("refueling_station")) {
                totalDemand += location.getDemand();
            }
        }

        // Check for capacity violations
        if (totalDemand > vehicle.getPayloadCapacity()) {
            capacityViolations = 1;
        }

        return totalDemand;
    }

    /**
     * Get number of time window violations in this route
     */
    public int getTimeWindowViolations() {
        if (timeWindowViolations < 0) {
            // Force recalculation
            getTotalTravelTime();
        }
        return timeWindowViolations;
    }

    /**
     * Get number of capacity violations in this route
     */
    public int getCapacityViolations() {
        if (capacityViolations < 0) {
            // Force recalculation
            getTotalDemand();
        }
        return capacityViolations;
    }

    /**
     * Get number of fuel violations in this route
     */
    public int getFuelViolations() {
        if (fuelViolations < 0) {
            // Force recalculation
            getTotalTravelTime();
        }
        return fuelViolations;
    }

    /**
     * Get refueling stations used in this route
     */
    public List<Location> getUsedRefuelingStations() {
        if (totalTravelTime < 0) {
            // Force recalculation
            getTotalTravelTime();
        }
        return usedRefuelingStations;
    }

    /**
     * Get fuel levels at each location
     */
    public List<Double> getFuelLevels() {
        if (fuelLevels.isEmpty()) {
            // Force recalculation
            getTotalTravelTime();
        }
        return fuelLevels;
    }

    /**
     * Calculate Euclidean distance between two locations
     */
    private double calculateDistance(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Check if a route edge intersects with another edge
     */
    private boolean intersectsEdge(Location a, Location b, double x1, double y1, double x2, double y2) {
        // Simple bounding box check
        double minX1 = Math.min(a.getX(), b.getX());
        double maxX1 = Math.max(a.getX(), b.getX());
        double minY1 = Math.min(a.getY(), b.getY());
        double maxY1 = Math.max(a.getY(), b.getY());

        double minX2 = Math.min(x1, x2);
        double maxX2 = Math.max(x1, x2);
        double minY2 = Math.min(y1, y2);
        double maxY2 = Math.max(y1, y2);

        // Check if bounding boxes overlap
        return (minX1 <= maxX2 && maxX1 >= minX2 && minY1 <= maxY2 && maxY1 >= minY2);
    }

    /**
     * Find nearest refueling station when fuel is low
     */
    private Location findNearestRefuelingStation(Location current) {
        double minDistance = Double.MAX_VALUE;
        Location nearest = null;

        for (Location station : refuelingStations) {
            double distance = calculateDistance(current, station);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = station;
            }
        }

        return nearest;
    }

    // Getters and setters
    public Vehicle getVehicle() {
        return vehicle;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public List<Location> getRefuelingStations() {
        return refuelingStations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = new ArrayList<>(locations);

        // Reset cached values
        totalDistance = -1;
        totalTravelTime = -1;
        totalCost = -1;
        totalDemand = -1;
        timeWindowViolations = -1;
        capacityViolations = -1;
        fuelViolations = -1;
        usedRefuelingStations.clear();
        fuelLevels.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Route{vehicle=").append(vehicle.getId()).append(", locations=[");

        for (Location loc : locations) {
            sb.append(loc.getId()).append("(").append(loc.getType()).append("), ");
        }

        if (!locations.isEmpty()) {
            sb.setLength(sb.length() - 2); // Remove trailing comma and space
        }

        sb.append("], cost=").append(getTotalCost())
                .append(", distance=").append(getTotalDistance())
                .append(", demand=").append(getTotalDemand())
                .append(", fuelViolations=").append(getFuelViolations())
                .append("}");

        return sb.toString();
    }
}