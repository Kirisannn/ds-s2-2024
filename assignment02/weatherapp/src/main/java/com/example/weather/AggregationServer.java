package com.example.weather;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.PriorityBlockingQueue;

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

    // File Paths
    private static final Path WEATHER = Paths.get("src/main/resources/weather.json");
    private static final Path WEATHER2 = Paths.get("src/main/resources/weather2.json");
    private static final Path TEMP = Paths.get("src/main/resources/temp.json");

    // Lamport lamportClock instance
    private static final LamportClock lamportClock = new LamportClock();

    // Priority queue to store PUT requests based on Lamport clock timestamps
    private static final PriorityBlockingQueue<LamportRequest> putQueue = new PriorityBlockingQueue<>();

    public static void main(String[] args) {
        cleanup();

        // Register shutdown hook for cleanup
        registerShutdown();

        // Receive port
        int port = args.length < 1 ? DEFAULT_PORT : Integer.parseInt(args[0]);
        if (port < 0 || port > 65535) {
            port = DEFAULT_PORT;
            System.err.println(createErrorResponse("Port out of range: " + port));
        }

        // Start the server
        startServer(port);

        // Endpoint for GET requests
        get("/*", (req, res) -> {
            // Retrieve the Lamport timestamp from the request headers
            String receivedTimestampStr = req.headers("Lamport-Timestamp");
            if (receivedTimestampStr != null) {
                try {
                    int receivedTimestamp = Integer.parseInt(receivedTimestampStr);
                    System.out.println(
                            """
                                    ------------------------------
                                    Received a GET request from: """ + req.ip() + " | Lamport Clock: "
                                    + receivedTimestamp);

                    // Update the Lamport clock based on the received timestamp
                    lamportClock.update(receivedTimestamp);
                    System.out.println("Current Lamport Clock after update: " + lamportClock.getTime());
                } catch (NumberFormatException e) {
                    System.err.println("Invalid Lamport timestamp format in headers: " + e.getMessage());
                    res.status(400); // Bad Request
                    return createErrorResponse("Invalid Lamport timestamp format.");
                }
            } else {
                System.err.println("No Lamport timestamp provided in the request headers.");
                res.status(400); // Bad Request
                return createErrorResponse("Lamport timestamp not found in the request headers.");
            }

            // Increment Lamport clock for the outgoing response
            lamportClock.increment();
            System.out.println("Current Lamport Clock after increment: " + lamportClock.getTime());

            // Wait for all preceding PUT requests to be processed
            processPutQueue();

            String id = req.queryParams("id");
            JsonArray data = readData(WEATHER);

            // If the client requested data for a specific ID
            if (id != null) {
                System.out.println("ID parameter provided: " + id);
                JsonArray result = getDataById(data, id);
                if (result != null) {
                    res.type("application/json");
                    res.header("Lamport-Timestamp", String.valueOf(lamportClock.getTime()));
                    return result.toString(); // Return specific data for the requested ID
                } else {
                    res.status(404); // Not Found
                    return createErrorResponse("Weather data for ID " + id + " not found.");
                }
            }

            // Return the updated feed after all preceding PUTs are applied
            res.type("application/json");
            res.header("Lamport-Timestamp", String.valueOf(lamportClock.getTime())); // Ensure this line is executed in
                                                                                     // a non-null context

            // Debug output for Lamport timestamp
            System.out.println("Lamport timestamp sent in headers: " + lamportClock.getTime());
            return data;
        });

        // Endpoint for PUT requests
        put("/*", (req, res) -> {
            String body = req.body(); // Retrieve the request body

            boolean dataExists = fileExists(WEATHER);

            // Check if the request body is empty
            if (body.isEmpty()) {
                res.status(204); // No Content
                return createErrorResponse("Empty request body.");
            }

            // Retrieve the Lamport timestamp from the request headers
            String receivedTimestampStr = req.headers("Lamport-Timestamp");
            if (receivedTimestampStr != null) {
                try {
                    int receivedTimestamp = Integer.parseInt(receivedTimestampStr);
                    System.out.println(
                            """
                                    ------------------------------
                                    Received a PUT request from: """ + req.ip()
                                    + " | Lamport Clock: " + receivedTimestamp);
                    lamportClock.update(receivedTimestamp); // Update the Lamport clock based on the received timestamp
                    System.out.println("Current Lamport Clock after update: " + lamportClock.getTime());
                } catch (NumberFormatException e) {
                    System.err.println("Invalid Lamport timestamp format in headers: " + e.getMessage());
                    res.status(400); // Bad Request
                    return createErrorResponse("Invalid Lamport timestamp format.");
                }
            } else {
                System.err.println("No Lamport timestamp provided in the request headers.");
                res.status(400); // Bad Request
                return createErrorResponse("Lamport timestamp not found in the request headers.");
            }

            // Parse the incoming JSON content
            JsonObject newEntry;
            try {
                newEntry = JsonParser.parseString(body).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                System.err.println("Failed to parse JSON: " + e.getMessage());
                res.status(400); // Bad Request
                return createErrorResponse("Invalid JSON format.");
            }

            // Validate the incoming weather data
            if (!isValidWeatherData(newEntry, res)) {
                return null; // Exit if validation fails
            }

            // Add the Lamport timestamp to the JSON object
            newEntry.addProperty("lamport_timestamp", lamportClock.getTime());

            // Add the PUT request to the queue based on the timestamp
            putQueue.add(new LamportRequest(lamportClock.getTime(), newEntry));
            // Debug output for Lamport timestamp
            System.out.println("Lamport timestamp added to the queue: " + lamportClock.getTime());

            // Process the PUT queue
            processPutQueue();

            // Increment Lamport clock for outgoing response
            lamportClock.increment();
            System.out.println("Current Lamport Clock after increment: " + lamportClock.getTime());

            // Add Lamport timestamp to response header
            res.header("Lamport-Timestamp", String.valueOf(lamportClock.getTime()));

            // Return appropriate status
            if (!dataExists) {
                res.status(201);
                return "{\"Success\": \"Weather data created!\"}\n";
            }
            res.status(200);
            return "{\"Success\": \"Weather data updated!\"}\n";
        });

        // Endpoint for HEAD requests
        head("/*", (req, res) -> {
            res.status(400);
            return createErrorResponse("Request type not supported.");
        });

        // Endpoint for all other requests
        notFound((req, res) -> {
            res.status(400);
            return createErrorResponse("Request type not supported.");
        });
    }

    private static JsonArray getDataById(JsonArray data, String id) {
        for (JsonElement element : data) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.get("id").getAsString().equals(id)) {
                String temp = "[ " + obj.toString() + " ]";
                return JsonParser.parseString(temp).getAsJsonArray(); // Return the matching object
            }
        }
        return null; // Return null if no match found
    }

    // Method to start the server
    private static void startServer(int port) {
        port(port);
        System.out.println("Server started on port: " + port);
    }

    // Cleanup function to clear temp.json
    private synchronized static void cleanup() {
        try {
            if (Files.exists(TEMP)) {
                Files.delete(TEMP);
                System.out.println("temp.json deleted");
            }
        } catch (IOException e) {
            System.err.println("Failed to delete temp.json: " + e.getMessage());
        }
    }

    // Graceful shutdown hook
    private synchronized static void registerShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (Files.exists(WEATHER)) {
                    Files.delete(WEATHER);
                    System.out.println("weather.json deleted");
                }
            } catch (IOException e) {
                System.err.println("Failed to delete weather.json: " + e.getMessage());
            }

            try {
                if (Files.exists(WEATHER2)) {
                    Files.delete(WEATHER2);
                    System.out.println("weather2.json deleted");
                }
            } catch (IOException e) {
                System.err.println("Failed to delete weather2.json: " + e.getMessage());
            }

            try {
                if (Files.exists(TEMP)) {
                    Files.delete(TEMP);
                    System.out.println("temp.json deleted");
                }
            } catch (IOException e) {
                System.err.println("Failed to delete temp.json: " + e.getMessage());
            }
        }));
    }

    // Read existing data from the weather file
    private synchronized static JsonArray readData(Path path) {
        JsonArray data = new JsonArray();
        try {
            String content = new String(Files.readAllBytes(path));
            JsonElement element = JsonParser.parseString(content);
            if (element.isJsonArray()) {
                data = element.getAsJsonArray();
            }
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Failed to read or parse weather data: " + e.getMessage());
        }
        return data;
    }

    // Check if a file exists
    private static boolean fileExists(Path path) {
        return Files.exists(path);
    }

    // Update existing data
    private static JsonArray updateData(JsonArray existing, JsonObject newEntry, String id) {
        if (existing.size() > 0) {
            for (JsonElement element : existing) {
                JsonObject object = element.getAsJsonObject();
                if (object.get("id").getAsString().equals(id)) {
                    object.add("air_temp", newEntry.get("air_temp"));
                    object.add("apparent_t", newEntry.get("apparent_t"));
                    object.add("cloud", newEntry.get("cloud"));
                    object.add("dewpt", newEntry.get("dewpt"));
                    object.add("press", newEntry.get("press"));
                    object.add("rel_hum", newEntry.get("rel_hum"));
                    object.add("wind_dir", newEntry.get("wind_dir"));
                    object.add("wind_spd_kmh", newEntry.get("wind_spd_kmh"));
                    object.add("wind_spd_kt", newEntry.get("wind_spd_kt"));
                    object.add("local_date_time", newEntry.get("local_date_time"));
                    object.add("local_date_time_full", newEntry.get("local_date_time_full"));
                    System.out.println("Data updated for ID: " + id);
                    return existing;
                }
            }
        }
        // Add new entry if ID not found
        existing.add(newEntry);
        System.out.println("New entry added to weather.json.");
        return existing;
    }

    // Process the PUT queue
    private synchronized static void processPutQueue() {
        while (!putQueue.isEmpty()) {
            LamportRequest request = putQueue.poll(); // Get and remove the head of the queue
            if (request != null) {
                // Update the weather.json file with the request data
                JsonArray existingData = readData(WEATHER);
                JsonArray updatedData = updateData(existingData, request.getContent(),
                        request.getContent().get("id").getAsString());

                // Write the updated data back to weather.json
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(WEATHER.toFile()))) {
                    writer.write(updatedData.toString());
                } catch (IOException e) {
                    System.err.println("Failed to write to weather.json: " + e.getMessage());
                }
            }
        }
    }

    // Create a standardized JSON error response
    private static String createErrorResponse(String message) {
        return "{\"Error\": \"" + message + "\"}";
    }

    // Validate the incoming weather data
    private static boolean isValidWeatherData(JsonObject newEntry, spark.Response res) {
        // Check if the entry is a valid JSON object
        if (!newEntry.isJsonObject()) {
            res.status(400); // Bad Request
            System.err.println("Invalid JSON format: Not a JSON object.");
            return false;
        }

        // Check for required 'id' field
        if (!newEntry.has("id")) {
            res.status(400); // Bad Request
            System.err.println("Missing required field 'id' in weather data.");
            return false;
        }

        // Ensure 'id' is a string
        if (!newEntry.get("id").isJsonPrimitive() || !newEntry.get("id").getAsJsonPrimitive().isString()) {
            res.status(400);
            System.err.println("Field 'id' must be a string.");
            return false;
        }

        return true;
    }
}
