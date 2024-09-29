package com.example.weather;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import static spark.Spark.get;
import static spark.Spark.head;
import static spark.Spark.notFound;
import static spark.Spark.port;
import static spark.Spark.put;


public class AggregationServer {
    private static final int DEFAULT_PORT = 4567; // Default port for your server
    // Path to the weather cache file
    private static final String WEATHER_CACHE = "src/main/resources/weather_db_cache.json";
    private static final String TEMP_FILE = "src/main/resources/temp.json";

    // Message Constants
    private static final String ERROR_MISSING_FILE = "{\"error\": \"Failed to load data. File is missing.\"}";
    private static final String ERROR_STATION_NOT_FOUND = "{\"error\": \"Station not found.\"}";
    private static final String ERROR_PORT_OUT_OF_RANGE = "\"error\": \"Port out of range. Using default port \"}";
    private static final String ERROR_INVALID_PORT_FORMAT = "{\"error\": \"Invalid port number format. Using default port \"}";
    private static final String ERROR_BAD_REQUEST = "{\"error\": \"HTTP request type is unsupported.\"}";
    private static final String ERROR_NOT_JSON_ARRAY = "{\"error\": \"Expected a JSON array.\"}";
    private static final String ERROR_INVALID_JSON_FORMAT = "{\"error\": \"Invalid JSON format.\"}";
    private static final String ERROR_FILE_CREATION_FAILED = "{\"error\": \"Failed to create weather cache file.\"}";
    private static final String ERROR_FILE_WRITE_FAILED = "{\"error\": \"Failed to write data to file.\"}";
    private static final String ERROR_FAILED_UPDATE_TEMP = "{\"error\": \"Failed to update TEMP.\"}";
    private static final String ERROR_FAILED_UPDATE = "{\"error\": \"Failed to update weather cache.\"}";
    private static final String ERROR_FAILED_DELETE = "{\"error\": \"Failed to delete file\"}";
    private static final String WEATHER_CACHE_CREATED = "{\"message\": \"Weather cache created.\"}";
    private static final String WEATHER_CACHE_UPDATED = "{\"message\": \"Weather cache updated.\"}";
    private static final String GRACEFUL_SHUTDOWN = "{\"message\": \"Server shutting down.\"}";

