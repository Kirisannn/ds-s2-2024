package com.example.weather;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import static spark.Spark.get;
import static spark.Spark.port;

public class AggregationServer {
    private static final int DEFAULT_PORT = 4567; // Default port for your server
    // Path to the weather cache file
    private static final String WEATHER_CACHE = "src/main/resources/weather_db_cache.json";

    public static void main(String[] args) {
        int port = DEFAULT_PORT; // Default port for your server

        // Check if a port number is provided
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);

                if (port < 0 || port > 65535) {
                    port = DEFAULT_PORT;
                    System.err.println("Port out of range. Using default port " + port);
                }
            } catch (NumberFormatException e) {
                port = DEFAULT_PORT;
                System.err.println("Invalid port number format. Using default port " + port);
            }
        }

        // Start the server
        try {
            port(port);
            System.out.println("Server started on port: " + port);
        } catch (Exception e) {
            System.err.println("Failed to start server on port: " + port);
        }

        // Endpoint for GET requests
        get("/weather", (req, res) -> {
            System.out.println("Received a GET request from: " + req.ip());
            res.type("application/json");

            // Get station id from query parameter
            String station_id = req.queryParams("id");

            // Load data from file, with or without station id
            String jsonResponse = loadDataFromFile(station_id);
            if (jsonResponse != null) {
                return jsonResponse;
            } else {
                res.status(404); // Internal server error if loading fails

                if (!Files.exists(Paths.get(WEATHER_CACHE))) {
                    return "{\"error\": \"Failed to load data. File is missing.\"}";
                }
                return "{\"error\": \"Station not found.\"}";
            }
        });
    }

    private static String loadDataFromFile(String station_id) {
        try {
            String jsonData = new String(Files.readAllBytes(Paths.get(WEATHER_CACHE)));

            // Validate JSON format
            JsonElement jsonElement = JsonParser.parseString(jsonData); // Throws JsonSyntaxException if invalid JSON

            // Check if it is an array
            if (jsonElement.isJsonArray()) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();

                // If station id is not provided, return whole JSON array
                if (station_id == null) {
                    return jsonArray.toString(); // Return the valid JSON array string
                } else {
                    // If no station id is found, filter the JSON array
                    for (JsonElement element : jsonArray) {
                        JsonObject jsonObject = element.getAsJsonObject();
                        if (jsonObject.has("id") && jsonObject.get("id").getAsString().equals(station_id)) {
                            return "[" + jsonObject.toString() + "]"; // Return the matching JSON object
                        }
                    }
                    System.err.println("Station not found.");
                    return null; // Return null if station ID is not found
                }
            } else {
                throw new JsonSyntaxException("Expected a JSON array");
            }
        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON format: " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.err.println("Error loading data: " + e.getMessage());
            return null;
        }
    }
}