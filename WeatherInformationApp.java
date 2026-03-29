package application;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;

/**
 * ============================================================================
 * MAIN CLASS - Weather Information App (Swing Version)
 * ============================================================================
 * 
 * Uses Java Swing for GUI - compatible with ALL JDK versions [[3]], [[4]].
 * Swing is part of Java SE and requires no additional modules [[7]].
 */
public class WeatherInformationApp extends JFrame {
    
    // =========================================================================
    // UI COMPONENTS - Swing Controls [[3]], [[4]]
    // =========================================================================
    
    private JTextField locationInput;
    private JButton searchButton;
    private JLabel temperatureLabel;
    private JLabel humidityLabel;
    private JLabel windSpeedLabel;
    private JLabel conditionLabel;
    private JLabel weatherIconLabel;
    private JLabel forecastLabel;
    private JList<String> historyList;
    private DefaultListModel<String> historyModel;
    private JComboBox<String> unitCombo;
    private JPanel mainPanel;
    private JLabel titleLabel;
    
    // =========================================================================
    // DATA MANAGEMENT
    // =========================================================================
    
    private WeatherAPI weatherAPI;
    private SearchHistory searchHistory;
    private boolean isCelsius = true;
    private boolean isMetricWind = true;
    private WeatherData currentWeatherData;
    
    // =========================================================================
    // DYNAMIC BACKGROUND COLORS (Requirement #9)
    // =========================================================================
    
    private static final Color MORNING_COLOR = new Color(135, 206, 235);
    private static final Color AFTERNOON_COLOR = new Color(255, 215, 0);
    private static final Color EVENING_COLOR = new Color(255, 99, 71);
    private static final Color NIGHT_COLOR = new Color(25, 25, 112);
    
    // =========================================================================
    // API CONFIGURATION [[5]], [[9]]
    // =========================================================================
    
