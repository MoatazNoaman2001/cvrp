package org.example.models;

public class Vehicle {
    private int id;
    private double speed; // Travel speed (km/h or m/s)
    private double maxFuelTank; // Maximum fuel tank capacity (kwh)
    private double minFuelTank; // Minimum fuel tank level before refueling (kwh)
    private double currentFuelLevel; // Current fuel tank charge
    private double payloadCapacity; // Maximum weight the vehicle can carry (kg)
    private double baseEnergyConsumption; // Energy used per km without load (kWh/km)
    private double payloadEnergyCoefficient; // Additional energy consumption per kg payload
    private double refuelingTime; // Time to fully refuel
    private double refuelingCost; // Cost of each refueling operation

    public Vehicle(int id, double speed, double maxFuelTank, double minFuelTank, double payloadCapacity,
                   double baseEnergyConsumption, double payloadEnergyCoefficient,
                   double refuelingTime, double refuelingCost) {
        this.id = id;
        this.speed = speed;
        this.maxFuelTank = maxFuelTank;
        this.minFuelTank = minFuelTank;
        this.currentFuelLevel = maxFuelTank; // Start with full tank
        this.payloadCapacity = payloadCapacity;
        this.baseEnergyConsumption = baseEnergyConsumption;
        this.payloadEnergyCoefficient = payloadEnergyCoefficient;
        this.refuelingTime = refuelingTime;
        this.refuelingCost = refuelingCost;
    }

    // Getters and setters
    public int getId() { return id; }
    public double getSpeed() { return speed; }
    public double getMaxFuelTank() { return maxFuelTank; }
    public double getMinFuelTank() { return minFuelTank; }
    public double getCurrentFuelLevel() { return currentFuelLevel; }
    public void setCurrentFuelLevel(double currentFuelLevel) { this.currentFuelLevel = currentFuelLevel; }
    public double getPayloadCapacity() { return payloadCapacity; }
    public double getBaseEnergyConsumption() { return baseEnergyConsumption; }
    public double getPayloadEnergyCoefficient() { return payloadEnergyCoefficient; }
    public double getRefuelingTime() { return refuelingTime; }
    public double getRefuelingCost() { return refuelingCost; }

    @Override
    public String toString() {
        return "Vehicle{" +
                "id=" + id +
                ", speed=" + speed +
                ", maxFuelTank=" + maxFuelTank +
                ", minFuelTank=" + minFuelTank +
                ", currentFuelLevel=" + currentFuelLevel +
                ", payloadCapacity=" + payloadCapacity +
                ", baseEnergyConsumption=" + baseEnergyConsumption +
                ", payloadEnergyCoefficient=" + payloadEnergyCoefficient +
                ", refuelingTime=" + refuelingTime +
                ", refuelingCost=" + refuelingCost +
                '}';
    }
}