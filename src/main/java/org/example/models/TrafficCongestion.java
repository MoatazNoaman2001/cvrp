package org.example.models;

public class TrafficCongestion {
    private double xBegin, yBegin; // Coordinates of the beginning of the edge
    private double xEnd, yEnd; // Coordinates of the end of the edge
    private double trafficSpeed; // Current traffic speed in km/h
    private double delay; // Computed delay in minutes

    public TrafficCongestion(double xBegin, double yBegin, double xEnd, double yEnd, double trafficSpeed) {
        this.xBegin = xBegin;
        this.yBegin = yBegin;
        this.xEnd = xEnd;
        this.yEnd = yEnd;
        this.trafficSpeed = trafficSpeed;
        this.delay = calculateDelay();
    }

    /**
     * Calculate delay based on traffic speed
     * Implements Algorithm 3 from the documentation
     */
    private double calculateDelay() {
        if (trafficSpeed > 60.0) {
            return 0.0; // No delay
        } else if (trafficSpeed <= 60.0 && trafficSpeed > 40.0) {
            return 5.0; // Minor delay, monitor conditions
        } else if (trafficSpeed <= 40.0 && trafficSpeed > 20.0) {
            return 10.0; // Possible rerouting required
        } else {
            return 15.0; // Immediate rerouting needed
        }
    }

    public double getXBegin() { return xBegin; }
    public double getYBegin() { return yBegin; }
    public double getXEnd() { return xEnd; }
    public double getYEnd() { return yEnd; }
    public double getTrafficSpeed() { return trafficSpeed; }
    public double getDelay() { return delay; }

    @Override
    public String toString() {
        String status;
        if (delay == 0.0) {
            status = "Free Flow";
        } else if (delay == 5.0) {
            status = "Moderate";
        } else if (delay == 10.0) {
            status = "Heavy";
        } else {
            status = "Severe";
        }

        return "TrafficCongestion{" +
                "from=(" + xBegin + "," + yBegin + "), " +
                "to=(" + xEnd + "," + yEnd + "), " +
                "speed=" + trafficSpeed + " km/h, " +
                "status='" + status + "', " +
                "delay=" + delay + " minutes" +
                '}';
    }
}