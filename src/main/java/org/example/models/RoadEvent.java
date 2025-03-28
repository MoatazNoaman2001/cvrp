package org.example.models;

public class RoadEvent {
    private double xBegin, yBegin; // Coordinates of the beginning of the edge
    private double xEnd, yEnd; // Coordinates of the end of the edge
    private String eventType; // Type of road event: "Clear", "Accident", "Construction", "Roadblock"
    private double delay; // Computed delay in minutes

    public RoadEvent(double xBegin, double yBegin, double xEnd, double yEnd, String eventType) {
        this.xBegin = xBegin;
        this.yBegin = yBegin;
        this.xEnd = xEnd;
        this.yEnd = yEnd;
        this.eventType = eventType;
        this.delay = calculateDelay();
    }

    /**
     * Calculate delay based on road event type
     * Implements Algorithm 3 from the documentation for Road Events
     */
    private double calculateDelay() {
        switch (eventType.toLowerCase()) {
            case "clear":
                return 0.0; // No delay
            case "accident":
                return 5.0; // Partial or full blockage, rerouting needed
            case "construction":
                return 10.0; // Moderate delays, reroute if severe
            case "roadblock":
                return 15.0; // Immediate rerouting required
            default:
                return 0.0; // Default: no delay
        }
    }

    // Getters
    public double getXBegin() { return xBegin; }
    public double getYBegin() { return yBegin; }
    public double getXEnd() { return xEnd; }
    public double getYEnd() { return yEnd; }
    public String getEventType() { return eventType; }
    public double getDelay() { return delay; }

    @Override
    public String toString() {
        return "RoadEvent{" +
                "from=(" + xBegin + "," + yBegin + "), " +
                "to=(" + xEnd + "," + yEnd + "), " +
                "eventType='" + eventType + "', " +
                "delay=" + delay + " minutes" +
                '}';
    }
}