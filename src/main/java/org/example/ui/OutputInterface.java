package org.example.ui;

import org.example.Route;
import org.example.models.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OutputInterface extends JFrame {
    private List<Route> routes = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();
    private List<Vehicle> vehicles = new ArrayList<>();

    // UI Components
    private JTabbedPane tabbedPane;
    private JPanel routeMapPanel;
    private JTable routeTable;
    private JTable orderStatusTable;
    private JTable vehicleStatusTable;
    private DefaultTableModel routeTableModel;
    private DefaultTableModel orderStatusTableModel;
    private DefaultTableModel vehicleStatusTableModel;

    // Performance indicators
    private JLabel totalDistanceLabel;
    private JLabel totalCostLabel;
    private JLabel totalTimeLabel;
    private JLabel deliveredOrdersLabel;
    private JLabel pendingOrdersLabel;
    private JLabel simulationTimeLabel; // Added for direct access

    // Simulation
    private Timer simulationTimer;
    private boolean simulationRunning = false;
    private double simulationTime = 0.0; // in hours
    private double simulationSpeed = 1.0; // 1 simulation hour per real second

    // Order details panel components
    private JPanel orderDetailsPanel;
    private JTextArea orderDetailsArea;
    private JPanel orderConditionsPanel;
    private JLabel temperatureLabel;
    private JLabel pressureLabel;
    private JLabel expiryDateLabel;
    private JProgressBar temperatureProgressBar;
    private JProgressBar pressureProgressBar;
    private JProgressBar expiryProgressBar;

    public OutputInterface() {
        setTitle("CVRP Output Interface");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Create tabs
        createDashboardPanel();
        createRoutePanel();
        createOrderStatusPanel();
        createVehicleStatusPanel();
        createRoutePlanningPanel();

        // Add tabbed pane to frame
        add(tabbedPane, BorderLayout.CENTER);

        // Create control panel
        JPanel controlPanel = new JPanel();
        JButton loadButton = new JButton("Load Results");
        JButton exportButton = new JButton("Export Reports");
        JButton simulateButton = new JButton("Start Simulation");

        loadButton.addActionListener(e -> loadResults());
        exportButton.addActionListener(e -> exportReports());
        simulateButton.addActionListener(e -> toggleSimulation(simulateButton));

        controlPanel.add(loadButton);
        controlPanel.add(exportButton);
        controlPanel.add(simulateButton);

        add(controlPanel, BorderLayout.SOUTH);

        // Initialize simulation timer
        simulationTimer = new Timer(1000, e -> updateSimulation());

        // Initialize with sample data for demo
        loadSampleData();
    }

    /**
     * Create dashboard panel with performance indicators
     */
    private void createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Performance indicators panel
        JPanel indicatorsPanel = new JPanel(new GridLayout(2, 3, 20, 20));
        indicatorsPanel.setBorder(BorderFactory.createTitledBorder("Performance Indicators"));

        // Create indicator panels
        totalDistanceLabel = createIndicatorPanel(indicatorsPanel, "Total Route Distance", "0.0 km");
        totalCostLabel = createIndicatorPanel(indicatorsPanel, "Total Route Cost", "$0.0");
        totalTimeLabel = createIndicatorPanel(indicatorsPanel, "Total Route Time", "0.0 hours");
        deliveredOrdersLabel = createIndicatorPanel(indicatorsPanel, "Delivered Orders", "0/0");
        pendingOrdersLabel = createIndicatorPanel(indicatorsPanel, "Pending Orders", "0");

        // Add a simulation time indicator
        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.setBorder(BorderFactory.createTitledBorder("Simulation Time"));
        simulationTimeLabel = new JLabel("0.0 hours", SwingConstants.CENTER);
        simulationTimeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        timePanel.add(simulationTimeLabel, BorderLayout.CENTER);
        indicatorsPanel.add(timePanel);

        panel.add(indicatorsPanel, BorderLayout.NORTH);

        // Create summary charts panel (placeholder)
        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 10, 10));

        // Route distance chart (placeholder)
        JPanel distanceChartPanel = new JPanel();
        distanceChartPanel.setBorder(BorderFactory.createTitledBorder("Route Distances"));
        distanceChartPanel.setPreferredSize(new Dimension(400, 300));
        chartsPanel.add(distanceChartPanel);

        // Route priority fulfillment chart (placeholder)
        JPanel priorityChartPanel = new JPanel();
        priorityChartPanel.setBorder(BorderFactory.createTitledBorder("Priority Fulfillment"));
        priorityChartPanel.setPreferredSize(new Dimension(400, 300));
        chartsPanel.add(priorityChartPanel);

        panel.add(chartsPanel, BorderLayout.CENTER);

        tabbedPane.addTab("Dashboard", panel);
    }

    /**
     * Create a performance indicator panel
     */
    private JLabel createIndicatorPanel(JPanel parent, String title, String initialValue) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        JLabel valueLabel = new JLabel(initialValue, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));

        panel.add(valueLabel, BorderLayout.CENTER);
        parent.add(panel);

        return valueLabel;
    }

    /**
     * Create route monitoring panel
     */
    private void createRoutePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Route table
        String[] columnNames = {"Route ID", "Vehicle ID", "Start Time", "End Time", "Distance",
                "Cost", "Locations Count", "Status"};
        routeTableModel = new DefaultTableModel(columnNames, 0);
        routeTable = new JTable(routeTableModel);

        JScrollPane scrollPane = new JScrollPane(routeTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Route details panel
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Route Details"));

        // Will be populated when a route is selected
        JTextArea routeDetailsArea = new JTextArea();
        routeDetailsArea.setEditable(false);
        detailsPanel.add(new JScrollPane(routeDetailsArea), BorderLayout.CENTER);

        // Add selection listener to show details
        routeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = routeTable.getSelectedRow();
                if (selectedRow >= 0) {
                    // Show details of selected route
                    int routeId = (int) routeTable.getValueAt(selectedRow, 0);
                    // In real implementation, we'd look up the route details
                    routeDetailsArea.setText("Details for Route " + routeId + ":\n\n" +
                            "This panel would show detailed information about the selected route, " +
                            "including each stop, arrival times, and current status.");
                } else {
                    routeDetailsArea.setText("");
                }
            }
        });

        panel.add(detailsPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Route Monitoring", panel);
    }

    /**
     * Create order status panel with enhanced monitoring capabilities
     */
    private void createOrderStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Order status table with enhanced columns
        String[] columnNames = {"Order ID", "Location", "Priority", "Deadline", "Products",
                "Status", "Delivery Time", "Quality", "Temp.", "Pressure", "Expiry"};
        orderStatusTableModel = new DefaultTableModel(columnNames, 0);
        orderStatusTable = new JTable(orderStatusTableModel);

        JScrollPane scrollPane = new JScrollPane(orderStatusTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Filter panel
        JPanel filterPanel = new JPanel();
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filters"));

        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All", "Delivered", "In Transit", "Pending"});
        JComboBox<String> priorityFilter = new JComboBox<>(new String[]{"All", "High", "Medium", "Low"});

        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(statusFilter);
        filterPanel.add(new JLabel("Priority:"));
        filterPanel.add(priorityFilter);

        JButton applyFilterButton = new JButton("Apply Filter");
        filterPanel.add(applyFilterButton);

        panel.add(filterPanel, BorderLayout.NORTH);

        // Enhanced Order details panel with conditions monitoring
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Order Details"));

        // Order information text area
        orderDetailsArea = new JTextArea();
        orderDetailsArea.setEditable(false);

        // Create order conditions panel for monitoring temperature, pressure and expiry date
        orderConditionsPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        orderConditionsPanel.setBorder(BorderFactory.createTitledBorder("Current Conditions"));

        // Temperature monitoring
        JPanel tempPanel = new JPanel(new BorderLayout());
        temperatureLabel = new JLabel("Temperature: 20.0 °C", SwingConstants.LEFT);
        temperatureProgressBar = new JProgressBar(0, 100);
        temperatureProgressBar.setValue(70);
        temperatureProgressBar.setStringPainted(true);
        tempPanel.add(temperatureLabel, BorderLayout.NORTH);
        tempPanel.add(temperatureProgressBar, BorderLayout.CENTER);

        // Pressure monitoring
        JPanel pressurePanel = new JPanel(new BorderLayout());
        pressureLabel = new JLabel("Pressure: 1.0 atm", SwingConstants.LEFT);
        pressureProgressBar = new JProgressBar(0, 100);
        pressureProgressBar.setValue(80);
        pressureProgressBar.setStringPainted(true);
        pressurePanel.add(pressureLabel, BorderLayout.NORTH);
        pressurePanel.add(pressureProgressBar, BorderLayout.CENTER);

        // Expiry date monitoring
        JPanel expiryPanel = new JPanel(new BorderLayout());
        expiryDateLabel = new JLabel("Expiry: 30 days remaining", SwingConstants.LEFT);
        expiryProgressBar = new JProgressBar(0, 100);
        expiryProgressBar.setValue(90);
        expiryProgressBar.setStringPainted(true);
        expiryPanel.add(expiryDateLabel, BorderLayout.NORTH);
        expiryPanel.add(expiryProgressBar, BorderLayout.CENTER);

        // Add condition panels to the order conditions panel
        orderConditionsPanel.add(tempPanel);
        orderConditionsPanel.add(pressurePanel);
        orderConditionsPanel.add(expiryPanel);

        // Layout the details panel
        JPanel orderInfoPanel = new JPanel(new BorderLayout());
        orderInfoPanel.add(new JScrollPane(orderDetailsArea), BorderLayout.CENTER);

        detailsPanel.add(orderInfoPanel, BorderLayout.CENTER);
        detailsPanel.add(orderConditionsPanel, BorderLayout.EAST);

        // Initially hide the conditions panel until an order is selected
        orderConditionsPanel.setVisible(false);

        // Add selection listener to show details
        orderStatusTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = orderStatusTable.getSelectedRow();
                if (selectedRow >= 0) {
                    // Show details of selected order
                    String orderId = (String) orderStatusTable.getValueAt(selectedRow, 0);
                    String status = (String) orderStatusTable.getValueAt(selectedRow, 5);

                    // Update the details text area
                    orderDetailsArea.setText("Details for Order " + orderId + ":\n\n" +
                            "This panel shows detailed information about the selected order, " +
                            "including product list, current status of each product, " +
                            "and environmental conditions monitoring.\n\n" +
                            "Status: " + status + "\n" +
                            "Products: " + orderStatusTable.getValueAt(selectedRow, 4) + "\n" +
                            "Priority: " + orderStatusTable.getValueAt(selectedRow, 2) + "\n" +
                            "Deadline: " + orderStatusTable.getValueAt(selectedRow, 3));

                    // Show the conditions panel and update with sample data
                    orderConditionsPanel.setVisible(true);

                    // In a real implementation, we would fetch the actual data for this order
                    // For now, use random data for demonstration
                    Random rand = new Random();

                    // Temperature (normal range: 2-8°C for medical supplies)
                    double temperature = 2 + rand.nextDouble() * 8;
                    int tempStatus = (int) (((temperature - 2) / 6) * 100);
                    temperatureLabel.setText(String.format("Temperature: %.1f °C", temperature));
                    temperatureProgressBar.setValue(tempStatus);

                    // Set color based on temperature range
                    if (temperature < 2 || temperature > 8) {
                        temperatureProgressBar.setForeground(Color.RED);
                    } else if (temperature < 3 || temperature > 7) {
                        temperatureProgressBar.setForeground(Color.ORANGE);
                    } else {
                        temperatureProgressBar.setForeground(Color.GREEN);
                    }

                    // Pressure (normal is around 1 atm)
                    double pressure = 0.9 + rand.nextDouble() * 0.2;
                    int pressureStatus = (int) (((pressure - 0.9) / 0.2) * 100);
                    pressureLabel.setText(String.format("Pressure: %.2f atm", pressure));
                    pressureProgressBar.setValue(pressureStatus);

                    // Set color based on pressure range
                    if (pressure < 0.95 || pressure > 1.05) {
                        pressureProgressBar.setForeground(Color.ORANGE);
                    } else {
                        pressureProgressBar.setForeground(Color.GREEN);
                    }

                    // Expiry date (days remaining)
                    int daysRemaining = 5 + rand.nextInt(60);
                    int expiryStatus = Math.min(100, (int) (daysRemaining / 60.0 * 100));
                    expiryDateLabel.setText("Expiry: " + daysRemaining + " days remaining");
                    expiryProgressBar.setValue(expiryStatus);

                    // Set color based on days remaining
                    if (daysRemaining < 15) {
                        expiryProgressBar.setForeground(Color.RED);
                    } else if (daysRemaining < 30) {
                        expiryProgressBar.setForeground(Color.ORANGE);
                    } else {
                        expiryProgressBar.setForeground(Color.GREEN);
                    }
                } else {
                    orderDetailsArea.setText("");
                    orderConditionsPanel.setVisible(false);
                }
            }
        });

        panel.add(detailsPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Order Status", panel);
    }

    /**
     * Create vehicle status panel
     */
    private void createVehicleStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Vehicle status table
        String[] columnNames = {"Vehicle ID", "Current Location", "Fuel Level", "Speed",
                "Current Load", "Status", "Next Stop"};
        vehicleStatusTableModel = new DefaultTableModel(columnNames, 0);
        vehicleStatusTable = new JTable(vehicleStatusTableModel);

        JScrollPane scrollPane = new JScrollPane(vehicleStatusTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Vehicle details panel
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Vehicle Details"));

        // Will be populated when a vehicle is selected
        JTextArea vehicleDetailsArea = new JTextArea();
        vehicleDetailsArea.setEditable(false);
        detailsPanel.add(new JScrollPane(vehicleDetailsArea), BorderLayout.CENTER);

        // Add selection listener to show details
        vehicleStatusTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = vehicleStatusTable.getSelectedRow();
                if (selectedRow >= 0) {
                    // Show details of selected vehicle
                    int vehicleId = (int) vehicleStatusTable.getValueAt(selectedRow, 0);
                    // In real implementation, we'd look up the vehicle details
                    vehicleDetailsArea.setText("Details for Vehicle " + vehicleId + ":\n\n" +
                            "This panel would show detailed information about the selected vehicle, " +
                            "including current route, payload details, and telemetry data.");
                } else {
                    vehicleDetailsArea.setText("");
                }
            }
        });

        panel.add(detailsPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Vehicle Status", panel);
    }

    /**
     * Create route planning visualization panel
     */
    private void createRoutePlanningPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Map visualization panel (placeholder)
        routeMapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawRouteMap(g);
            }
        };
        routeMapPanel.setBackground(Color.WHITE);

        panel.add(routeMapPanel, BorderLayout.CENTER);

        // Route selection panel
        JPanel selectionPanel = new JPanel();
        selectionPanel.setBorder(BorderFactory.createTitledBorder("Route Selection"));

        JComboBox<String> routeSelector = new JComboBox<>(new String[]{"All Routes", "Route 1", "Route 2", "Route 3"});
        JCheckBox showDepotCheckbox = new JCheckBox("Show Depot", true);
        JCheckBox showRefuelingCheckbox = new JCheckBox("Show Refueling Stations", true);
        JCheckBox showTrafficCheckbox = new JCheckBox("Show Traffic", true);
        JCheckBox showWeatherCheckbox = new JCheckBox("Show Weather", true);

        selectionPanel.add(new JLabel("Display:"));
        selectionPanel.add(routeSelector);
        selectionPanel.add(showDepotCheckbox);
        selectionPanel.add(showRefuelingCheckbox);
        selectionPanel.add(showTrafficCheckbox);
        selectionPanel.add(showWeatherCheckbox);

        panel.add(selectionPanel, BorderLayout.NORTH);

        // Legend panel
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        legendPanel.setBorder(BorderFactory.createTitledBorder("Legend"));

        addLegendItem(legendPanel, Color.RED, "Depot");
        addLegendItem(legendPanel, Color.BLUE, "Customer");
        addLegendItem(legendPanel, Color.GREEN, "Refueling Station");
        addLegendItem(legendPanel, Color.ORANGE, "Vehicle");
        addLegendItem(legendPanel, Color.MAGENTA, "Traffic Congestion");
        addLegendItem(legendPanel, Color.CYAN, "Weather Event");

        panel.add(legendPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Route Visualization", panel);
    }

    /**
     * Add a legend item to the legend panel
     */
    private void addLegendItem(JPanel panel, Color color, String label) {
        JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JPanel colorSquare = new JPanel();
        colorSquare.setBackground(color);
        colorSquare.setPreferredSize(new Dimension(15, 15));

        itemPanel.add(colorSquare);
        itemPanel.add(new JLabel(label));

        panel.add(itemPanel);
    }

    /**
     * Draw route map (placeholder implementation)
     */
    private void drawRouteMap(Graphics g) {
        int width = routeMapPanel.getWidth();
        int height = routeMapPanel.getHeight();

        // Draw background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Add grid lines
        g.setColor(Color.LIGHT_GRAY);
        for (int x = 0; x < width; x += 50) {
            g.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += 50) {
            g.drawLine(0, y, width, y);
        }

        // In a real implementation, this would draw the actual routes, locations, and vehicles
        // Here we just draw some sample shapes

        // Draw depot
        g.setColor(Color.RED);
        g.fillRect(width / 2 - 10, height / 2 - 10, 20, 20);

        // Draw some customer locations
        g.setColor(Color.BLUE);
        Random rand = new Random(42); // Fixed seed for consistent drawing
        for (int i = 0; i < 10; i++) {
            int x = rand.nextInt(width - 40) + 20;
            int y = rand.nextInt(height - 40) + 20;
            g.fillOval(x - 5, y - 5, 10, 10);
        }

        // Draw some refueling stations
        g.setColor(Color.GREEN);
        for (int i = 0; i < 3; i++) {
            int x = rand.nextInt(width - 40) + 20;
            int y = rand.nextInt(height - 40) + 20;

            // Draw as a triangle
            int[] xPoints = {x, x - 10, x + 10};
            int[] yPoints = {y - 10, y + 10, y + 10};
            g.fillPolygon(xPoints, yPoints, 3);
        }

        // Draw sample routes
        g.setColor(Color.BLACK);
        drawSampleRoute(g, width, height, rand, 3, new Color(255, 0, 0, 128));
        drawSampleRoute(g, width, height, rand, 4, new Color(0, 0, 255, 128));
        drawSampleRoute(g, width, height, rand, 5, new Color(0, 255, 0, 128));

        // Draw vehicles (as circles on the routes)
        g.setColor(Color.ORANGE);
        for (int i = 0; i < 3; i++) {
            int x = rand.nextInt(width - 40) + 20;
            int y = rand.nextInt(height - 40) + 20;
            g.fillOval(x - 7, y - 7, 14, 14);
        }
    }

    /**
     * Draw a sample route for visualization
     */
    private void drawSampleRoute(Graphics g, int width, int height, Random rand,
                                 int numPoints, Color color) {
        // Start at depot
        int centerX = width / 2;
        int centerY = height / 2;

        int prevX = centerX;
        int prevY = centerY;

        g.setColor(color);

        // Generate random points for the route
        for (int i = 0; i < numPoints; i++) {
            int x = rand.nextInt(width - 40) + 20;
            int y = rand.nextInt(height - 40) + 20;

            // Draw line to this point
            g.drawLine(prevX, prevY, x, y);

            prevX = x;
            prevY = y;
        }

        // Return to depot
        g.drawLine(prevX, prevY, centerX, centerY);
    }

    /**
     * Load results (placeholder)
     */
    private void loadResults() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Results File");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this, "Loading results from: " +
                    fileChooser.getSelectedFile().getName());
            // In a real implementation, this would load actual results
        }
    }

    /**
     * Export reports (placeholder)
     */
    private void exportReports() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Reports");

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this, "Reports exported to: " +
                    fileChooser.getSelectedFile().getName());
            // In a real implementation, this would generate and save reports
        }
    }

    /**
     * Toggle simulation
     */
    private void toggleSimulation(JButton button) {
        if (simulationRunning) {
            simulationTimer.stop();
            simulationRunning = false;
            button.setText("Start Simulation");
        } else {
            simulationTimer.start();
            simulationRunning = true;
            button.setText("Stop Simulation");
        }
    }

    /**
     * Update simulation (called by timer)
     */
    private void updateSimulation() {
        // Increment simulation time
        simulationTime += simulationSpeed / 3600.0; // Convert to hours

        // Update labels
        updateSimulationDisplay();

        // Update route map
        routeMapPanel.repaint();
    }

    /**
     * Update simulation display
     */
    private void updateSimulationDisplay() {
        // Update dashboard time
        simulationTimeLabel.setText(String.format("%.1f hours", simulationTime));

        // In a real implementation, this would update vehicle positions, order statuses, etc.
        // Here we just update some random values for demonstration

        Random rand = new Random();

        // Update some random vehicle
        if (vehicleStatusTableModel.getRowCount() > 0) {
            int row = rand.nextInt(vehicleStatusTableModel.getRowCount());
            double fuelLevel = (double) vehicleStatusTableModel.getValueAt(row, 2);
            fuelLevel = Math.max(0.0, fuelLevel - rand.nextDouble() * 3.0);
            vehicleStatusTableModel.setValueAt(fuelLevel, row, 2);

            double speed = 20.0 + rand.nextDouble() * 40.0;
            vehicleStatusTableModel.setValueAt(speed, row, 3);
        }

        // Update some random order status including temperature, pressure, and expiry
        if (orderStatusTableModel.getRowCount() > 0) {
            int row = rand.nextInt(orderStatusTableModel.getRowCount());
            String status = (String) orderStatusTableModel.getValueAt(row, 5);

            // Update environmental conditions for this order
            double temperature = 2.0 + rand.nextDouble() * 10.0; // °C
            double pressure = 0.9 + rand.nextDouble() * 0.3; // atm
            int expiryDays = 10 + rand.nextInt(50); // days

            // Update the extended columns
            orderStatusTableModel.setValueAt(String.format("%.1f°C", temperature), row, 8);
            orderStatusTableModel.setValueAt(String.format("%.2f", pressure), row, 9);
            orderStatusTableModel.setValueAt(expiryDays + " days", row, 10);

            // Update status changes
            if (status.equals("Pending") && rand.nextDouble() < 0.2) {
                orderStatusTableModel.setValueAt("In Transit", row, 5);
            } else if (status.equals("In Transit") && rand.nextDouble() < 0.1) {
                orderStatusTableModel.setValueAt("Delivered", row, 5);
                orderStatusTableModel.setValueAt(
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        row, 6);

                // Update delivered orders count
                int delivered = Integer.parseInt(deliveredOrdersLabel.getText().split("/")[0]) + 1;
                int total = Integer.parseInt(deliveredOrdersLabel.getText().split("/")[1]);
                deliveredOrdersLabel.setText(delivered + "/" + total);

                // Update pending orders count
                int pending = Integer.parseInt(pendingOrdersLabel.getText()) - 1;
                pendingOrdersLabel.setText(String.valueOf(pending));
            }
        }
    }

    /**
     * Load sample data for demonstration
     */
    private void loadSampleData() {
        // Add sample routes
        routeTableModel.addRow(new Object[]{1, 101, "08:00", "10:30", 45.6, 78.9, 8, "Completed"});
        routeTableModel.addRow(new Object[]{2, 102, "08:15", "11:00", 52.3, 91.5, 10, "In Progress"});
        routeTableModel.addRow(new Object[]{3, 103, "09:00", "12:45", 38.7, 67.2, 6, "In Progress"});

        // Add sample orders with temperature, pressure, and expiry information
        orderStatusTableModel.addRow(new Object[]{"ORD-001", "Loc-12", "High", "09:30", 3, "Delivered", "09:15", "Good", "4.2°C", "1.01", "45 days"});
        orderStatusTableModel.addRow(new Object[]{"ORD-002", "Loc-15", "Medium", "10:15", 2, "In Transit", "", "", "5.7°C", "0.98", "30 days"});
        orderStatusTableModel.addRow(new Object[]{"ORD-003", "Loc-18", "Low", "11:30", 4, "Pending", "", "", "6.1°C", "1.03", "60 days"});
        orderStatusTableModel.addRow(new Object[]{"ORD-004", "Loc-21", "High", "09:45", 1, "Delivered", "09:30", "Excellent", "3.9°C", "1.00", "20 days"});
        orderStatusTableModel.addRow(new Object[]{"ORD-005", "Loc-24", "Medium", "12:00", 3, "Pending", "", "", "7.3°C", "0.97", "15 days"});

        // Add sample vehicle statuses
        vehicleStatusTableModel.addRow(new Object[]{101, "Depot", 85.3, 0.0, 0.0, "Idle", "None"});
        vehicleStatusTableModel.addRow(new Object[]{102, "Loc-15", 67.8, 42.5, 45.6, "Delivering", "Loc-18"});
        vehicleStatusTableModel.addRow(new Object[]{103, "Loc-24", 43.2, 38.9, 78.2, "Delivering", "Loc-27"});

        // Update performance indicators
        totalDistanceLabel.setText("136.6 km");
        totalCostLabel.setText("$237.6");
        totalTimeLabel.setText("7.5 hours");
        deliveredOrdersLabel.setText("2/5");
        pendingOrdersLabel.setText("3");
    }

    /**
     * Create a new tab for detailed order status monitoring
     */
    private void createOrderMonitoringPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create a split pane for different order categories
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        // Panel for orders being delivered
        JPanel activeOrdersPanel = new JPanel(new BorderLayout());
        activeOrdersPanel.setBorder(BorderFactory.createTitledBorder("Orders In Transit"));

        // Create table for active orders with detailed condition monitoring
        String[] activeColumns = {"Order ID", "Vehicle", "ETA", "Priority",
                "Temperature", "Pressure", "Expiry", "Status"};
        DefaultTableModel activeOrdersModel = new DefaultTableModel(activeColumns, 0);
        JTable activeOrdersTable = new JTable(activeOrdersModel);

        // Add some sample data
        activeOrdersModel.addRow(new Object[]{"ORD-002", "102", "10:45", "Medium",
                "5.7°C \u2713", "0.98 atm \u2713", "30 days \u2713", "Normal"});
        activeOrdersModel.addRow(new Object[]{"ORD-006", "103", "12:15", "High",
                "8.2°C \u26A0", "1.05 atm \u2713", "12 days \u26A0", "Warning"});

        activeOrdersPanel.add(new JScrollPane(activeOrdersTable), BorderLayout.CENTER);

        // Panel for order history and status changes
        JPanel orderHistoryPanel = new JPanel(new BorderLayout());
        orderHistoryPanel.setBorder(BorderFactory.createTitledBorder("Order History & Condition Logs"));

        // Create table for order history with condition log
        String[] historyColumns = {"Time", "Order ID", "Event", "Temperature", "Pressure", "Action Taken"};
        DefaultTableModel historyModel = new DefaultTableModel(historyColumns, 0);
        JTable historyTable = new JTable(historyModel);

        // Add some sample history data
        historyModel.addRow(new Object[]{"09:15", "ORD-001", "Delivered", "4.2°C", "1.01 atm", "None"});
        historyModel.addRow(new Object[]{"09:30", "ORD-004", "Delivered", "3.9°C", "1.00 atm", "None"});
        historyModel.addRow(new Object[]{"10:05", "ORD-002", "Temperature Alert", "9.1°C", "0.98 atm", "Cooling system activated"});
        historyModel.addRow(new Object[]{"10:15", "ORD-006", "Loaded", "8.2°C", "1.05 atm", "Added ice packs"});
        historyModel.addRow(new Object[]{"10:25", "ORD-002", "Temperature Normalized", "5.7°C", "0.98 atm", "None"});

        orderHistoryPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        // Add panels to split pane
        splitPane.setTopComponent(activeOrdersPanel);
        splitPane.setBottomComponent(orderHistoryPanel);

        // Add controls for monitoring settings
        JPanel controlPanel = new JPanel();
        controlPanel.setBorder(BorderFactory.createTitledBorder("Monitoring Controls"));

        JCheckBox autoAlertCheckbox = new JCheckBox("Enable Auto Alerts", true);
        JComboBox<String> refreshRateCombo = new JComboBox<>(new String[]{"5 sec", "15 sec", "30 sec", "1 min", "5 min"});
        JButton refreshButton = new JButton("Refresh Now");

        controlPanel.add(new JLabel("Refresh Rate:"));
        controlPanel.add(refreshRateCombo);
        controlPanel.add(autoAlertCheckbox);
        controlPanel.add(refreshButton);

        // Add components to main panel
        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(controlPanel, BorderLayout.SOUTH);

        // Add as new tab
        tabbedPane.addTab("Order Monitoring", panel);
    }

    /**
     * Create a customized table cell renderer for condition status cells
     */
    private class ConditionStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value != null) {
                String text = value.toString();

                // Check for temperature values
                if (column == 8 && text.contains("°C")) {
                    double temp = Double.parseDouble(text.replace("°C", ""));
                    if (temp < 2 || temp > 8) {
                        c.setForeground(Color.RED);
                    } else if (temp < 3 || temp > 7) {
                        c.setForeground(Color.ORANGE);
                    } else {
                        c.setForeground(Color.GREEN);
                    }
                }

                // Check for pressure values
                else if (column == 9) {
                    double pressure = Double.parseDouble(text);
                    if (pressure < 0.95 || pressure > 1.05) {
                        c.setForeground(Color.ORANGE);
                    } else {
                        c.setForeground(Color.GREEN);
                    }
                }

                // Check for expiry dates
                else if (column == 10 && text.contains("days")) {
                    int days = Integer.parseInt(text.replace(" days", ""));
                    if (days < 15) {
                        c.setForeground(Color.RED);
                    } else if (days < 30) {
                        c.setForeground(Color.ORANGE);
                    } else {
                        c.setForeground(Color.GREEN);
                    }
                }
            }

            return c;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            OutputInterface outputInterface = new OutputInterface();
            outputInterface.setVisible(true);
        });
    }
}