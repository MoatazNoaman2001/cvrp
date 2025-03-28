package org.example.models;

public class WeatherCondition {
    private double xBegin, yBegin; // Coordinates of the beginning of the edge
    private double xEnd, yEnd; // Coordinates of the end of the edge
    private String weatherType; // Type of weather: "Clear", "Light Rain", "Heavy Rain", "Fog", "Snow", "Storm"
    private double delay; // Computed delay in minutes

    public WeatherCondition(double xBegin, double yBegin, double xEnd, double yEnd, String weatherType) {
        this.xBegin = xBegin;
        this.yBegin = yBegin;
        this.xEnd = xEnd;
        this.yEnd = yEnd;
        this.weatherType = weatherType;
        this.delay = calculateDelay();
    }

    /**
     * Calculate delay based on weather condition
     * Implements Algorithm 3 from the documentation for Weather Conditions
     */
    private double calculateDelay() {
        switch (weatherType.toLowerCase()) {
            case "clear":
                return 0.0; // No impact
            case "light rain":
                return 3.0; // Minor slowdown, continue route
            case "heavy rain":
            case "fog":
                return 8.0; // Speed reduction, reroute if needed
            case "snow":
            case "storm":
                return 15.0; // High-risk conditions, rerouting needed
            default:
                return 0.0; // Default: no delay
        }
    }

    // Getters
    public double getXBegin() { return xBegin; }
    public double getYBegin() { return yBegin; }
    public double getXEnd() { return xEnd; }
    public double getYEnd() { return yEnd; }
    public String getWeatherType() { return weatherType; }
    public double getDelay() { return delay; }

    @Override
    public String toString() {
        return "WeatherCondition{" +
                "from=(" + xBegin + "," + yBegin + "), " +
                "to=(" + xEnd + "," + yEnd + "), " +
                "weatherType='" + weatherType + "', " +
                "delay=" + delay + " minutes" +
                '}';
    }
}