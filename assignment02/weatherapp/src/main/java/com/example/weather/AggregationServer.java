package com.example.weather;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import static spark.Spark.get;
import static spark.Spark.port;

public class AggregationServer {
    private static final int DEFAULT_PORT = 4567;
    private static final String WEATHER_CACHE = "src/main/resources/weather_cache.json";

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            // Try to parse the port number from the command line arguments, catch if out of
            // range or non-numeric
            try {
                port = Integer.parseInt(args[0]);
                if (port < 0 || port > 65535) {
                    System.err.println("Port number out of range. Starting server on default port: " + DEFAULT_PORT);
                    port = DEFAULT_PORT;
                } else {
                    System.out.println("Server started on port: " + port);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Starting server on default port: " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }

        port(port);

        // Endpoint for GET requests
        get("/weather", (req, res) -> {
            System.out.println("Received a GET request from: " + req.ip());
            res.type("application/json");
            String jsonResponse = loadDataFromFile();
            if (jsonResponse != null) {
                return jsonResponse;
            } else {
                res.status(500); // Internal server error if loading fails

                // return an error message
                return "{\"error\": \"Failed to load data. Either the file is missing or the JSON is invalid.\"}";
            }
        });
    }

    // Load JSON data from file as a String
    private static String loadDataFromFile() {
        try {
            String jsonData = new String(Files.readAllBytes(Paths.get(WEATHER_CACHE)));

            // Validate JSON format
            JsonElement jsonElement = JsonParser.parseString(jsonData); // Throws JsonSyntaxException if invalid JSON

            // Check if it is an array
            if (jsonElement.isJsonArray()) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                return jsonArray.toString(); // Return the valid JSON array string
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
