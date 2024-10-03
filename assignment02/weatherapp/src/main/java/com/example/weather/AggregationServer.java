package com.example.weather;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
            System.err.println("{\"Error\": \"Port out of range: " + port + "\"}");
        }

        // Start the server
        startServer(port);

        get("/*", (req, res) -> {
            // Retrieve the Lamport timestamp from the request headers
            String receivedTimestampStr = req.headers("Lamport-Timestamp");
            if (receivedTimestampStr != null) {
                try {
                    int receivedTimestamp = Integer.parseInt(receivedTimestampStr);
                    System.out.println(
                            "Received a GET request from: " + req.ip() + " | Lamport Clock: " + receivedTimestamp);

                    // Update the Lamport clock based on the received timestamp
                    lamportClock.update(receivedTimestamp);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid Lamport timestamp format in headers: " + e.getMessage());
                    res.status(400); // Bad Request
                    return "{\"Error\": \"Invalid Lamport timestamp format.\"}\n";
                }
            } else {
                System.err.println("No Lamport timestamp provided in the request headers.");
                res.status(400); // Bad Request
                return "{\"Error\": \"Lamport timestamp not found in the request headers.\"}\n";
            }

            // Increment Lamport clock for the outgoing response
            int currentTimestamp = lamportClock.increment();
            System.out.println("Current Lamport Clock after increment: " + currentTimestamp);

            // Wait for all preceding PUT requests to be processed
            processPutQueue();

            // Return the updated feed after all preceding PUTs are applied
            JsonArray data = readData(WEATHER);
            res.type("application/json");
            res.header("Lamport-Timestamp", currentTimestamp);
            
            // Debug output for lamport timestamp
            System.out.println("Lamport timestamp sent in headers: " + currentTimestamp);
            return data;
        });

        // Endpoint for PUT requests
        put("/*", (req, res) -> {
            String body = req.body(); // Retrieve the request body

            boolean dataExists = fileExists(WEATHER);

            // Check if the request body is empty
            if (body.isEmpty()) {
                res.status(204); // No Content
                return "{\"Error\": \"Empty request body.\"}\n";
            }

            // Retrieve the Lamport timestamp from the request headers
            String receivedTimestampStr = req.headers("Lamport-Timestamp");
            if (receivedTimestampStr != null) {
                try {
                    int receivedTimestamp = Integer.parseInt(receivedTimestampStr);
                    System.out.println(
                            "Received a PUT request from: " + req.ip() + " | Lamport Clock: " + receivedTimestamp);
                    lamportClock.update(receivedTimestamp); // Update the Lamport clock based on the received timestamp
                } catch (NumberFormatException e) {
                    System.err.println("Invalid Lamport timestamp format in headers: " + e.getMessage());
                    res.status(400); // Bad Request
                    return "{\"Error\": \"Invalid Lamport timestamp format.\"}\n";
                }
            } else {
                System.err.println("No Lamport timestamp provided in the request headers.");
                res.status(400); // Bad Request
                return "{\"Error\": \"Lamport timestamp not found in the request headers.\"}\n";
            }

            // Increment Lamport clock for outgoing response
            int currentTimestamp = lamportClock.increment();

            // Parse the incoming JSON content
            JsonObject newEntry;
            try {
                newEntry = JsonParser.parseString(body).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                System.err.println("Failed to parse JSON: " + e.getMessage());
                res.status(400); // Bad Request
                return "{\"Error\": \"Invalid JSON format.\"}\n";
            }

            // Add the Lamport timestamp to the JSON object
            newEntry.addProperty("lamport_timestamp", currentTimestamp);

            // Add the PUT request to the queue based on the timestamp
            putQueue.add(new LamportRequest(currentTimestamp, newEntry));

            // Process the PUT queue
            processPutQueue();

            // Add Lamport timestamp to response header
            res.header("Lamport-Timestamp", String.valueOf(currentTimestamp));

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
            return "{\"Error\": \"Request type not supported.\"}\n";
        });

        // Endpoint for all other requests
        notFound((req, res) -> {
            res.status(400);
            return "{\"Error\": \"Request type not supported.\"}\n";
        });
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

    // Server startup
    private synchronized static void startServer(int port) {
        try {
            port(port);
            System.out.println("Server started on port: " + port);
        } catch (Exception e) {
            System.err.println("Failed to start server on port: " + port);
        }
    }

    // Check if a file exists
    private synchronized static boolean fileExists(Path path) {
        return Files.exists(path);
    }

    // Create weather.json with an empty JSON array
    private synchronized static void createFile(Path path) {
        try {
            Files.createFile(path);
            Files.write(path, "[]".getBytes());
            System.out.println("File created at: " + WEATHER2.toString());
        } catch (IOException e) {
            System.err.println("Failed to create weather.json: " + e.getMessage());
        }
    }

    // Read data from weather.json as a JSON array without ID
    private synchronized static JsonArray readData(Path path) {
        try {
            return JsonParser.parseString(new String(Files.readAllBytes(path))).getAsJsonArray();
        } catch (IOException e) {
            System.err.println("Failed to read weather.json: " + e.getMessage());
            return null;
        }
    }

    // Read data from weather.json as a JSON array WITH ID
    private synchronized static JsonArray readID(Path path, String id) {
        JsonObject object = null;
        try {
            JsonArray array = JsonParser.parseString(new String(Files.readAllBytes(path))).getAsJsonArray();
            System.out.println("{\"Success\": \"read data from weather.json!\"}");
            if (array.size() > 0) {
                // Check if id exists in existing
                for (JsonElement element : array) {
                    object = element.getAsJsonObject();

                    if (object.get("id").getAsString().equals(id)) {
                        JsonArray returnArray = new JsonArray();
                        returnArray.add(object);

                        // Return the object
                        return returnArray;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Failed to read data from weather.json.");
            System.err.println("Error: " + e.getMessage());
            return null;
        }

        return null;
    }

    // Update array
    private synchronized static JsonArray updateData(JsonArray existing, JsonObject newEntry, String id) {
        if (existing.size() > 0) {
            // Check if id exists in existing
            for (JsonElement element : existing) {
                JsonObject object = element.getAsJsonObject();

                if (object.get("id").getAsString().equals(id)) {
                    existing.remove(element);
                }
            }
        }

        // Insert update
        existing.add(newEntry);

        return existing;
    }

    // Write update
    private synchronized static void writeUpdate(JsonArray newData) {
        // Write to temp.json first
        createFile(TEMP); // Create temp.json to write to for security in case of crash

        // If temp.json successful, write newData to temp.json
        // Try writing to temp.json
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEMP.toString()))) {
            writer.write(newData.toString());
            System.out.println("{\"Success\": \"Wrote to temp.json!\"}");
        } catch (IOException e) {
            System.err.println("Failed to write to temp.json D:");
            System.err.println("Error: " + e.getMessage());
        }

        // Rename weather.json to weather_old.json
        try {
            Files.move(WEATHER, WEATHER2, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("{\"Success\": \"Renamed weather.json to weather2.json\"}");
        } catch (IOException e) {
            System.err.println("Failed to rename weather.json.");
            System.err.println("Error: " + e.getMessage());
        }

        // Rename temp.json to weather.json
        try {
            Files.move(TEMP, WEATHER);
            System.out.println("{\"Success\": \"Renamed temp.json to weather.json\"}");
        } catch (IOException e) {
            System.err.println("Failed to rename temp.json.");
            System.err.println("Error: " + e.getMessage());
        }

    }

    // Process the PUT queue and apply updates in Lamport clock order
    private static synchronized void processPutQueue() {
        // Check if the weather.json file exists, and create it if it doesn't
        if (!fileExists(WEATHER)) {
            createFile(WEATHER); // Create the weather.json file with an empty array
        }

        while (!putQueue.isEmpty()) {
            // Get the PUT request with the smallest Lamport timestamp
            LamportRequest nextPut = putQueue.poll();

            if (nextPut != null) {
                // Apply the PUT request (e.g., update the weather data file)
                JsonArray existingData = readData(WEATHER);
                if (existingData == null) {
                    // If reading the existing data fails, initialize it
                    existingData = new JsonArray();
                }
                JsonArray newData = updateData(existingData, nextPut.getContent(),
                        nextPut.getContent().get("id").getAsString());
                writeUpdate(newData);
            }
        }
    }

}