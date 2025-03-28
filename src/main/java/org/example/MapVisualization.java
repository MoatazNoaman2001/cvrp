package org.example;

import org.example.models.*;

import java.util.List;

public class MapVisualization {

    // Map size parameters
    private static final int MAP_WIDTH = 80;
    private static final int MAP_HEIGHT = 25;

    // Characters for visualization
    private static final char DEPOT_CHAR = 'D';
    private static final char CUSTOMER_CHAR = 'C';
    private static final char REFUELING_CHAR = 'R';
    private static final char PATH_CHAR = '·';
    private static final char EMPTY_CHAR = ' ';
    private static final char ROAD_EVENT_CHAR = 'E';
    private static final char WEATHER_CHAR = 'W';
    private static final char TRAFFIC_CHAR = 'T';

    /**
     * Print a simple ASCII map of the route
     */
    public static void printRouteMap(Route route,
                                     List<RoadEvent> roadEvents,
                                     List<WeatherCondition> weatherConditions,
                                     List<TrafficCongestion> trafficCongestions) {
        List<Location> locations = route.getLocations();

        if (locations.isEmpty()) {
            System.out.println("Empty route - nothing to visualize");
            return;
        }

        // Find map bounds
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;

        for (Location loc : locations) {
            minX = Math.min(minX, loc.getX());
            maxX = Math.max(maxX, loc.getX());
            minY = Math.min(minY, loc.getY());
            maxY = Math.max(maxY, loc.getY());
        }

        // Add margin
        double margin = 0.1 * Math.max(maxX - minX, maxY - minY);
        minX -= margin;
        maxX += margin;
        minY -= margin;
        maxY += margin;

        // Create map
        char[][] map = new char[MAP_HEIGHT][MAP_WIDTH];

        // Initialize map with empty spaces
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                map[y][x] = EMPTY_CHAR;
            }
        }

        // Plot road events
        for (RoadEvent event : roadEvents) {
            int x1 = (int) Math.round((event.getXBegin() - minX) / (maxX - minX) * (MAP_WIDTH - 1));
            int y1 = (int) Math.round((event.getYBegin() - minY) / (maxY - minY) * (MAP_HEIGHT - 1));

            if (x1 >= 0 && x1 < MAP_WIDTH && y1 >= 0 && y1 < MAP_HEIGHT) {
                map[y1][x1] = ROAD_EVENT_CHAR;
            }
        }

        // Plot weather conditions
        for (WeatherCondition weather : weatherConditions) {
            int x1 = (int) Math.round((weather.getXBegin() - minX) / (maxX - minX) * (MAP_WIDTH - 1));
            int y1 = (int) Math.round((weather.getYBegin() - minY) / (maxY - minY) * (MAP_HEIGHT - 1));

            if (x1 >= 0 && x1 < MAP_WIDTH && y1 >= 0 && y1 < MAP_HEIGHT) {
                map[y1][x1] = WEATHER_CHAR;
            }
        }

        // Plot traffic congestions
        for (TrafficCongestion traffic : trafficCongestions) {
            int x1 = (int) Math.round((traffic.getXBegin() - minX) / (maxX - minX) * (MAP_WIDTH - 1));
            int y1 = (int) Math.round((traffic.getYBegin() - minY) / (maxY - minY) * (MAP_HEIGHT - 1));

            if (x1 >= 0 && x1 < MAP_WIDTH && y1 >= 0 && y1 < MAP_HEIGHT) {
                map[y1][x1] = TRAFFIC_CHAR;
            }
        }

        // Plot route path
        for (int i = 0; i < locations.size() - 1; i++) {
            Location current = locations.get(i);
            Location next = locations.get(i + 1);

            // Plot line between locations using Bresenham's algorithm
            plotLine(map, current, next, minX, maxX, minY, maxY);
        }

        // Plot locations (last to override paths)
        for (Location loc : locations) {
            int x = (int) Math.round((loc.getX() - minX) / (maxX - minX) * (MAP_WIDTH - 1));
            int y = (int) Math.round((loc.getY() - minY) / (maxY - minY) * (MAP_HEIGHT - 1));

            if (x >= 0 && x < MAP_WIDTH && y >= 0 && y < MAP_HEIGHT) {
                if (loc.getType().equalsIgnoreCase("depot")) {
                    map[y][x] = DEPOT_CHAR;
                } else if (loc.getType().equalsIgnoreCase("refueling_station")) {
                    map[y][x] = REFUELING_CHAR;
                } else {
                    map[y][x] = CUSTOMER_CHAR;
                }
            }
        }

        // Print the map
        System.out.println("\nRoute Visualization:");
        System.out.println("┌" + "─".repeat(MAP_WIDTH) + "┐");

        for (int y = 0; y < MAP_HEIGHT; y++) {
            System.out.print("│");
            for (int x = 0; x < MAP_WIDTH; x++) {
                System.out.print(map[y][x]);
            }
            System.out.println("│");
        }

        System.out.println("└" + "─".repeat(MAP_WIDTH) + "┘");
        System.out.println("Legend: D=Depot, C=Customer, R=Refueling, E=Road Event, W=Weather, T=Traffic, ·=Path");
    }

    /**
     * Plot a line between two locations using Bresenham's algorithm
     */
    private static void plotLine(char[][] map, Location start, Location end,
                                 double minX, double maxX, double minY, double maxY) {
        int x1 = (int) Math.round((start.getX() - minX) / (maxX - minX) * (MAP_WIDTH - 1));
        int y1 = (int) Math.round((start.getY() - minY) / (maxY - minY) * (MAP_HEIGHT - 1));
        int x2 = (int) Math.round((end.getX() - minX) / (maxX - minX) * (MAP_WIDTH - 1));
        int y2 = (int) Math.round((end.getY() - minY) / (maxY - minY) * (MAP_HEIGHT - 1));

        // Ensure points are within bounds
        x1 = Math.max(0, Math.min(MAP_WIDTH - 1, x1));
        y1 = Math.max(0, Math.min(MAP_HEIGHT - 1, y1));
        x2 = Math.max(0, Math.min(MAP_WIDTH - 1, x2));
        y2 = Math.max(0, Math.min(MAP_HEIGHT - 1, y2));

        // Calculate deltas
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            // Skip plotting on locations (will be plotted later)
            char existing = map[y1][x1];
            if (existing != DEPOT_CHAR && existing != CUSTOMER_CHAR &&
                    existing != REFUELING_CHAR) {
                map[y1][x1] = PATH_CHAR;
            }

            if (x1 == x2 && y1 == y2) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }
}