package org.example.models;

import java.time.LocalDateTime;

/**
 * Represents a product being delivered with specific characteristics
 * that need to be monitored during delivery.
 */
public class Product {
    private int id;
    private String name;
    private double weight;
    private double temperature; // Current temperature (for medical supplies that need temp control)
    private double requiredTemperature; // Required temperature range
    private double temperatureTolerance; // Allowed deviation from required temperature
    private double pressure; // Current pressure (for sensitive medical supplies)
    private double requiredPressure;
    private double pressureTolerance; // Allowed deviation from required pressure
    private LocalDateTime expiryDate;
    private int priority; // Delivery priority (1-5, where 1 is highest)

    // Product status tracking
    private boolean delivered;
    private LocalDateTime deliveryTime;
    private double qualityIndex; // Index from 0 to 100 indicating product quality

    public Product(int id, String name, double weight, double requiredTemperature,
                   double temperatureTolerance, double requiredPressure,
                   double pressureTolerance, LocalDateTime expiryDate, int priority) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.temperature = requiredTemperature; // Start at required temperature
        this.requiredTemperature = requiredTemperature;
        this.temperatureTolerance = temperatureTolerance;
        this.pressure = requiredPressure; // Start at required pressure
        this.requiredPressure = requiredPressure;
        this.pressureTolerance = pressureTolerance;
        this.expiryDate = expiryDate;
        this.priority = priority;
        this.delivered = false;
        this.qualityIndex = 100.0; // Start with perfect quality
    }

    /**
     * Update product status based on current environmental conditions
     * @param currentTemperature Current temperature
     * @param currentPressure Current pressure
     * @param timeElapsed Time elapsed since last update (minutes)
     */
    public void updateStatus(double currentTemperature, double currentPressure, double timeElapsed) {
        // Update current temperature and pressure
        this.temperature = currentTemperature;
        this.pressure = currentPressure;

        // Calculate quality degradation based on deviation from required conditions
        double tempDeviation = Math.abs(currentTemperature - requiredTemperature) / temperatureTolerance;
        double pressureDeviation = Math.abs(currentPressure - requiredPressure) / pressureTolerance;

        // Calculate quality degradation rate (% per minute)
        double degradationRate = 0.0;

        // Temperature outside tolerance causes faster degradation
        if (tempDeviation > 1.0) {
            degradationRate += 0.05 * tempDeviation;
        }

        // Pressure outside tolerance causes faster degradation
        if (pressureDeviation > 1.0) {
            degradationRate += 0.03 * pressureDeviation;
        }

        // Apply degradation based on time elapsed
        double qualityReduction = degradationRate * timeElapsed;
        this.qualityIndex = Math.max(0.0, this.qualityIndex - qualityReduction);
    }

    /**
     * Mark the product as delivered
     */
    public void markDelivered() {
        this.delivered = true;
        this.deliveryTime = LocalDateTime.now();
    }

    /**
     * Check if product is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Check if product quality is acceptable (above 50%)
     */
    public boolean isAcceptableQuality() {
        return qualityIndex >= 50.0;
    }

    /**
     * Check if temperature is within acceptable range
     */
    public boolean isTemperatureInRange() {
        return Math.abs(temperature - requiredTemperature) <= temperatureTolerance;
    }

    /**
     * Check if pressure is within acceptable range
     */
    public boolean isPressureInRange() {
        return Math.abs(pressure - requiredPressure) <= pressureTolerance;
    }

    // Getters and setters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getWeight() { return weight; }
    public double getTemperature() { return temperature; }
    public double getRequiredTemperature() { return requiredTemperature; }
    public double getTemperatureTolerance() { return temperatureTolerance; }
    public double getPressure() { return pressure; }
    public double getRequiredPressure() { return requiredPressure; }
    public double getPressureTolerance() { return pressureTolerance; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public int getPriority() { return priority; }
    public boolean isDelivered() { return delivered; }
    public LocalDateTime getDeliveryTime() { return deliveryTime; }
    public double getQualityIndex() { return qualityIndex; }

    public void setTemperature(double temperature) { this.temperature = temperature; }
    public void setPressure(double pressure) { this.pressure = pressure; }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", weight=" + weight +
                ", temperature=" + temperature +
                ", requiredTemperature=" + requiredTemperature +
                ", pressure=" + pressure +
                ", requiredPressure=" + requiredPressure +
                ", expiryDate=" + expiryDate +
                ", priority=" + priority +
                ", delivered=" + delivered +
                ", qualityIndex=" + qualityIndex +
                '}';
    }
}