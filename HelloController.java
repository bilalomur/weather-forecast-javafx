package org.example.project;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class HelloController {

    @FXML private TextField daysField, latField, lonField, nameField;
    @FXML private RadioButton rbLatLon, rbName;
    @FXML private CheckBox cbTemp, cbPrecip, cbPressure, cbWind;
    @FXML private TextArea infoArea;

    private static final String CACHE_DIR = "weather_cache";
    private static final long CACHE_EXPIRY_MS = 3 * 60 * 60 * 1000; // 3 hours

    @FXML
    public void initialize() {
        // Toggle field availability based on radio selection
        nameField.disableProperty().bind(rbLatLon.selectedProperty());
        latField.disableProperty().bind(rbName.selectedProperty());
        lonField.disableProperty().bind(rbName.selectedProperty());

        File dir = new File(CACHE_DIR);
        if (!dir.exists()) dir.mkdir();
    }

    @FXML
    private void handleFetch() {
        try {
            double lat, lon;
            String locationLabel;

            if (rbName.isSelected()) {
                String cityName = nameField.getText();
                // Fetch up to 5 results to show alternatives
                String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                        URLEncoder.encode(cityName, StandardCharsets.UTF_8) + "&count=5";

                String geoJson = fetchWithCache(geoUrl, "geo_" + cityName.hashCode());

                if (geoJson.contains("\"results\":")) {
                    // 1. Parse data for the primary result (the one we chart)
                    lat = Double.parseDouble(extractValue(geoJson, "latitude"));
                    lon = Double.parseDouble(extractValue(geoJson, "longitude"));
                    String mainName = extractValue(geoJson, "name");
                    String mainCountry = extractValue(geoJson, "country");
                    locationLabel = mainName + ", " + mainCountry;

                    // 2. Build the Info area text
                    StringBuilder sb = new StringBuilder();
                    sb.append("USING PRIMARY RESULT:\n");
                    sb.append(locationLabel).append(" [Lat: ").append(lat).append(", Lon: ").append(lon).append("]\n\n");
                    sb.append("OTHER POSSIBILITIES FOUND:\n");

                    // 3. Scan for up to 4 other possibilities including their coordinates
                    String key = "\"name\":\"";
                    int pos = geoJson.indexOf(key); // skip the first result
                    int altCount = 0;

                    while ((pos = geoJson.indexOf(key, pos + 1)) != -1 && altCount < 4) {
                        // Extract Name, Country, Lat, Lon for the alternative
                        String otherName = geoJson.substring(pos + 8, geoJson.indexOf("\"", pos + 8));

                        int latPos = geoJson.indexOf("\"latitude\":", pos);
                        String otherLat = extractValue(geoJson.substring(latPos), "latitude");

                        int lonPos = geoJson.indexOf("\"longitude\":", pos);
                        String otherLon = extractValue(geoJson.substring(lonPos), "longitude");

                        int countryPos = geoJson.indexOf("\"country\":\"", pos);
                        String otherCountry = "Unknown";
                        if (countryPos != -1 && countryPos < pos + 500) {
                            int start = countryPos + 11;
                            otherCountry = geoJson.substring(start, geoJson.indexOf("\"", start));
                        }

                        sb.append("- ").append(otherName).append(" (").append(otherCountry)
                                .append(") [").append(otherLat).append(", ").append(otherLon).append("]\n");
                        altCount++;
                    }

                    if (altCount == 0) sb.append("- No other alternatives found.");
                    infoArea.setText(sb.toString());

                } else {
                    infoArea.setText("Error: Location not found.");
                    return;
                }
            } else {
                // Manual coordinates logic
                lat = Double.parseDouble(latField.getText());
                lon = Double.parseDouble(lonField.getText());
                locationLabel = lat + " / " + lon;
                infoArea.setText("Using Coordinates: " + locationLabel);
            }

            // Execute the forecast fetching and charting
            processForecast(lat, lon, locationLabel);

        } catch (Exception e) {
            infoArea.setText("Error: Check your inputs. " + e.getMessage());
        }
    }

    private void processForecast(double lat, double lon, String loc) throws Exception {
        List<String> vars = new ArrayList<>();
        if (cbTemp.isSelected()) vars.add("temperature_2m");
        if (cbPrecip.isSelected()) vars.add("precipitation");
        if (cbPressure.isSelected()) vars.add("surface_pressure");
        if (cbWind.isSelected()) vars.add("wind_speed_10m");

        if (vars.isEmpty()) return;

        String varList = String.join(",", vars);
        String url = String.format("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&hourly=%s&forecast_days=%s",
                lat, lon, varList, daysField.getText());

        String fullJson = fetchWithCache(url, "f_" + (lat + "-" + lon + "-" + varList + "-" + daysField.getText()).hashCode());

        // Split JSON into Units and Data parts to prevent Surface Pressure errors
        int hourlyStart = fullJson.indexOf("\"hourly\":");
        String unitsPart = fullJson.substring(fullJson.indexOf("\"hourly_units\":"), hourlyStart);
        String dataPart = fullJson.substring(hourlyStart);

        String[] times = extractArray(dataPart, "time");

        for (String v : vars) {
            String unit = extractValue(unitsPart, v);
            String[] values = extractArray(dataPart, v);
            showChart(v, loc, unit, times, values);
        }
    }

    private String fetchWithCache(String urlStr, String fileName) throws Exception {
        Path p = Paths.get(CACHE_DIR, fileName + ".json");
        // 3-hour cache check
        System.out.print(fileName);
        if (Files.exists(p) && (System.currentTimeMillis() - p.toFile().lastModified() < CACHE_EXPIRY_MS)) {
            return Files.readString(p);
        }

        // Legacy HttpURLConnection for zero-module-config compatibility
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder res = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) res.append(line);
        in.close();

        String body = res.toString();
        Files.writeString(p, body);
        return body;
    }

    private void showChart(String var, String loc, String unit, String[] times, String[] values) {
        Stage s = new Stage();
        CategoryAxis x = new CategoryAxis(); x.setLabel("dates");
        NumberAxis y = new NumberAxis(); y.setLabel("values (" + unit + ")");

        LineChart<String, Number> c = new LineChart<>(x, y);
        c.setTitle(var + " for " + loc);
        c.setCreateSymbols(false);

        XYChart.Series<String, Number> ser = new XYChart.Series<>();
        ser.setName(var);

        int len = Math.min(times.length, values.length);
        for (int i = 0; i < len; i++) {
            ser.getData().add(new XYChart.Data<>(times[i].replace("\"", ""), Double.parseDouble(values[i])));
        }

        c.getData().add(ser);
        s.setScene(new Scene(c, 750, 450));
        s.show();
    }

    // --- MANUAL PARSING HELPERS ---
    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "?";
        start += search.length();
        if (start < json.length() && json.charAt(start) == '"') start++;

        int end = start;
        while (end < json.length() && json.charAt(end) != '"' && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ']') {
            end++;
        }
        return json.substring(start, end).trim();
    }

    private String[] extractArray(String json, String key) {
        String s = "\"" + key + "\":[";
        int start = json.indexOf(s);
        if (start == -1) return new String[0];
        start += s.length();
        int end = json.indexOf("]", start);
        return json.substring(start, end).split(",");
    }
}