package org.example;

import org.example.models.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class IoTIntegration {
    private Map<String, Double> trafficSpeedData = new HashMap<>();
    private Map<String, String> roadEventData = new HashMap<>();
    private Map<String, String> weatherConditionData = new HashMap<>();
    private Map<Integer, VehicleIoTStatus> vehicleStatusData = new HashMap<>();

    private Random random = new Random();
    private List<String> weatherTypes = List.of("Clear", "Light Rain", "Heavy Rain", "Fog", "Snow", "Storm");
    private List<String> roadEventTypes = List.of("Clear", "Accident", "Construction", "Roadblock");

    /**
     * Initialize IoT data for the given locations and vehicles
     */
    public void initialize(List<Location> locations, List<Vehicle> vehicles) {
        System.out.println("Initializing IoT data integration...");

        // Create edges between all locations
        for (int i = 0; i < locations.size(); i++) {
            for (int j = i + 1; j < locations.size(); j++) {
                Location loc1 = locations.get(i);
                Location loc2 = locations.get(j);
                String edgeKey = getEdgeKey(loc1, loc2);

                // Initial traffic speed (40-80 km/h)
                trafficSpeedData.put(edgeKey, 40.0 + random.nextDouble() * 40.0);

                // Initial road event (mostly clear)
                roadEventData.put(edgeKey, random.nextDouble() < 0.8 ? "Clear" :
                        roadEventTypes.get(random.nextInt(roadEventTypes.size())));

                // Initial weather condition (mostly clear)
                weatherConditionData.put(edgeKey, random.nextDouble() < 0.7 ? "Clear" :
                        weatherTypes.get(random.nextInt(weatherTypes.size())));
            }
        }

        for (Vehicle vehicle : vehicles) {
            vehicleStatusData.put(vehicle.getId(), new VehicleIoTStatus(vehicle.getId()));
        }

        System.out.println("IoT data initialized: " + trafficSpeedData.size() + " edges monitored");
    }

    /**
     * Update IoT data to simulate real-time changes
     */
    public void updateIoTData() {
        // Update traffic speeds (small changes)
        for (String edge : trafficSpeedData.keySet()) {
            double currentSpeed = trafficSpeedData.get(edge);
            double change = (random.nextDouble() - 0.5) * 10.0; // -5 to +5 km/h change
            double newSpeed = Math.max(5.0, Math.min(120.0, currentSpeed + change));
            trafficSpeedData.put(edge, newSpeed);
        }

        // Update road events (occasional changes)
        for (String edge : roadEventData.keySet()) {
            if (random.nextDouble() < 0.02) { // 2% chance of event change
                roadEventData.put(edge, roadEventTypes.get(random.nextInt(roadEventTypes.size())));
            }
        }

        // Update weather conditions (occasional changes)
        for (String edge : weatherConditionData.keySet()) {
            if (random.nextDouble() < 0.01) { // 1% chance of weather change
                weatherConditionData.put(edge, weatherTypes.get(random.nextInt(weatherTypes.size())));
            }
        }

        // Update vehicle status
        for (VehicleIoTStatus status : vehicleStatusData.values()) {
            status.updateRandomly();
        }
    }

    /**
     * Get traffic speed for a road segment between two locations
     */
    public double getTrafficSpeed(Location loc1, Location loc2) {
        String edgeKey = getEdgeKey(loc1, loc2);
        return trafficSpeedData.getOrDefault(edgeKey, 60.0); // Default: 60 km/h
    }

    /**
     * Get road event for a road segment between two locations
     */
    public String getRoadEvent(Location loc1, Location loc2) {
        String edgeKey = getEdgeKey(loc1, loc2);
        return roadEventData.getOrDefault(edgeKey, "Clear"); // Default: Clear
    }

    /**
     * Get weather condition for a road segment between two locations
     */
    public String getWeatherCondition(Location loc1, Location loc2) {
        String edgeKey = getEdgeKey(loc1, loc2);
        return weatherConditionData.getOrDefault(edgeKey, "Clear"); // Default: Clear
    }

    /**
     * Get current vehicle status
     */
    public VehicleIoTStatus getVehicleStatus(int vehicleId) {
        return vehicleStatusData.getOrDefault(vehicleId,
                new VehicleIoTStatus(vehicleId));
    }

    /**
     * Generate traffic congestion objects from current IoT data
     */
    public List<TrafficCongestion> generateTrafficCongestions(List<Location> locations) {
        List<TrafficCongestion> congestions = new ArrayList<>();

        for (int i = 0; i < locations.size(); i++) {
            for (int j = i + 1; j < locations.size(); j++) {
                Location loc1 = locations.get(i);
                Location loc2 = locations.get(j);
                String edgeKey = getEdgeKey(loc1, loc2);

                double speed = trafficSpeedData.getOrDefault(edgeKey, 60.0);

                // Only add congestion if speed is below free flow
                if (speed < 60.0) {
                    TrafficCongestion congestion = new TrafficCongestion(
                            loc1.getX(), loc1.getY(), loc2.getX(), loc2.getY(), speed
                    );
                    congestions.add(congestion);
                }
            }
        }

        return congestions;
    }

    /**
     * Generate road event objects from current IoT data
     */
    public List<RoadEvent> generateRoadEvents(List<Location> locations) {
        List<RoadEvent> events = new ArrayList<>();

        for (int i = 0; i < locations.size(); i++) {
            for (int j = i + 1; j < locations.size(); j++) {
                Location loc1 = locations.get(i);
                Location loc2 = locations.get(j);
                String edgeKey = getEdgeKey(loc1, loc2);

                String eventType = roadEventData.getOrDefault(edgeKey, "Clear");

                // Only add event if it's not clear
                if (!eventType.equals("Clear")) {
                    RoadEvent event = new RoadEvent(
                            loc1.getX(), loc1.getY(), loc2.getX(), loc2.getY(), eventType
                    );
                    events.add(event);
                }
            }
        }

        return events;
    }

    /**
     * Generate weather condition objects from current IoT data
     */
    public List<WeatherCondition> generateWeatherConditions(List<Location> locations) {
        List<WeatherCondition> conditions = new ArrayList<>();

        for (int i = 0; i < locations.size(); i++) {
            for (int j = i + 1; j < locations.size(); j++) {
                Location loc1 = locations.get(i);
                Location loc2 = locations.get(j);
                String edgeKey = getEdgeKey(loc1, loc2);

                String weatherType = weatherConditionData.getOrDefault(edgeKey, "Clear");

                // Only add condition if it's not clear
                if (!weatherType.equals("Clear")) {
                    WeatherCondition condition = new WeatherCondition(
                            loc1.getX(), loc1.getY(), loc2.getX(), loc2.getY(), weatherType
                    );
                    conditions.add(condition);
                }
            }
        }

        return conditions;
    }

    /**
     * Create a unique key for an edge between two locations
     */
    private String getEdgeKey(Location loc1, Location loc2) {
        // Ensure the lower ID is always first for consistent keys
        if (loc1.getId() < loc2.getId()) {
            return loc1.getId() + "-" + loc2.getId();
        } else {
            return loc2.getId() + "-" + loc1.getId();
        }
    }

    /**
     * Inner class for vehicle IoT status
     */
    public static class VehicleIoTStatus {
        private int vehicleId;
        private double fuelLevel;
        private double currentSpeed;
        private double currentTemperature;
        private double currentPressure;
        private double latitude;
        private double longitude;
        private double engineTemperature;
        private double tireStatus;  // percentage
        private boolean doorStatus; // true if door open
        private Random random = new Random();

        public VehicleIoTStatus(int vehicleId) {
            this.vehicleId = vehicleId;
            this.fuelLevel = 100.0;
            this.currentSpeed = 0.0;
            this.currentTemperature = 20.0;
            this.currentPressure = 101.3;
            this.latitude = 0.0;
            this.longitude = 0.0;
            this.engineTemperature = 80.0;
            this.tireStatus = 100.0;
            this.doorStatus = false;
        }

        /**
         * Update status with random changes to simulate IoT data
         */
        public void updateRandomly() {
            // Small random changes
            fuelLevel = Math.max(0.0, Math.min(100.0, fuelLevel - random.nextDouble() * 0.5));
            currentSpeed = Math.max(0.0, Math.min(120.0, currentSpeed + (random.nextDouble() - 0.5) * 10.0));
            currentTemperature = Math.max(-10.0, Math.min(40.0, currentTemperature + (random.nextDouble() - 0.5) * 0.5));
            currentPressure = Math.max(98.0, Math.min(103.0, currentPressure + (random.nextDouble() - 0.5) * 0.1));
            engineTemperature = Math.max(60.0, Math.min(110.0, engineTemperature + (random.nextDouble() - 0.5) * 2.0));
            tireStatus = Math.max(50.0, Math.min(100.0, tireStatus - random.nextDouble() * 0.01));

            // Occasionally toggle door status
            if (random.nextDouble() < 0.01) {
                doorStatus = !doorStatus;
            }
        }

        // Getters
        public int getVehicleId() { return vehicleId; }
        public double getFuelLevel() { return fuelLevel; }
        public double getCurrentSpeed() { return currentSpeed; }
        public double getCurrentTemperature() { return currentTemperature; }
        public double getCurrentPressure() { return currentPressure; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public double getEngineTemperature() { return engineTemperature; }
        public double getTireStatus() { return tireStatus; }
        public boolean isDoorOpen() { return doorStatus; }

        public void setPosition(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public void setFuelLevel(double fuelLevel) {
            this.fuelLevel = Math.max(0.0, Math.min(100.0, fuelLevel));
        }

        @Override
        public String toString() {
            return "VehicleStatus{" +
                    "vehicleId=" + vehicleId +
                    ", fuelLevel=" + String.format("%.1f", fuelLevel) + "%" +
                    ", currentSpeed=" + String.format("%.1f", currentSpeed) + " km/h" +
                    ", temperature=" + String.format("%.1f", currentTemperature) + "°C" +
                    ", engineTemp=" + String.format("%.1f", engineTemperature) + "°C" +
                    ", tireStatus=" + String.format("%.1f", tireStatus) + "%" +
                    ", doorOpen=" + doorStatus +
                    '}';
        }
    }
}