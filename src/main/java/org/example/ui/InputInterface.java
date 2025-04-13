package org.example.ui;

import org.example.models.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * UI class for input data for the CVRP model
 */
public class InputInterface extends JFrame {
    private List<Location> locations = new ArrayList<>();
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();

    // UI Components
    private JTabbedPane tabbedPane;
    private JTable locationTable;
    private JTable vehicleTable;
    private JTable productTable;
    private JTable orderTable;
    private DefaultTableModel locationModel;
    private DefaultTableModel vehicleModel;
    private DefaultTableModel productModel;
    private DefaultTableModel orderModel;

    // Input fields
    private JTextField numLocationsField;
    private JTextField numVehiclesField;
    private JTextField numRefuelingStationsField;

    public InputInterface() {
        setTitle("CVRP Input Interface");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Create tabs
        createControlPanel();
        createLocationPanel();
        createVehiclePanel();
        createProductPanel();
        createOrderPanel();

        // Add tabbed pane to frame
        add(tabbedPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save Data");
        JButton loadButton = new JButton("Load Data");
        JButton runButton = new JButton("Run Optimization");

        saveButton.addActionListener(e -> saveData());
        loadButton.addActionListener(e -> loadData());
        runButton.addActionListener(e -> runOptimization());

        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(runButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Create control panel with global settings
     */
    private void createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Problem Configuration"));

        inputPanel.add(new JLabel("Number of Delivery Locations:"));
        numLocationsField = new JTextField("10");
        inputPanel.add(numLocationsField);

        inputPanel.add(new JLabel("Number of Vehicles:"));
        numVehiclesField = new JTextField("3");
        inputPanel.add(numVehiclesField);

        inputPanel.add(new JLabel("Number of Refueling Stations:"));
        numRefuelingStationsField = new JTextField("2");
        inputPanel.add(numRefuelingStationsField);

        inputPanel.add(new JLabel("Import Data from File:"));
        JButton importButton = new JButton("Select File");
        importButton.addActionListener(e -> importDataFromFile());
        inputPanel.add(importButton);

        inputPanel.add(new JLabel("Generate Random Data:"));
        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(e -> generateRandomData());
        inputPanel.add(generateButton);

        panel.add(inputPanel, BorderLayout.NORTH);

        // Help text
        JTextArea helpText = new JTextArea(
                "This interface allows you to set up the CVRP problem instance.\n\n" +
                        "You can either input data manually in the various tabs, import from a file, " +
                        "or generate random data for testing.\n\n" +
                        "Once your data is set up, click 'Run Optimization' to start the algorithm."
        );
        helpText.setEditable(false);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        helpText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(helpText);
        panel.add(scrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Control Panel", panel);
    }

    /**
     * Create location input panel
     */
    private void createLocationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table model
        String[] columnNames = {"ID", "X", "Y", "Demand", "Earliest Time", "Latest Time", "Service Time", "Type"};
        locationModel = new DefaultTableModel(columnNames, 0);
        locationTable = new JTable(locationModel);

        JScrollPane scrollPane = new JScrollPane(locationTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Location");
        JButton deleteButton = new JButton("Delete Selected");

        addButton.addActionListener(e -> addLocation());
        deleteButton.addActionListener(e -> deleteSelectedLocation());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Locations", panel);
    }

    /**
     * Create vehicle input panel
     */
    private void createVehiclePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table model
        String[] columnNames = {"ID", "Speed", "Max Fuel", "Min Fuel", "Capacity",
                "Base Consumption", "Payload Coefficient", "Refueling Time", "Refueling Cost"};
        vehicleModel = new DefaultTableModel(columnNames, 0);
        vehicleTable = new JTable(vehicleModel);

        JScrollPane scrollPane = new JScrollPane(vehicleTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Vehicle");
        JButton deleteButton = new JButton("Delete Selected");

        addButton.addActionListener(e -> addVehicle());
        deleteButton.addActionListener(e -> deleteSelectedVehicle());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Vehicles", panel);
    }

    /**
     * Create product input panel
     */
    private void createProductPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table model
        String[] columnNames = {"ID", "Name", "Weight", "Required Temp", "Temp Tolerance",
                "Required Pressure", "Pressure Tolerance", "Expiry Date", "Priority"};
        productModel = new DefaultTableModel(columnNames, 0);
        productTable = new JTable(productModel);

        JScrollPane scrollPane = new JScrollPane(productTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Product");
        JButton deleteButton = new JButton("Delete Selected");

        addButton.addActionListener(e -> addProduct());
        deleteButton.addActionListener(e -> deleteSelectedProduct());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Products", panel);
    }

    /**
     * Create order input panel
     */
    private void createOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table model
        String[] columnNames = {"Order ID", "Location ID", "Order Time", "Delivery Deadline",
                "Products Count", "Total Weight", "Priority"};
        orderModel = new DefaultTableModel(columnNames, 0);
        orderTable = new JTable(orderModel);

        JScrollPane scrollPane = new JScrollPane(orderTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Order");
        JButton deleteButton = new JButton("Delete Selected");
        JButton assignButton = new JButton("Assign Products");

        addButton.addActionListener(e -> addOrder());
        deleteButton.addActionListener(e -> deleteSelectedOrder());
        assignButton.addActionListener(e -> assignProductsToOrder());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(assignButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Orders", panel);
    }

    /**
     * Add a new location (placeholder)
     */
    private void addLocation() {
        // Create input dialog
        JPanel inputPanel = new JPanel(new GridLayout(8, 2));

        JTextField idField = new JTextField();
        JTextField xField = new JTextField();
        JTextField yField = new JTextField();
        JTextField demandField = new JTextField();
        JTextField earliestTimeField = new JTextField();
        JTextField latestTimeField = new JTextField();
        JTextField serviceTimeField = new JTextField();
        JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"customer", "depot", "refueling_station"});

        inputPanel.add(new JLabel("ID:"));
        inputPanel.add(idField);
        inputPanel.add(new JLabel("X Coordinate:"));
        inputPanel.add(xField);
        inputPanel.add(new JLabel("Y Coordinate:"));
        inputPanel.add(yField);
        inputPanel.add(new JLabel("Demand:"));
        inputPanel.add(demandField);
        inputPanel.add(new JLabel("Earliest Time:"));
        inputPanel.add(earliestTimeField);
        inputPanel.add(new JLabel("Latest Time:"));
        inputPanel.add(latestTimeField);
        inputPanel.add(new JLabel("Service Time:"));
        inputPanel.add(serviceTimeField);
        inputPanel.add(new JLabel("Type:"));
        inputPanel.add(typeComboBox);

        int result = JOptionPane.showConfirmDialog(this, inputPanel,
                "Add Location", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int id = Integer.parseInt(idField.getText());
                double x = Double.parseDouble(xField.getText());
                double y = Double.parseDouble(yField.getText());
                double demand = Double.parseDouble(demandField.getText());
                double earliestTime = Double.parseDouble(earliestTimeField.getText());
                double latestTime = Double.parseDouble(latestTimeField.getText());
                double serviceTime = Double.parseDouble(serviceTimeField.getText());
                String type = (String) typeComboBox.getSelectedItem();

                // Add to table
                locationModel.addRow(new Object[]{id, x, y, demand, earliestTime, latestTime, serviceTime, type});

                // Create location object
                Location location = new Location(id, x, y, demand, new double[]{earliestTime, latestTime},
                        serviceTime, type);
                locations.add(location);

                JOptionPane.showMessageDialog(this, "Location added successfully!");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input. Please check your values.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Delete selected location (placeholder)
     */
    private void deleteSelectedLocation() {
        int selectedRow = locationTable.getSelectedRow();
        if (selectedRow >= 0) {
            int id = (int) locationModel.getValueAt(selectedRow, 0);

            // Remove from model
            locationModel.removeRow(selectedRow);

            // Remove from list
            locations.removeIf(location -> location.getId() == id);

            JOptionPane.showMessageDialog(this, "Location deleted!");
        } else {
            JOptionPane.showMessageDialog(this, "Please select a location to delete.");
        }
    }

    /**
     * Add a new vehicle (placeholder)
     */
    private void addVehicle() {
        // Create input dialog
        JPanel inputPanel = new JPanel(new GridLayout(9, 2));

        JTextField idField = new JTextField();
        JTextField speedField = new JTextField();
        JTextField maxFuelField = new JTextField();
        JTextField minFuelField = new JTextField();
        JTextField capacityField = new JTextField();
        JTextField baseConsumptionField = new JTextField();
        JTextField payloadCoefficientField = new JTextField();
        JTextField refuelingTimeField = new JTextField();
        JTextField refuelingCostField = new JTextField();

        inputPanel.add(new JLabel("ID:"));
        inputPanel.add(idField);
        inputPanel.add(new JLabel("Speed (km/h):"));
        inputPanel.add(speedField);
        inputPanel.add(new JLabel("Max Fuel Tank:"));
        inputPanel.add(maxFuelField);
        inputPanel.add(new JLabel("Min Fuel Tank:"));
        inputPanel.add(minFuelField);
        inputPanel.add(new JLabel("Payload Capacity:"));
        inputPanel.add(capacityField);
        inputPanel.add(new JLabel("Base Energy Consumption:"));
        inputPanel.add(baseConsumptionField);
        inputPanel.add(new JLabel("Payload Energy Coefficient:"));
        inputPanel.add(payloadCoefficientField);
        inputPanel.add(new JLabel("Refueling Time:"));
        inputPanel.add(refuelingTimeField);
        inputPanel.add(new JLabel("Refueling Cost:"));
        inputPanel.add(refuelingCostField);

        int result = JOptionPane.showConfirmDialog(this, inputPanel,
                "Add Vehicle", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int id = Integer.parseInt(idField.getText());
                double speed = Double.parseDouble(speedField.getText());
                double maxFuel = Double.parseDouble(maxFuelField.getText());
                double minFuel = Double.parseDouble(minFuelField.getText());
                double capacity = Double.parseDouble(capacityField.getText());
                double baseConsumption = Double.parseDouble(baseConsumptionField.getText());
                double payloadCoefficient = Double.parseDouble(payloadCoefficientField.getText());
                double refuelingTime = Double.parseDouble(refuelingTimeField.getText());
                double refuelingCost = Double.parseDouble(refuelingCostField.getText());

                // Add to table
                vehicleModel.addRow(new Object[]{id, speed, maxFuel, minFuel, capacity,
                        baseConsumption, payloadCoefficient, refuelingTime, refuelingCost});

                // Create vehicle object
                Vehicle vehicle = new Vehicle(id, speed, maxFuel, minFuel, capacity,
                        baseConsumption, payloadCoefficient,
                        refuelingTime, refuelingCost);
                vehicles.add(vehicle);

                JOptionPane.showMessageDialog(this, "Vehicle added successfully!");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input. Please check your values.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Delete selected vehicle (placeholder)
     */
    private void deleteSelectedVehicle() {
        int selectedRow = vehicleTable.getSelectedRow();
        if (selectedRow >= 0) {
            int id = (int) vehicleModel.getValueAt(selectedRow, 0);

            // Remove from model
            vehicleModel.removeRow(selectedRow);

            // Remove from list
            vehicles.removeIf(vehicle -> vehicle.getId() == id);

            JOptionPane.showMessageDialog(this, "Vehicle deleted!");
        } else {
            JOptionPane.showMessageDialog(this, "Please select a vehicle to delete.");
        }
    }

    /**
     * Add a new product (placeholder)
     */
    private void addProduct() {
        // Implement adding a product
    }

    /**
     * Delete selected product (placeholder)
     */
    private void deleteSelectedProduct() {
        // Implement deleting a product
    }

    /**
     * Add a new order (placeholder)
     */
    private void addOrder() {
        // Implement adding an order
    }

    /**
     * Delete selected order (placeholder)
     */
    private void deleteSelectedOrder() {
        // Implement deleting an order
    }

    /**
     * Assign products to an order (placeholder)
     */
    private void assignProductsToOrder() {
        // Implement assigning products to an order
    }

    /**
     * Import data from file (placeholder)
     */
    private void importDataFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Data File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            JOptionPane.showMessageDialog(this, "Importing data from: " + file.getName());
            // Implement actual data import
        }
    }

    /**
     * Generate random data for testing (placeholder)
     */
    private void generateRandomData() {
        try {
            int numLocations = Integer.parseInt(numLocationsField.getText());
            int numVehicles = Integer.parseInt(numVehiclesField.getText());
            int numStations = Integer.parseInt(numRefuelingStationsField.getText());

            // Clear existing data
            locations.clear();
            vehicles.clear();
            locationModel.setRowCount(0);
            vehicleModel.setRowCount(0);

            // Generate depot
            Location depot = new Location(0, 0, 0, 0, new double[]{0, 24}, 0, "depot");
            locations.add(depot);
            locationModel.addRow(new Object[]{0, 0, 0, 0, 0, 24, 0, "depot"});

            // Generate refueling stations
            for (int i = 1; i <= numStations; i++) {
                double x = Math.random() * 100 - 50;
                double y = Math.random() * 100 - 50;
                Location station = new Location(i, x, y, 0, new double[]{0, 24}, 0, "refueling_station");
                locations.add(station);
                locationModel.addRow(new Object[]{i, x, y, 0, 0, 24, 0, "refueling_station"});
            }

            // Generate customer locations
            for (int i = numStations + 1; i <= numLocations; i++) {
                double x = Math.random() * 100 - 50;
                double y = Math.random() * 100 - 50;
                double demand = 5 + Math.random() * 20;
                double earliest = Math.random() * 12;
                double latest = earliest + 4 + Math.random() * 8;
                double serviceTime = 0.5 + Math.random() * 1.5;

                Location customer = new Location(i, x, y, demand, new double[]{earliest, latest},
                        serviceTime, "customer");
                locations.add(customer);
                locationModel.addRow(new Object[]{i, x, y, demand, earliest, latest, serviceTime, "customer"});
            }

            // Generate vehicles
            for (int i = 1; i <= numVehicles; i++) {
                double speed = 40 + Math.random() * 30;
                double maxFuel = 100 + Math.random() * 100;
                double minFuel = maxFuel * 0.1;
                double capacity = 80 + Math.random() * 100;
                double baseConsumption = 0.1 + Math.random() * 0.2;
                double payloadCoefficient = 0.001 + Math.random() * 0.003;
                double refuelingTime = 1 + Math.random() * 2;
                double refuelingCost = 10 + Math.random() * 20;

                Vehicle vehicle = new Vehicle(i, speed, maxFuel, minFuel, capacity,
                        baseConsumption, payloadCoefficient,
                        refuelingTime, refuelingCost);
                vehicles.add(vehicle);

                vehicleModel.addRow(new Object[]{i, speed, maxFuel, minFuel, capacity,
                        baseConsumption, payloadCoefficient, refuelingTime, refuelingCost});
            }

            JOptionPane.showMessageDialog(this, "Random data generated successfully!");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please enter valid numbers.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Save data to file (placeholder)
     */
    private void saveData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Data");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            // Ensure extension
            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }

            try {
                // Save location data
                saveLocations(new File(file.getParent(), "locations.txt"));

                // Save vehicle data
                saveVehicles(new File(file.getParent(), "vehicles.txt"));

                JOptionPane.showMessageDialog(this, "Data saved successfully!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving data: " + e.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Save locations to file
     */
    private void saveLocations(File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("# ID, X, Y, Demand, EarliestTime, LatestTime, ServiceTime, Type");

            for (Location location : locations) {
                writer.printf("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s%n",
                        location.getId(), location.getX(), location.getY(), location.getDemand(),
                        location.getTimeWindow()[0], location.getTimeWindow()[1],
                        location.getServiceTime(), location.getType());
            }
        }
    }

    /**
     * Save vehicles to file
     */
    private void saveVehicles(File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("# ID, Speed, MaxFuel, MinFuel, Capacity, BaseConsumption, PayloadCoefficient, RefuelingTime, RefuelingCost");

            for (Vehicle vehicle : vehicles) {
                writer.printf("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.4f,%.2f,%.2f%n",
                        vehicle.getId(), vehicle.getSpeed(), vehicle.getMaxFuelTank(),
                        vehicle.getMinFuelTank(), vehicle.getPayloadCapacity(),
                        vehicle.getBaseEnergyConsumption(), vehicle.getPayloadEnergyCoefficient(),
                        vehicle.getRefuelingTime(), vehicle.getRefuelingCost());
            }
        }
    }

    /**
     * Load data from files (placeholder)
     */
    private void loadData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Data Directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File directory = fileChooser.getSelectedFile();

            try {
                // Load location data
                File locationFile = new File(directory, "locations.txt");
                if (locationFile.exists()) {
                    loadLocations(locationFile);
                }

                // Load vehicle data
                File vehicleFile = new File(directory, "vehicles.txt");
                if (vehicleFile.exists()) {
                    loadVehicles(vehicleFile);
                }

                JOptionPane.showMessageDialog(this, "Data loaded successfully!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage(),
                        "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Load locations from file
     */
    private void loadLocations(File file) throws IOException {
        locations.clear();
        locationModel.setRowCount(0);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0]);
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double demand = Double.parseDouble(parts[3]);
                double earliest = Double.parseDouble(parts[4]);
                double latest = Double.parseDouble(parts[5]);
                double serviceTime = Double.parseDouble(parts[6]);
                String type = parts[7];

                Location location = new Location(id, x, y, demand, new double[]{earliest, latest},
                        serviceTime, type);
                locations.add(location);

                // Add to table
                locationModel.addRow(new Object[]{id, x, y, demand, earliest, latest, serviceTime, type});
            }
        }
    }

    /**
     * Load vehicles from file
     */
    private void loadVehicles(File file) throws IOException {
        vehicles.clear();
        vehicleModel.setRowCount(0);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0]);
                double speed = Double.parseDouble(parts[1]);
                double maxFuel = Double.parseDouble(parts[2]);
                double minFuel = Double.parseDouble(parts[3]);
                double capacity = Double.parseDouble(parts[4]);
                double baseConsumption = Double.parseDouble(parts[5]);
                double payloadCoefficient = Double.parseDouble(parts[6]);
                double refuelingTime = Double.parseDouble(parts[7]);
                double refuelingCost = Double.parseDouble(parts[8]);

                Vehicle vehicle = new Vehicle(id, speed, maxFuel, minFuel, capacity,
                        baseConsumption, payloadCoefficient,
                        refuelingTime, refuelingCost);
                vehicles.add(vehicle);

                // Add to table
                vehicleModel.addRow(new Object[]{id, speed, maxFuel, minFuel, capacity,
                        baseConsumption, payloadCoefficient, refuelingTime, refuelingCost});
            }
        }
    }

    /**
     * Run optimization (placeholder)
     */
    private void runOptimization() {
        if (locations.isEmpty() || vehicles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please define locations and vehicles first.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check for depot
        boolean hasDepot = locations.stream().anyMatch(loc -> loc.getType().equalsIgnoreCase("depot"));
        if (!hasDepot) {
            JOptionPane.showMessageDialog(this, "A depot location is required.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Running optimization with " +
                locations.size() + " locations and " +
                vehicles.size() + " vehicles.\n\n" +
                "This would launch the CVRP algorithm in a real implementation.");

        // This would be where we call the actual optimization algorithm
        // For now, just open the output interface
        OutputInterface outputInterface = new OutputInterface();
        outputInterface.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            InputInterface inputInterface = new InputInterface();
            inputInterface.setVisible(true);
        });
    }
}