    // IMPORTANT: Replace with your own API key from https://openweathermap.org/api
    private static final String API_KEY = "d74afb52c569af9ee6dca65d042bde3b";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    
    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    
    /**
     * Constructor - initializes the application window and all components.
     * Swing applications extend JFrame for the main window [[3]], [[4]].
     */
    public WeatherInformationApp() {
        // Initialize API handler and history manager
        weatherAPI = new WeatherAPI();
        searchHistory = new SearchHistory();
        
        // Configure main window
        setTitle("🌤️ Weather Information App");
        setSize(800, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen
        setMinimumSize(new Dimension(700, 500));
        
        // Create and add main layout
        mainPanel = createMainLayout();
        add(mainPanel);
        
        // Apply dynamic background
        updateBackground();
        
        // Update background every hour
        startBackgroundTimer();
        
        // Make window visible
        setVisible(true);
    }
    
    // =========================================================================
    // GUI LAYOUT CREATION [[3]], [[4]]
    // =========================================================================
    
    /**
     * Creates the main application layout with all UI components.
     * Uses BorderLayout and nested panels for organization [[3]], [[4]].
     */
    private JPanel createMainLayout() {
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Gradient background
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(
                    0, 0, getBackground(),
                    0, getHeight(), Color.WHITE);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // ---------------------------------------------------------------------
        // SECTION 1: Title
        // ---------------------------------------------------------------------
        titleLabel = new JLabel("🌤️ Weather Information App", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
        
        // ---------------------------------------------------------------------
        // SECTION 2: Search Panel
        // ---------------------------------------------------------------------
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        searchPanel.setOpaque(false);
        
        locationInput = new JTextField(25);
        locationInput.setFont(new Font("Arial", Font.PLAIN, 14));
        locationInput.setPreferredSize(new Dimension(300, 35));
        locationInput.setToolTipText("Enter city name (e.g., London, New York)");
        
        searchButton = new JButton("🔍 Search");
        searchButton.setFont(new Font("Arial", Font.PLAIN, 14));
        searchButton.setBackground(new Color(76, 175, 80));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchButton.addActionListener(e -> searchWeather());
        
        unitCombo = new JComboBox<>(new String[]{"°C / km/h", "°F / mph"});
        unitCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        unitCombo.addActionListener(e -> handleUnitChange());
        
        searchPanel.add(locationInput);
        searchPanel.add(searchButton);
        searchPanel.add(unitCombo);
        
        // Allow Enter key to search
        locationInput.addActionListener(e -> searchWeather());
        
        // ---------------------------------------------------------------------
        // SECTION 3: Weather Display Panel
        // ---------------------------------------------------------------------
        JPanel weatherPanel = new JPanel();
        weatherPanel.setLayout(new BoxLayout(weatherPanel, BoxLayout.Y_AXIS));
        weatherPanel.setBackground(new Color(255, 255, 255, 200));
        weatherPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        weatherPanel.setMaximumSize(new Dimension(600, 350));
        weatherPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Weather icon
        weatherIconLabel = new JLabel();
        weatherIconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        weatherIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        weatherIconLabel.setPreferredSize(new Dimension(100, 100));
        
        // Temperature (large)
        temperatureLabel = new JLabel("--°C", SwingConstants.CENTER);
        temperatureLabel.setFont(new Font("Arial", Font.BOLD, 48));
        temperatureLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Condition
        conditionLabel = new JLabel("Search for a city to see weather", SwingConstants.CENTER);
        conditionLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        conditionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Humidity and Wind
        humidityLabel = new JLabel("💧 Humidity: --%");
        humidityLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        humidityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        windSpeedLabel = new JLabel("💨 Wind: -- km/h");
        windSpeedLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        windSpeedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Forecast
        forecastLabel = new JLabel("");
        forecastLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        forecastLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        forecastLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        weatherPanel.add(weatherIconLabel);
        weatherPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        weatherPanel.add(temperatureLabel);
        weatherPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        weatherPanel.add(conditionLabel);
        weatherPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        weatherPanel.add(humidityLabel);
        weatherPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        weatherPanel.add(windSpeedLabel);
        weatherPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        weatherPanel.add(forecastLabel);
        
        // ---------------------------------------------------------------------
        // SECTION 4: History Panel (Requirement #8)
        // ---------------------------------------------------------------------
        JLabel historyTitle = new JLabel("📜 Recent Search History");
        historyTitle.setFont(new Font("Arial", Font.BOLD, 16));
        historyTitle.setForeground(Color.WHITE);
        historyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        historyTitle.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setFont(new Font("Arial", Font.PLAIN, 13));
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setBackground(new Color(255, 255, 255, 200));
        historyList.setPreferredSize(new Dimension(500, 100));
        historyList.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Click to reload search
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !searchButton.isEnabled()) {
                    String selected = historyList.getSelectedValue();
                    if (selected != null) {
                        String city = selected.split(" - ")[0];
                        locationInput.setText(city);
                        searchWeather();
                    }
                }
            }
        });
        
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setPreferredSize(new Dimension(500, 100));
        historyScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        historyScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        
        // Clear history button
        JButton clearHistoryBtn = new JButton("🗑️ Clear History");
        clearHistoryBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        clearHistoryBtn.setBackground(new Color(244, 67, 54));
        clearHistoryBtn.setForeground(Color.WHITE);
        clearHistoryBtn.setFocusPainted(false);
        clearHistoryBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearHistoryBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearHistoryBtn.addActionListener(e -> clearHistory());
        
        // ---------------------------------------------------------------------
        // SECTION 5: Footer
        // ---------------------------------------------------------------------
        JLabel footerLabel = new JLabel("💡 Tip: Press Enter to search | Double-click history to reload");
        footerLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        footerLabel.setForeground(Color.WHITE);
        footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        footerLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
        
        // ---------------------------------------------------------------------
        // ASSEMBLE ALL COMPONENTS
        // ---------------------------------------------------------------------
        mainPanel.add(titleLabel);
        mainPanel.add(searchPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(weatherPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(historyTitle);
        mainPanel.add(historyScroll);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(clearHistoryBtn);
        mainPanel.add(footerLabel);
        
        return mainPanel;
    }
    
    // =========================================================================
    // WEATHER SEARCH FUNCTIONALITY
    // =========================================================================
    
    /**
     * Initiates weather search for the entered location.
     * Implements input validation and error handling (Requirement #7) [[7]].
     */
    private void searchWeather() {
        String location = locationInput.getText().trim();
        
        // INPUT VALIDATION (Requirement #7) [[7]]
        if (location.isEmpty()) {
            showAlert("⚠️ Input Error", "Please enter a city name to search.", JOptionPane.WARNING_MESSAGE);
            locationInput.requestFocus();
            return;
        }
        
        if (!location.matches("^[a-zA-Z\\s,-]+$")) {
            showAlert("⚠️ Invalid Format", 
                "Invalid location format. Use only letters, spaces, commas, or hyphens.", 
                JOptionPane.WARNING_MESSAGE);
            locationInput.requestFocus();
            return;
        }
        
        if (location.length() < 2) {
            showAlert("⚠️ Input Too Short", "Please enter at least 2 characters.", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // DISABLE UI DURING SEARCH
        searchButton.setEnabled(false);
        searchButton.setText("⏳ Searching...");
        locationInput.setEnabled(false);
        
        // BACKGROUND THREAD FOR API CALL
        SwingWorker<WeatherData, Void> worker = new SwingWorker<WeatherData, Void>() {
            @Override
            protected WeatherData doInBackground() throws Exception {
                return weatherAPI.getCurrentWeather(location);
            }
            
            @Override
            protected void done() {
                try {
                    WeatherData data = get();
                    
                    if (data != null && data.getCityName() != null) {
                        displayWeather(data, location);
                        addToHistory(location);
                        titleLabel.setText("🌤️ Weather: " + data.getCityName());
                    } else {
                        showAlert("❌ Not Found", 
                            "City '" + location + "' not found. Check spelling.", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    showAlert("❌ Connection Error", 
                        "Failed to connect. Check internet connection.", 
                        JOptionPane.ERROR_MESSAGE);
                }
                
                // RE-ENABLE UI
                searchButton.setEnabled(true);
                searchButton.setText("🔍 Search");
                locationInput.setEnabled(true);
                locationInput.requestFocus();
            }
        };
        
        worker.execute();
    }
    
    // =========================================================================
    // WEATHER DISPLAY
    // =========================================================================
    
    /**
     * Displays weather data in the GUI components.
     */
    private void displayWeather(WeatherData data, String location) {
        currentWeatherData = data;
        
        // Temperature conversion
        double temp = isCelsius ? data.getTemperature() : convertToFahrenheit(data.getTemperature());
        String tempUnit = isCelsius ? "°C" : "°F";
        
        // Wind conversion
        double wind = isMetricWind ? data.getWindSpeed() : convertToMph(data.getWindSpeed());
        String windUnit = isMetricWind ? "km/h" : "mph";
        
        // Update labels
        temperatureLabel.setText(String.format("%.1f%s", temp, tempUnit));
        conditionLabel.setText(data.getCondition().toUpperCase());
        humidityLabel.setText("💧 Humidity: " + data.getHumidity() + "%");
        windSpeedLabel.setText("💨 Wind: " + String.format("%.1f %s", wind, windUnit));
        
        // Update icon
        updateWeatherIcon(data.getIconCode());
        
        // Update forecast
        updateForecastDisplay(data);
    }
    
    /**
     * Updates weather icon from OpenWeatherMap [[5]].
     */
    private void updateWeatherIcon(String iconCode) {
        if (iconCode == null || iconCode.isEmpty()) {
            weatherIconLabel.setIcon(null);
            return;
        }
        
        String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
        
        SwingWorker<BufferedImage, Void> iconLoader = new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                URL url = new URL(iconUrl);
                return ImageIO.read(url);
            }
            
            @Override
            protected void done() {
                try {
                    BufferedImage icon = get();
                    if (icon != null) {
                        Image scaled = icon.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                        weatherIconLabel.setIcon(new ImageIcon(scaled));
                    }
                } catch (Exception e) {
                    weatherIconLabel.setIcon(null);
                }
            }
        };
        iconLoader.execute();
    }
    
    /**
     * Updates forecast label.
     */
    private void updateForecastDisplay(WeatherData data) {
        StringBuilder forecast = new StringBuilder();
        forecast.append("📍 Location: ").append(data.getCityName()).append(" | ");
        forecast.append("🕐 Updated: ").append(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        
        if (data.getFeelsLike() != null) {
            double feelsLike = isCelsius ? data.getFeelsLike() : convertToFahrenheit(data.getFeelsLike());
            forecast.append(" | 🌡️ Feels Like: ").append(String.format("%.1f%s", feelsLike, 
                isCelsius ? "°C" : "°F"));
        }
        
        forecastLabel.setText(forecast.toString());
    }
    
    // =========================================================================
    // UNIT CONVERSION
    // =========================================================================
    
    private void handleUnitChange() {
        String selected = (String) unitCombo.getSelectedItem();
        
        if (selected != null) {
            isCelsius = selected.equals("°C / km/h");
            isMetricWind = selected.equals("°C / km/h");
            
            // Refresh if we have data
            if (currentWeatherData != null) {
                displayWeather(currentWeatherData, currentWeatherData.getCityName());
            }
        }
    }
    
    private double convertToFahrenheit(double celsius) {
        return (celsius * 9.0 / 5.0) + 32.0;
    }
    
    private double convertToMph(double kmh) {
        return kmh * 0.621371;
    }
    
    // =========================================================================
    // HISTORY MANAGEMENT (Requirement #8)
    // =========================================================================
    
    private void addToHistory(String location) {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        searchHistory.addSearch(location, timestamp);
        saveHistory();
        loadHistory();
    }
    
    private void loadHistory() {
        historyModel.clear();
        List<String> history = searchHistory.getHistory();
        int startIndex = Math.max(0, history.size() - 10);
        for (int i = startIndex; i < history.size(); i++) {
            historyModel.addElement(history.get(i));
        }
    }
    
    private void saveHistory() {
        searchHistory.saveToFile("weather_history.txt");
    }
    
    private void clearHistory() {
        int response = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to clear all search history?",
            "Clear History", JOptionPane.YES_NO_OPTION);
        
        if (response == JOptionPane.YES_OPTION) {
            searchHistory.clearHistory();
            loadHistory();
            showAlert("✓ History Cleared", "All search history has been cleared.", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    // =========================================================================
    // DYNAMIC BACKGROUNDS (Requirement #9)
    // =========================================================================
    
    private void updateBackground() {
        int hour = LocalDateTime.now().getHour();
        Color bgColor;
        
        if (hour >= 5 && hour < 12) {
            bgColor = MORNING_COLOR;
        } else if (hour >= 12 && hour < 17) {
            bgColor = AFTERNOON_COLOR;
        } else if (hour >= 17 && hour < 21) {
            bgColor = EVENING_COLOR;
        } else {
            bgColor = NIGHT_COLOR;
        }
        
        mainPanel.setBackground(bgColor);
        mainPanel.repaint();
    }
    
    private void startBackgroundTimer() {
        Timer timer = new Timer(3600000, e -> updateBackground()); // Every hour
        timer.start();
    }
    
    // =========================================================================
    // ERROR HANDLING (Requirement #7) [[7]]
    // =========================================================================
    
    private void showAlert(String title, String message, int messageType) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(this, message, title, messageType));
    }
    
    // =========================================================================
    // MAIN METHOD
    // =========================================================================
    
    public static void main(String[] args) {
        // Set look and feel for better appearance [[3]], [[4]]
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Run on Event Dispatch Thread (Swing best practice) [[3]]
        SwingUtilities.invokeLater(() -> new WeatherInformationApp());
    }
    
    // =========================================================================
    // INNER CLASS: WeatherData
    // =========================================================================
    
    static class WeatherData {
        private String cityName;
        private double temperature;
        private Double feelsLike;
        private int humidity;
        private double windSpeed;
        private String condition;
        private String iconCode;
        private int pressure;
        private int visibility;
        
        public WeatherData() {}
        
        // Getters
        public String getCityName() { return cityName; }
        public double getTemperature() { return temperature; }
        public Double getFeelsLike() { return feelsLike; }
        public int getHumidity() { return humidity; }
        public double getWindSpeed() { return windSpeed; }
        public String getCondition() { return condition; }
        public String getIconCode() { return iconCode; }
        public int getPressure() { return pressure; }
        public int getVisibility() { return visibility; }
        
        // Setters
        public void setCityName(String cityName) { this.cityName = cityName; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public void setFeelsLike(Double feelsLike) { this.feelsLike = feelsLike; }
        public void setHumidity(int humidity) { this.humidity = humidity; }
        public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
        public void setCondition(String condition) { this.condition = condition; }
        public void setIconCode(String iconCode) { this.iconCode = iconCode; }
        public void setPressure(int pressure) { this.pressure = pressure; }
        public void setVisibility(int visibility) { this.visibility = visibility; }
    }
    
    // =========================================================================
    // INNER CLASS: WeatherAPI
    // =========================================================================
    
    static class WeatherAPI {
        
        public WeatherData getCurrentWeather(String location) {
            try {
                if (API_KEY.equals("YOUR_API_KEY_HERE") || API_KEY.isEmpty()) {
                    System.err.println("ERROR: API Key not configured!");
                    return null;
                }
                
                String encodedLocation = URLEncoder.encode(location, "UTF-8");
                String apiUrl = BASE_URL + "/weather?q=" + encodedLocation + 
                               "&appid=" + API_KEY + "&units=metric";
                
                String jsonResponse = makeApiRequest(apiUrl);
                
                if (jsonResponse == null || jsonResponse.isEmpty()) {
                    return null;
                }
                
                return parseWeatherResponse(jsonResponse);
                
            } catch (Exception e) {
                System.err.println("API Error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        
        private String makeApiRequest(String urlString) throws Exception {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "WeatherApp/1.0");
            connection.setRequestProperty("Accept", "application/json");
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();
                
                return response.toString();
            } else {
                System.err.println("API Error: HTTP " + responseCode);
                return null;
            }
        }
        
        private WeatherData parseWeatherResponse(String jsonResponse) {
            try {
                JSONObject json = new JSONObject(jsonResponse);
                
                if (json.has("cod")) {
                    int code = json.getInt("cod");
                    if (code != 200) {
                        return null;
                    }
                }
                
                WeatherData data = new WeatherData();
                data.setCityName(json.getString("name"));
                
                if (json.has("main")) {
                    JSONObject main = json.getJSONObject("main");
                    data.setTemperature(main.getDouble("temp"));
                    data.setHumidity(main.getInt("humidity"));
                    
                    if (main.has("feels_like")) {
                        data.setFeelsLike(main.getDouble("feels_like"));
                    }
                    if (main.has("pressure")) {
                        data.setPressure(main.getInt("pressure"));
                    }
                }
                
                if (json.has("wind")) {
                    JSONObject wind = json.getJSONObject("wind");
                    data.setWindSpeed(wind.getDouble("speed"));
                }
                
                if (json.has("weather") && json.getJSONArray("weather").length() > 0) {
                    JSONArray weather = json.getJSONArray("weather");
                    JSONObject weatherCondition = weather.getJSONObject(0);
                    data.setCondition(weatherCondition.getString("description"));
                    data.setIconCode(weatherCondition.getString("icon"));
                }
                
                if (json.has("visibility")) {
                    data.setVisibility(json.getInt("visibility"));
                }
                
                return data;
                
            } catch (Exception e) {
                System.err.println("JSON Parse Error: " + e.getMessage());
                return null;
            }
        }
    }
    
    // =========================================================================
    // INNER CLASS: SearchHistory
    // =========================================================================
    
    static class SearchHistory {
        
        private List<String> history;
        private static final int MAX_HISTORY = 20;
        private static final String HISTORY_FILE = "weather_history.txt";
        
        public SearchHistory() {
            history = new ArrayList<>();
            loadFromFile();
        }
        
        public void addSearch(String location, String timestamp) {
            String entry = location + " - " + timestamp;
            
            if (!history.isEmpty() && history.get(0).equals(entry)) {
                return;
            }
            
            history.add(0, entry);
            
            if (history.size() > MAX_HISTORY) {
                history = new ArrayList<>(history.subList(0, MAX_HISTORY));
            }
        }
        
        public List<String> getHistory() {
            return new ArrayList<>(history);
        }
        
        public void saveToFile(String filename) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                for (String entry : history) {
                    writer.println(entry);
                }
            } catch (IOException e) {
                System.err.println("Error saving history: " + e.getMessage());
            }
        }
        
        public void saveToFile() {
            saveToFile(HISTORY_FILE);
        }
        
        private void loadFromFile() {
            try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        history.add(line);
                    }
                }
            } catch (FileNotFoundException e) {
                // Normal for first run
            } catch (IOException e) {
                System.err.println("Error loading history: " + e.getMessage());
            }
        }
        
        public void clearHistory() {
            history.clear();
            saveToFile();
        }
    }
}
