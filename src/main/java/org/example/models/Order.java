package org.example.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an order containing multiple products to be delivered to a location
 */
public class Order {
    private String orderId;
    private int locationId;
    private LocalDateTime orderTime;
    private LocalDateTime deliveryDeadline;
    private List<Product> products;
    private boolean delivered;
    private LocalDateTime deliveryTime;
    private int priority; // Calculated priority based on contents

    public Order(int locationId, LocalDateTime orderTime, LocalDateTime deliveryDeadline) {
        this.orderId = UUID.randomUUID().toString();
        this.locationId = locationId;
        this.orderTime = orderTime;
        this.deliveryDeadline = deliveryDeadline;
        this.products = new ArrayList<>();
        this.delivered = false;
        this.priority = 5; // Default lowest priority
    }

    /**
     * Add a product to the order
     */
    public void addProduct(Product product) {
        products.add(product);
        recalculatePriority(); // Update priority based on new product
    }

    /**
     * Calculate total weight of the order
     */
    public double getTotalWeight() {
        return products.stream()
                .mapToDouble(Product::getWeight)
                .sum();
    }

    /**
     * Mark the order as delivered
     */
    public void markDelivered() {
        this.delivered = true;
        this.deliveryTime = LocalDateTime.now();

        // Mark all products as delivered
        for (Product product : products) {
            product.markDelivered();
        }
    }

    /**
     * Calculate the order's priority based on product priorities
     * and delivery deadline urgency
     */
    private void recalculatePriority() {
        // Calculate average product priority
        int avgProductPriority = 5;
        if (!products.isEmpty()) {
            avgProductPriority = (int) Math.round(products.stream()
                    .mapToInt(Product::getPriority)
                    .average()
                    .orElse(5.0));
        }

        // Adjust priority based on deadline urgency
        LocalDateTime now = LocalDateTime.now();
        long hoursToDeadline = java.time.Duration.between(now, deliveryDeadline).toHours();

        if (hoursToDeadline < 2) {
            // Very urgent - increase priority by 2 levels (minimum 1)
            this.priority = Math.max(1, avgProductPriority - 2);
        } else if (hoursToDeadline < 6) {
            // Urgent - increase priority by 1 level (minimum 1)
            this.priority = Math.max(1, avgProductPriority - 1);
        } else {
            // Normal - use average product priority
            this.priority = avgProductPriority;
        }
    }

    /**
     * Check if order has any expired products
     */
    public boolean hasExpiredProducts() {
        return products.stream().anyMatch(Product::isExpired);
    }

    /**
     * Check if all products have acceptable quality
     */
    public boolean hasAcceptableQuality() {
        return products.stream().allMatch(Product::isAcceptableQuality);
    }

    /**
     * Check if order is overdue
     */
    public boolean isOverdue() {
        return !delivered && LocalDateTime.now().isAfter(deliveryDeadline);
    }

    /**
     * Update all products status with current environmental conditions
     */
    public void updateProductStatus(double temperature, double pressure, double timeElapsed) {
        for (Product product : products) {
            product.updateStatus(temperature, pressure, timeElapsed);
        }
    }

    // Getters and setters
    public String getOrderId() { return orderId; }
    public int getLocationId() { return locationId; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public LocalDateTime getDeliveryDeadline() { return deliveryDeadline; }
    public List<Product> getProducts() { return products; }
    public boolean isDelivered() { return delivered; }
    public LocalDateTime getDeliveryTime() { return deliveryTime; }
    public int getPriority() { return priority; }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", locationId=" + locationId +
                ", orderTime=" + orderTime +
                ", deliveryDeadline=" + deliveryDeadline +
                ", productsCount=" + products.size() +
                ", delivered=" + delivered +
                ", priority=" + priority +
                '}';
    }
}