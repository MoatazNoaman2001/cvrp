package org.example;

import org.example.models.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CVRPDriver {
    static List<WeatherCondition> weatherConditions = new ArrayList<>();
    static List<RoadEvent> roadEvents = new ArrayList<>();
    static List<TrafficCongestion> trafficCongestions = new ArrayList<>();

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        try {
            System.out.println("Loading data...");

            validateFilePath("C:\\Users\\moata\\IdeaProjects\\cvrp\\src\\main\\java\\org\\example\\resource\\location.txt");
            validateFilePath("C:\\Users\\moata\\IdeaProjects\\cvrp\\src\\main\\java\\org\\example\\resource\\vehicles.txt");
            validateFilePath("C:\\Users\\moata\\IdeaProjects\\cvrp\\src\\main\\java\\org\\example\\resource\\weather_conditions.txt");
            validateFilePath("C:\\Users\\moata\\IdeaProjects\\cvrp\\src\\main\\java\\org\\example\\resource\\road_events.txt");
            validateFilePath("C:\\Users\\moata\\IdeaProjects\\cvrp\\src\\main\\java\\org\\example\\resource\\traffic_congestions.txt");

            List<Location> locations = loadLocations("C:\\Users\\moata\\IdeaProjects\\cvrp\\src\\main\\java\\org\\example\\resource\\location.txt");
            List<Vehicle> vehicles = loadVehicles("C:\\Users\\moata\\IdeaProjects\\cvrp\\src\\main\\java\\org\\example\\resource\\vehicles.txt");
            weatherConditions = loadWeatherConditions("C:\\Users\\moata\\IdeaProjects\\cvrp\\src\\main\\java\\org\\example\\resource\\weather_conditions.txt");
            roadEvents = loadRoadEvents("C:\\Users\\moata\\IdeaProjects\\cvrp\\src\\main\\java\\org\\example\\resource\\road_events.txt");
            trafficCongestions = loadTrafficCongestions("C:\\Users\\moata\\IdeaProjects\\cvrp\\src\\main\\java\\org\\example\\resource\\traffic_congestions.txt");

            System.out.println("Data loaded successfully in " + (System.currentTimeMillis() - startTime) + "ms");
            printInputSummary(locations, vehicles, weatherConditions, roadEvents, trafficCongestions);

            System.out.println("\n==== Running CVRP Hybrid Algorithm with Enhanced Timeout Protection ====\n");

            final int TIMEOUT_SECONDS = 180;
            ExecutorService executor = Executors.newSingleThreadExecutor();
            final AtomicBoolean algorithmRunning = new AtomicBoolean(true);

            final CVRPHybridAlgorithm algorithm = new CVRPHybridAlgorithm(
                    vehicles, locations, weatherConditions, roadEvents, trafficCongestions,
                    TIMEOUT_SECONDS * 1000);

            Thread watchdog = new Thread(() -> {
                try {
                    Thread.sleep(TIMEOUT_SECONDS * 1000);
                    if (algorithmRunning.get()) {
                        System.out.println("Watchdog timeout reached. Attempting to terminate algorithm.");
                        algorithm.terminate();
                        Thread.sleep(5000);

                        if (algorithmRunning.get()) {
                            System.out.println("Algorithm didn't terminate gracefully. Forcing shutdown.");
                            executor.shutdownNow(); // Force shutdown if it doesn't respond
                        }
                    }
                } catch (InterruptedException e) {
                }
            });
            watchdog.setDaemon(true);
            watchdog.start();

            Future<List<Route>> future = executor.submit(algorithm::findOptimalRoutes);

            List<Route> optimalRoutes;
            try {
                optimalRoutes = future.get(TIMEOUT_SECONDS + 10, TimeUnit.SECONDS); // Add 10 seconds grace period
                algorithmRunning.set(false);
                watchdog.interrupt();
                System.out.println("\n==== Results ====\n");

                if (optimalRoutes == null || optimalRoutes.isEmpty()) {
                    System.out.println("No feasible routes found.");
                    return;
                }

                System.out.println("Routes found: " + optimalRoutes.size());
                System.out.println("Total Distance: " + optimalRoutes.stream()
                        .map(Route::getTotalDistance)
                        .reduce(Double::sum).orElse(0.0));

                double totalCost = 0.0;
                int totalRefuelingStationsUsed = 0;

                for (int i = 0; i < optimalRoutes.size(); i++) {
                    Route route = optimalRoutes.get(i);
                    totalCost += route.getTotalCost();
                    totalRefuelingStationsUsed += countRefuelingStations(route.getLocations());

                    System.out.println("\nRoute #" + (i+1) + " (Vehicle #" + route.getVehicle().getId() + "):");
                    printRouteDetails(route);
                }

                System.out.println("\nTotal cost: " + totalCost);
                System.out.println("Total refueling stations used: " + totalRefuelingStationsUsed);

                long totalRuntime = (System.currentTimeMillis() - startTime) / 1000;
                System.out.println("Total runtime: " + totalRuntime + " seconds");

            } catch (TimeoutException e) {
                algorithmRunning.set(false);
                watchdog.interrupt();
                System.err.println("ERROR: Algorithm timed out after " + TIMEOUT_SECONDS + " seconds.");
                System.err.println("This typically happens when the problem is too complex or constraints are difficult to satisfy.");
                System.err.println("Consider simplifying the problem or adjusting algorithm parameters.");
                future.cancel(true);
            } catch (Exception e) {
                algorithmRunning.set(false);
                watchdog.interrupt();
                System.err.println("Error executing algorithm: " + e.getMessage());
                e.printStackTrace();
            } finally {
                executor.shutdownNow();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        System.err.println("Executor did not terminate in the expected time.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static void validateFilePath(String path) throws FileNotFoundException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + path);
        }
        if (!file.canRead()) {
            throw new FileNotFoundException("Cannot read file: " + path);
        }
        System.out.println("File validated: " + path);
    }

    private static int countRefuelingStations(List<Location> locations) {
        int count = 0;
        for (Location loc : locations) {
            if (loc.getType().equalsIgnoreCase("refueling_station")) {
                count++;
            }
        }
        return count;
    }

    public static List<Location> loadLocations(String filename) throws FileNotFoundException {
        List<Location> locations = new ArrayList<>();
        Scanner scanner = new Scanner(new File(filename));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("#") || line.isEmpty()) continue;

            String[] parts = line.split(",");
            int id = Integer.parseInt(parts[0]);
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double demand = Double.parseDouble(parts[3]);
            double et = Double.parseDouble(parts[4]);
            double lt = Double.parseDouble(parts[5]);
            double serviceTime = Double.parseDouble(parts[6]);
            String type = parts[7];

            locations.add(new Location(id, x, y, demand, new double[]{et, lt}, serviceTime, type));
        }

        scanner.close();
        return locations;
    }

    public static List<Vehicle> loadVehicles(String filename) throws FileNotFoundException {
        List<Vehicle> vehicles = new ArrayList<>();
        Scanner scanner = new Scanner(new File(filename));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("#") || line.isEmpty()) continue;

            String[] parts = line.split(",");
            int id = Integer.parseInt(parts[0]);
            double speed = Double.parseDouble(parts[1]);
            double maxFuelTank = Double.parseDouble(parts[2]);
            double minFuelTank = Double.parseDouble(parts[3]);
            double payloadCapacity = Double.parseDouble(parts[4]);
            double baseEnergyConsumption = Double.parseDouble(parts[5]);
            double payloadEnergyCoefficient = Double.parseDouble(parts[6]);
            double refuelingTime = Double.parseDouble(parts[7]);
            double refuelingCost = Double.parseDouble(parts[8]);

            vehicles.add(new Vehicle(id, speed, maxFuelTank, minFuelTank, payloadCapacity,
                    baseEnergyConsumption, payloadEnergyCoefficient,
                    refuelingTime, refuelingCost));
        }

        scanner.close();
        return vehicles;
    }

    public static List<WeatherCondition> loadWeatherConditions(String filename) throws FileNotFoundException {
        List<WeatherCondition> conditions = new ArrayList<>();
        Scanner scanner = new Scanner(new File(filename));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("#") || line.isEmpty()) continue;

            String[] parts = line.split(",");
            double xBegin = Double.parseDouble(parts[0]);
            double yBegin = Double.parseDouble(parts[1]);
            double xEnd = Double.parseDouble(parts[2]);
            double yEnd = Double.parseDouble(parts[3]);
            String weatherType = parts[4];

            conditions.add(new WeatherCondition(xBegin, yBegin, xEnd, yEnd, weatherType));
        }

        scanner.close();
        return conditions;
    }

    public static List<RoadEvent> loadRoadEvents(String filename) throws FileNotFoundException {
        List<RoadEvent> events = new ArrayList<>();
        Scanner scanner = new Scanner(new File(filename));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("#") || line.isEmpty()) continue;

            String[] parts = line.split(",");
            double xBegin = Double.parseDouble(parts[0]);
            double yBegin = Double.parseDouble(parts[1]);
            double xEnd = Double.parseDouble(parts[2]);
            double yEnd = Double.parseDouble(parts[3]);
            String eventType = parts[4];

            events.add(new RoadEvent(xBegin, yBegin, xEnd, yEnd, eventType));
        }

        scanner.close();
        return events;
    }

    public static List<TrafficCongestion> loadTrafficCongestions(String filename) throws FileNotFoundException {
        List<TrafficCongestion> congestions = new ArrayList<>();
        Scanner scanner = new Scanner(new File(filename));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("#") || line.isEmpty()) continue;

            String[] parts = line.split(",");
            double xBegin = Double.parseDouble(parts[0]);
            double yBegin = Double.parseDouble(parts[1]);
            double xEnd = Double.parseDouble(parts[2]);
            double yEnd = Double.parseDouble(parts[3]);
            double trafficSpeed = Double.parseDouble(parts[4]);

            congestions.add(new TrafficCongestion(xBegin, yBegin, xEnd, yEnd, trafficSpeed));
        }

        scanner.close();
        return congestions;
    }

    private static void printInputSummary(List<Location> locations, List<Vehicle> vehicles,
                                          List<WeatherCondition> weatherConditions,
                                          List<RoadEvent> roadEvents,
                                          List<TrafficCongestion> trafficCongestions) {
        System.out.println("=== Input Summary ===");

        int depotCount = 0, customerCount = 0, stationCount = 0;

        for (Location loc : locations) {
            if (loc.getType().equalsIgnoreCase("depot")) depotCount++;
            else if (loc.getType().equalsIgnoreCase("refueling_station")) stationCount++;
            else customerCount++;
        }

        System.out.println("Locations: " + locations.size());
        System.out.println("  - Depots: " + depotCount);
        System.out.println("  - Customers: " + customerCount);
        System.out.println("  - Refueling Stations: " + stationCount);

        System.out.println("Vehicles: " + vehicles.size());
        for (Vehicle vehicle : vehicles) {
            System.out.printf("  - Vehicle #%d: Speed=%.1f, Capacity=%.1f, Fuel Tank=%.1f\n",
                    vehicle.getId(), vehicle.getSpeed(), vehicle.getPayloadCapacity(), vehicle.getMaxFuelTank());
        }

        System.out.println("Weather Conditions: " + weatherConditions.size());
        System.out.println("Road Events: " + roadEvents.size());
        System.out.println("Traffic Congestions: " + trafficCongestions.size());
    }

    private static void printRouteDetails(Route route) {
        System.out.println("  - Locations: " + formatLocationSequence(route.getLocations()));
        System.out.printf("  - Cost: %.2f\n", route.getTotalCost());
        System.out.printf("  - Distance: %.2f\n", route.getTotalDistance());
        System.out.printf("  - Time: %.2f\n", route.getTotalTravelTime());
        System.out.printf("  - Payload: %.2f / %.2f\n", route.getTotalDemand(), route.getVehicle().getPayloadCapacity());

        // Print fuel information
        List<Double> fuelLevels = route.getFuelLevels();
        System.out.println("  - Fuel: Initial=" + route.getVehicle().getMaxFuelTank() +
                ", Minimum=" + route.getVehicle().getMinFuelTank());

        if (route.getFuelViolations() > 0) {
            System.out.println("  - Fuel Violations: " + route.getFuelViolations() + " (CRITICAL!)");
        }

        if (route.getTimeWindowViolations() > 0) {
            System.out.println("  - Time Window Violations: " + route.getTimeWindowViolations());
        }

        if (route.getCapacityViolations() > 0) {
            System.out.println("  - Capacity Violations: " + route.getCapacityViolations());
        }

        // Show the refueling station usage
        List<Location> usedRefuelingStations = route.getUsedRefuelingStations();
        if (!usedRefuelingStations.isEmpty()) {
            System.out.println("  - Refueling Stations Used: " + usedRefuelingStations.size());
            System.out.println("  - Refueling Stations: " + formatLocationSequence(usedRefuelingStations));
        }

        printRouteSequenceWithFuel(route);
    }

    private static void printRouteSequenceWithFuel(Route route) {
        List<Location> locations = route.getLocations();
        List<Double> fuelLevels = route.getFuelLevels();

        System.out.println("\nRoute Sequence with Fuel Levels:");
        System.out.println("┌───────┬─────────────┬─────────────┬──────────────┬────────────┬────────────┐");
        System.out.println("│ Step  │ Location ID │ Coordinates │  Time Window │   Type     │   Fuel     │");
        System.out.println("├───────┼─────────────┼─────────────┼──────────────┼────────────┼────────────┤");

        for (int i = 0; i < locations.size(); i++) {
            Location loc = locations.get(i);
            String type = loc.getType();
            double fuelLevel = (i < fuelLevels.size()) ? fuelLevels.get(i) : 0.0;

            String fuelStr;
            if (fuelLevel < route.getVehicle().getMinFuelTank() * 1.2) {
                fuelStr = "\u001B[31m" + String.format("%.2f", fuelLevel) + "\u001B[0m"; // Red for critically low
            } else if (fuelLevel < route.getVehicle().getMaxFuelTank() * 0.3) {
                fuelStr = "\u001B[33m" + String.format("%.2f", fuelLevel) + "\u001B[0m"; // Yellow for low
            } else {
                fuelStr = String.format("%.2f", fuelLevel);
            }

            String formattedType = type;
            if (type.equalsIgnoreCase("refueling_station")) {
                formattedType = "\u001B[31m" + type + "\u001B[0m"; // Red color
            }

            System.out.printf("│ %5d │ %11d │ (%6.2f,%6.2f) │ %4.1f - %4.1f │ %-10s │ %10s │%n",
                    i+1, loc.getId(), loc.getX(), loc.getY(),
                    loc.getTimeWindow()[0], loc.getTimeWindow()[1], formattedType, fuelStr);
        }

        System.out.println("└───────┴─────────────┴─────────────┴──────────────┴────────────┴────────────┘");
    }

    private static String formatLocationSequence(List<Location> locations) {
        StringBuilder sb = new StringBuilder();
        for (Location loc : locations) {
            sb.append(loc.getId());
            if (loc.getType().equalsIgnoreCase("depot")) sb.append("(D)");
            else if (loc.getType().equalsIgnoreCase("refueling_station")) sb.append("(R)");
            sb.append(" → ");
        }

        if (sb.length() > 3) sb.setLength(sb.length() - 3);
        return sb.toString();
    }
}