package com.example.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class GETClient {
    private static final int DEFAULT_PORT = 4567; // Default port for your server
    // Array to store month names
    private static final String[] dateParts = { "January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December" };

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("No URL provided. Please enter a valid URL.");
            System.exit(1);
        }

        String serverURL = args[0];
        String weatherEndpoint = "/weather";

        // Ensure URL has a protocol
        if (!serverURL.startsWith("http://") && !serverURL.startsWith("https://")) {
            serverURL = "http://" + serverURL; // Prepend "http://" if missing
        }

        try {
            // Create a URI object from the user input
            URI uri = new URI(serverURL);
            String host = uri.getHost();

            // Check if host is null or invalid
            if (host == null) {
                System.err.println("Invalid URL format: Missing host.");
                return;
            }

            int port = uri.getPort(); // Get port from URL or -1 if not specified

            // Use default port if none is specified
            if (port == -1) {
                port = DEFAULT_PORT; // Default to 4567 (or 80 for standard HTTP)
            }

            // Construct the full URL for the weather endpoint
            String fullURL = uri.getScheme() + "://" + host + ":" + port + weatherEndpoint;
            URI uri2 = new URI(fullURL);
            URL url = uri2.toURL();

            // Open HTTP connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Check the response code
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder jsonResponse = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        jsonResponse.append(inputLine);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading the server response: " + e.getMessage());
                    return;
                }

                // Display the JSON response formatted
                displayData(jsonResponse.toString());
            } else {
                System.err.println("GET request failed. Response Code: " + responseCode);
            }

        } catch (URISyntaxException e) {
            System.err.println("Invalid URL: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error connecting to the server: " + e.getMessage());
        }
    }

    // Function to display JSON data in the required format
    private static void displayData(String jsonData) {
        try {
            // Parse JSON response string into a JsonArray
            JsonElement jsonElement = JsonParser.parseString(jsonData);
            if (!jsonElement.isJsonArray()) {
                throw new JsonSyntaxException("Expected a JSON array.");
            }

            // Iterate through the array and display each object
            for (JsonElement element : jsonElement.getAsJsonArray()) {
                JsonObject jsonObject = element.getAsJsonObject();

                // Extracting required values
                String name = jsonObject.get("name").getAsString();
                String state = jsonObject.get("state").getAsString();
                String timeZone = jsonObject.get("time_zone").getAsString();
                String localDateTimeFull = jsonObject.get("local_date_time_full").getAsString();
                double airTemp = jsonObject.get("air_temp").getAsDouble();
                double apparentTemp = jsonObject.get("apparent_t").getAsDouble();
                String cloudiness = jsonObject.get("cloud").getAsString();
                String windDir = jsonObject.get("wind_dir").getAsString();
                double windSpdKmh = jsonObject.get("wind_spd_kmh").getAsDouble();
                double windSpdKt = jsonObject.get("wind_spd_kt").getAsDouble();

                // Formatting output
                String formattedDate = formatDate(localDateTimeFull);
                String formattedTime = formatTime(localDateTimeFull) + " " + timeZone;
                System.out.println("======================================");
                System.out.println("Location: " + name + ", " + state);
                System.out.println("Date: " + formattedDate);
                System.out.println("Time: " + formattedTime);
                System.out.printf("Temperature: %.1fC (Feels like %.1fC)%n", airTemp, apparentTemp);
                System.out.printf("Cloudiness: %s%n", cloudiness);
                System.out.printf("Wind: %s %.1f km/h, %.1f knots%n", windDir, windSpdKmh, windSpdKt);
                System.out.println("--------------------------------------");
            }
            System.out.println("======================================");

        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON format. Failed to parse: " + e.getMessage());
        }
    }

    private static String formatDate(String fullDateTime) {
        String month = fullDateTime.substring(4, 6);
        String day = fullDateTime.substring(6, 8);

        // Convert month to int
        int monthInt = Integer.parseInt(month);
        month = dateParts[monthInt - 1]; // Get month name from array

        return day + " " + month;
    }

    private static String formatTime(String fullDateTime) {
        String hour = fullDateTime.substring(8, 10);
        String minute = fullDateTime.substring(10, 12);

        return hour + ":" + minute;
    }
}