    public static void main(String[] args) {
        int port = DEFAULT_PORT; // Default port for your server

        // Cleanup
        try {
            // Delete the TEMP_FILE if it exists
            if (Files.exists(Paths.get(TEMP_FILE))) {
                Files.delete(Paths.get(TEMP_FILE));
                System.out.println("temp.json deleted");
            }
        } catch (IOException e) {
            System.err.println(ERROR_FAILED_DELETE + ": " + e.getMessage());
        }

        // Register shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(GRACEFUL_SHUTDOWN);
            try {
                // Delete weather_db_cache.json if it exists
                if (Files.exists(Paths.get(WEATHER_CACHE))) {
                    Files.delete(Paths.get(WEATHER_CACHE));
                    System.out.println("weather_db_cache.json deleted");
                }
            } catch (IOException e) {
                System.err.println(ERROR_FAILED_DELETE + ": " + e.getMessage());

            }
            System.out.println("Cleanup complete. Server shutting down.");
        }));

        // Check if a port number is provided
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);

                if (port < 0 || port > 65535) {
                    port = DEFAULT_PORT;
                    System.err.println(ERROR_PORT_OUT_OF_RANGE + port);
                }
            } catch (NumberFormatException e) {
                port = DEFAULT_PORT;
                System.err.println(ERROR_INVALID_PORT_FORMAT + port);
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

            // Get station id from query param
            String station_id = req.queryParams("id");

            // Load data from file, with or without station id
            String jsonResponse = loadDataFromFile(station_id);
            if (jsonResponse != null) {
                return jsonResponse;
            } else {
                res.status(404); // Internal server error if loading fails

                if (!Files.exists(Paths.get(WEATHER_CACHE))) {
                    return ERROR_MISSING_FILE;
                }
                return ERROR_STATION_NOT_FOUND;
            }
        });

        // Endpoint for PUT requests
        put("/weather", (var req, var res) -> {
            System.out.println("Received a PUT request from: " + req.ip());

            // Read the request body into a string
            String body = req.body();

            // Check if body is in JSON array format - respond with status 500 if not
            try {
                JsonElement jsonElement = JsonParser.parseString(body);
                if (!jsonElement.isJsonArray()) {
                    res.status(500);
                    System.out.println(ERROR_NOT_JSON_ARRAY);
                    return ERROR_NOT_JSON_ARRAY;
                }
            } catch (JsonSyntaxException e) {
                res.status(500);
                System.out.println(ERROR_INVALID_JSON_FORMAT);
                return ERROR_INVALID_JSON_FORMAT;
            } // Continue if JSON array format is valid

            // Check if the file exists
            boolean fileCreated = fileExists(WEATHER_CACHE);
            if (!fileCreated) {
                try {
                    Files.createFile(Paths.get(WEATHER_CACHE)); // Create the file if it doesn't exist
                    res.status(201);
                } catch (IOException e) {
                    System.err.println(ERROR_FILE_CREATION_FAILED + " " + e.getMessage());
                    res.status(500);
                    return ERROR_FILE_CREATION_FAILED;
                }

                JsonArray init = new JsonArray();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(WEATHER_CACHE))) {
                    writer.write(init.toString()); // Write an empty JSON array to the file
                } catch (IOException e) {
                    System.err.println(ERROR_FILE_WRITE_FAILED + " " + e.getMessage());
                    res.status(500); // Internal server error if writing fails
                    return ERROR_FILE_WRITE_FAILED;
                }
            }

            // Create temp.json
            try {
                Files.createFile(Paths.get(TEMP_FILE));
            } catch (IOException e) {
                System.err.println("Error writing to file: " + e.getMessage());
                res.status(500); // Internal server error if writing fails
                return "{\"error\": \"Failed to write data to file.\"}";
            }

            // Load the content of WEATHER_CACHE into a string
            String existingData = loadDataFromFile(null);
            // Convert the string into a JSON array
            JsonArray weatherDataCache = JsonParser.parseString(existingData).getAsJsonArray();
            JsonArray newEntries = JsonParser.parseString(body).getAsJsonArray();

            // Add new entries to the existing data, updating existing id with new data
            for (JsonElement entry : newEntries) {
                JsonObject entryObject = entry.getAsJsonObject();
                String id = entryObject.get("id").getAsString();
                boolean found = false;

                // Iterate through the existing data to find the entry with the same id
                for (JsonElement element : weatherDataCache) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    if (jsonObject.has("id") && jsonObject.get("id").getAsString().equals(id)) {
                        weatherDataCache.remove(element);
                        weatherDataCache.add(entryObject);
                        found = true;
                        break;
                    }
                }

                if (!found) { // If the entry with the same id is not found, add the new entry
                    weatherDataCache.add(entryObject);
                }
            }

            // Sort the JSON array by id
            weatherDataCache = sortByID(weatherDataCache);

            // Try writing to temp.json
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEMP_FILE))) {
                writer.write(weatherDataCache.toString());
            } catch (IOException e) {
                System.err.println(ERROR_FAILED_UPDATE_TEMP + " " + e.getMessage());
                res.status(500); // Internal server error if writing fails
                return ERROR_FILE_WRITE_FAILED;
            }

            // Write the content of temp.json to WEATHER_CACHE
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(WEATHER_CACHE))) {
                writer.write(weatherDataCache.toString());
            } catch (IOException e) {
                System.err.println(ERROR_FAILED_UPDATE + " " + e.getMessage());
                res.status(500); // Internal server error if writing fails
                return ERROR_FAILED_UPDATE;
            }

            if (!fileCreated) {
                res.status(201);
                System.out.println(WEATHER_CACHE_CREATED);
                return WEATHER_CACHE_CREATED;
            }
            res.status(200);
            System.out.println(WEATHER_CACHE_UPDATED);
            return WEATHER_CACHE_UPDATED;

        });


        // Endpoint for HEAD requests
        head("/*", (req, res) -> {
            res.status(400);
            return ERROR_BAD_REQUEST;
        });

        // Endpoint for all other requests
        notFound((req, res) -> {
            res.status(400);
            return ERROR_BAD_REQUEST;
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

    private static boolean fileExists(String path) {
        return Files.exists(Paths.get(path));
    }

    private static JsonArray sortByID(JsonArray weatherDataCache) {
        // Convert JsonArray to ArrayList
        ArrayList<JsonObject> list = new ArrayList<>();
        for (JsonElement element : weatherDataCache) {
            list.add(element.getAsJsonObject());
        }

        // Sort the ArrayList by id
        Collections.sort(list, (JsonObject a, JsonObject b) -> {
            String idA = a.get("id").getAsString();
            String idB = b.get("id").getAsString();
            return idA.compareTo(idB);
        });

        // Convert the sorted ArrayList back to JsonArray
        JsonArray sortedArray = new JsonArray();
        for (JsonObject object : list) {
            sortedArray.add(object);
        }

        return sortedArray;
    }
}