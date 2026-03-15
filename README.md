# 🌦️ JavaFX Weather Dashboard

A lightweight, high-performance weather application that provides real-time data and 7-day forecasts using the Open-Meteo API.

## 🚀 Key Features
* **Intelligent Search:** Allows users to find weather by City Name (with alternative results) or direct Latitude/Longitude coordinates.
* **Dynamic Data Visualization:** Automatically generates separate `LineChart` windows for Temperature, Precipitation, Surface Pressure, and Wind Speed.
* **Smart Local Caching:** Implements a 3-hour TTL (Time-To-Live) cache system to reduce API latency and handle repeated requests efficiently.
* **Reactive UI:** Utilizes JavaFX property binding to dynamically enable/disable input fields based on user selection.

## 🛠️ Technical Implementation
* **Zero-Dependency Parsing:** Features custom JSON extraction logic to handle API responses without external libraries like Jackson or Gson.
* **API Integration:** Connects to the Open-Meteo Geocoding and Forecast REST APIs using standard `HttpURLConnection`.
* **Asynchronous Logic:** Managed through JavaFX event handling to ensure a responsive user experience.

## 📸 How it Works
1. **Input:** Enter a city name (e.g., "Paris") or coordinates.
2. **Select Metrics:** Choose which weather variables you want to visualize.
3. **Fetch:** The app checks the local cache first; if expired or missing, it fetches fresh data from the API and displays the charts.

## 🔧 Setup & Installation
1. Clone the repository: `git clone https://github.com/YOUR_USERNAME/weather-forecast-javafx.git`
2. Ensure you have **JDK 17+** and **JavaFX 17+** installed.
3. Run the `Launcher.java` file.

---
*Created as part of a Java Programming Examination project.*
