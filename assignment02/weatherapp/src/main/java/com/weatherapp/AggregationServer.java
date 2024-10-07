package com.weatherapp;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class AggregationServer {
    private static final int DEFAULT_PORT = 4567; // Default port number for the server
    private static final LamportClock lamportClock = new LamportClock(); // Lamport clock for timestamping
    private static final ReentrantLock lock = new ReentrantLock(); // Lock to enforce concurrency and prevent race
                                                                   // conditions

    private static final String WEATHER = "src/main/resources/weather.json"; // Path to the weather data file
    private static final String TEMP = "src/main/resources/temp.json"; // Path to the temporary file
    private static final ConcurrentHashMap<String, JsonElement> weatherData = new ConcurrentHashMap<>(); // Data store
                                                                                                         // for weather
                                                                                                         // data
    private static final ConcurrentHashMap<String, Long> lastConnection = new ConcurrentHashMap<>(); // Data store for
                                                                                                     // last connection
                                                                                                     // time
    private static final PriorityQueue<ClientRequests> requests = new PriorityQueue<>(); // Request queue for client
                                                                                         // requests

    /**
     * Main method for the AggregationServer class.
     * 
     * @param args Command-line arguments where:
     *             args[0] is the port number for the server.
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT; // Initialise port with default value
        if (args.length > 0 && validatePort(Integer.parseInt(args[0]))) {
            port = Integer.parseInt(args[0]); // Set port to user-specified value if valid
        }

        restoreData(); // Restore the saved data from the file if it exists

        // Thread to clear expired entries from the data store
        new Thread(AggregationServer::clearExpired).start();

        // Thread to process client requests
        new Thread(AggregationServer::processRequests).start();

        startServer(port); // Start the server with the specified port
    }

    /**
     * Validates the provided port number.
     *
     * @param port The port number to validate.
     * @return true if the port number is valid; false otherwise.
     */
    static boolean validatePort(int port) {
        return port >= 0 && port <= 65535; // Check if port is in the valid range
    }

    /**
     * Restores the weather data from the JSON file.
     * 
     * If the file is empty or malformed, initializes an empty data store.
     */
    static void restoreData() {
        lock.lock(); // Acquire the lock to prevent concurrent access
        try {
            if (Files.exists(Paths.get(WEATHER))) {
                // Read the saved data from the file
                String json = new String(Files.readAllBytes(Paths.get(WEATHER)));

                // Check if the file is empty or contains invalid JSON
                if (json.isEmpty()) {
                    System.out.println("Weather file is empty. Initializing with empty data.");
                    weatherData.clear(); // Clear any existing data
                } else {
                    try {
                        // Parse the file as a JSON array
                        JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();

                        // Fill data store with the saved data
                        for (JsonElement obj : jsonArray) {
                            weatherData.put(obj.getAsJsonObject().get("id").getAsString(), obj);
                            lastConnection.put(obj.getAsJsonObject().get("id").getAsString(),
                                    System.currentTimeMillis());
                        }
                    } catch (JsonSyntaxException | IllegalStateException e) {
                        // Handle case where JSON is not an array or is malformed
                        System.err.println("Invalid or malformed JSON file. Initializing with empty data.");
                        weatherData.clear(); // Clear any existing data
                    }
                }
            } else {
                System.out.println("No previous data found. Proceeding with a new data store.");
                weatherData.clear(); // Ensure we start with an empty data store
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } finally {
            lock.unlock(); // Release the lock
        }
    }

    /**
     * Periodically clears expired entries from the weather data store.
     * Entries are considered expired if they haven't been updated in the last 30
     * seconds.
     */
    static void clearExpired() {
        while (true) {
            long currTime = System.currentTimeMillis();
            lastConnection.entrySet().removeIf(entry -> {
                boolean expired = (currTime - entry.getValue()) > 30000; // Check for expiration
                if (expired) {
                    System.out.println("Station " + entry.getKey() + " has expired.");
                    weatherData.remove(entry.getKey()); // Remove expired station data
                    try {
                        writeData(); // Write updated data to file
                    } catch (IOException e) {
                        System.err.println("Error writing data to file when clearing old: " + e.getMessage());
                    }
                }
                return expired; // Return whether the entry was expired
            });

            try {
                Thread.sleep(5000); // Sleep for 5 seconds before next check
            } catch (InterruptedException e) {
                System.err.println("Error removing expired entries: " + e.getMessage());
                Thread.currentThread().interrupt(); // Restore interrupted status
                break;
            }
        }
    }

    /**
     * Continuously processes incoming client requests from the queue.
     */
    static void processRequests() {
        while (true) {
            ClientRequests request;
            synchronized (requests) {
                request = requests.poll(); // Retrieve the next request from the queue
            }

            if (request != null) {
                try {
                    request.process(); // Process the retrieved request
                } catch (IOException e) {
                    System.err.println("Error processing request: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Creates a ServerSocket and listens for incoming client connections.
     *
     * @param port The port number to listen on.
     */
    static void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("""
                    ===================================
                    Server started on port: """ + port);

            // Listen for incoming connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("""
                        -----------------------------------
                        Connection established with client: """ + clientSocket.getRemoteSocketAddress());

                // Create a new thread to handle the client connection
                Thread clientThread = new Thread(() -> connectionHandler(clientSocket));
                clientThread.start(); // Start the client handler thread
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    /**
     * Handles individual client connections.
     *
     * @param clientSocket The socket associated with the client connection.
     */
    static void connectionHandler(Socket clientSocket) {
        try {
            clientSocket.setSoTimeout(10000); // Set timeout for the client socket

            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

            StringBuilder headers = new StringBuilder();
            String line;

            // Read headers from the client
            int clientTime = 0;
            while (!(line = in.readUTF()).isEmpty()) {
                headers.append(line).append("\n");
                if (line.contains("Lamport-Time")) {
                    clientTime = Integer.parseInt(line.split(": ")[1]); // Extract Lamport time
                    break;
                }
            }
            System.out.println("Headers: " + headers);

            // Parse the headers into a map
            String[] headerLines = headers.toString().split("\n");
            Map<String, String> headerMap = new ConcurrentHashMap<>();
            for (int i = 0; i < headerLines.length; i++) {
                if (i == 0) {
                    String[] request = headerLines[i].split(" ");
                    headerMap.put("Request-Type", request[0]); // Store request type
                } else {
                    String[] header = headerLines[i].split(": ");
                    headerMap.put(header[0], header[1]); // Store other headers
                }
            }

            // Read the JSON data after headers
            StringBuilder requestBody = new StringBuilder();
            while (in.available() > 0) {
                requestBody.append(in.readUTF()).append("\n"); // Read request body
            }

            // Determine request type (PUT or GET)
            String requestType = headerMap.get("Request-Type");
            System.out.println("Request type: " + requestType);
            switch (requestType) {
                case "PUT" -> {
                    if (!requestBody.isEmpty()) { // If request body is not empty
                        try {
                            @SuppressWarnings("unused")
                            JsonElement jsonElement = JsonParser.parseString(requestBody.toString()); // Validate JSON
                        } catch (JsonSyntaxException e) {
                            System.out.println("Invalid JSON format in PUT request.");
                            out.writeUTF("Invalid JSON format in PUT request.");
                            out.flush();
                            return; // Exit on invalid JSON
                        }
                    }
                }
                case "GET" -> {
                }
                default -> // Invalid request type
                    System.out.println("""
                            Invalid request type. Please use PUT or GET.
                            Closing connection with client: """ + clientSocket.getRemoteSocketAddress());
            }
            // Valid GET request

            // Sync the Lamport time
            synchronized (AggregationServer.class) {
                lamportClock.updateClock(clientTime); // Update Lamport clock with client's time
            }

            // Add request to the queue for processing
            ClientRequests request = new ClientRequests(clientSocket, requestType,
                    headers.toString(), requestBody.toString(), clientTime);
            synchronized (requests) {
                requests.add(request); // Add request to the shared queue
            }
        } catch (IOException e) {
            System.err.println("Error handling connection: " + e.getMessage());
        }
    }

    /**
     * Updates the weather data for a specified station.
     *
     * @param stationID The ID of the weather station.
     * @param data      The JSON object containing weather data.
     * @return true if the weather file was created; false otherwise.
     * @throws IOException if an I/O error occurs.
     */
    static boolean updateData(String stationID, JsonObject data) throws IOException {
        boolean fileExists = Files.exists(Paths.get(WEATHER)); // Check if the weather file exists
        if (!fileExists) {
            Files.createFile(Paths.get(WEATHER)); // Create the weather file if it doesn't exist
        }

        lock.lock(); // Acquire the lock to prevent concurrent access
        try {
            weatherData.put(stationID, data); // Update the weather data
            lastConnection.put(stationID, System.currentTimeMillis()); // Update the last connection time

            // If more than 20 entries, remove the oldest entry
            if (weatherData.size() > 20) {
                String oldestKey = lastConnection.entrySet().stream().min(Map.Entry.comparingByValue()).get().getKey();
                weatherData.remove(oldestKey); // Remove the oldest weather data
                lastConnection.remove(oldestKey); // Remove the last connection time for the oldest entry
            }

            writeData(); // Write the updated data to the file
        } catch (IOException e) {
            System.err.println("Error updating weather data: " + e.getMessage());
        } finally {
            lock.unlock(); // Release the lock
        }

        return !fileExists; // Return whether the file was newly created
    }

    /**
     * Writes the current weather data to the weather JSON file.
     * 
     * Uses a temporary file to avoid data loss during the write process.
     * 
     * @throws IOException if an I/O error occurs.
     */
    static void writeData() throws IOException {
        JsonArray jsonArray = new JsonArray(); // Create a JsonArray to hold all weather data

        // Add all entries from the weatherData map to the array
        for (String stationID : weatherData.keySet()) {
            jsonArray.add(weatherData.get(stationID)); // Add each weather entry
        }

        // Write the JsonArray to the temp file
        Path tempFile = Paths.get(TEMP);
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            writer.write(jsonArray.toString()); // Convert the JsonArray to a string and write to the temp file
            writer.newLine();
        }

        System.out.println("Successfully wrote data to temp file.");

        // Replace the original weather.json file with the temp file
        Files.copy(tempFile, Paths.get(WEATHER), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Successfully replaced weather.json with temp file.");
    }

    /**
     * Retrieves the current Lamport clock time.
     *
     * @return the current Lamport clock time.
     */
    static int getClock() {
        return lamportClock.getTime(); // Return the current Lamport clock time
    }
}
