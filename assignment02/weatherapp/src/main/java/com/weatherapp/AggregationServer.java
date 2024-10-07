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
import java.util.HashMap;
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

    // Path to the weather data file
    private static final String WEATHER = "src/main/java/com/weatherapp/weather.json";
    // Path to the temp file
    private static final String TEMP = "src/main/java/com/weatherapp/temp.json";
    // Data store for weather data
    private static final ConcurrentHashMap<String, JsonElement> weatherData = new ConcurrentHashMap<>();
    // Data store for last connection time
    private static final ConcurrentHashMap<String, Long> lastConnection = new ConcurrentHashMap<>();
    // Request queue for client requests
    private static final PriorityQueue<ClientRequests> requests = new PriorityQueue<>();

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

        // Restore the saved data from the file if it exists
        restoreData();

        // Thread to clear expired entries from the data store
        new Thread(AggregationServer::clearExpired).start();

        // Thread to process client requests
        new Thread(AggregationServer::processRequests).start();

        // Start the server with the specified port
        startServer(port);
    }

    static boolean validatePort(int port) {
        return port >= 0 && port <= 65535;
    }

    static void restoreData() {
        lock.lock(); // Acquire the lock to prevent concurrent access
        try {
            if (Files.exists(Paths.get("weather.json"))) {
                // Read the saved data from the file get as JsonArray
                String json = new String(Files.readAllBytes(Paths.get("weather.json")));
                JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();

                // Fill data store with the saved data
                for (JsonElement obj : jsonArray) {
                    weatherData.put(obj.getAsJsonObject().get("id").getAsString(), obj);
                    lastConnection.put(obj.getAsJsonObject().get("id").getAsString(), System.currentTimeMillis());
                }
            } else {
                System.out.println("No previous data found. Proceeding with a new data store.");
            }
        } catch (JsonSyntaxException e1) {
            System.out.println("Error reading JSON data from file.");
        } catch (IOException e2) {
            System.out.println("Error reading file.");
        } finally {
            lock.unlock(); // Release the lock
        }
    }

    static void clearExpired() {
        while (true) {
            long currTime = System.currentTimeMillis();
            lastConnection.entrySet().removeIf(entry -> {
                boolean expired = (currTime - entry.getValue()) > 30000;
                if (expired) {
                    System.out.println("Station " + entry.getKey() + " has expired.");
                    weatherData.remove(entry.getKey());
                    try {
                        writeData();
                    } catch (IOException e) {
                        System.err.println("Error writing data to file when clearing old: " + e.getMessage());
                        // e.printStackTrace();
                    }
                }
                return expired;
            });

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.err.println("Error removing expired entries: " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    static void processRequests() {
        while (true) {
            ClientRequests request;
            synchronized (requests) {
                request = requests.poll();
            }

            if (request != null) {
                try {
                    request.process();
                } catch (IOException e) {
                    System.err.println("Error processing request: " + e.getMessage());
                    // e.printStackTrace();
                }
            }
        }
    }

    // Creates ServerSocket and listens for incoming connections
    static void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("===================================\n"
                    + "Server started on port: " + port);

            // Listen for incoming connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("-----------------------------------\n"
                        + "Connection established with client: " + clientSocket.getRemoteSocketAddress());

                // Create a new thread to handle the client connection
                Thread clientThread = new Thread(() -> connectionHandler(clientSocket));
                // System.out.println("Starting new thread for client: " + clientSocket.getRemoteSocketAddress());
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            // e.printStackTrace();
        }
    }

    // Unique connection handler for each client
    static void connectionHandler(Socket clientSocket) {
        try {
            // Set timeout for the client socket
            clientSocket.setSoTimeout(10000);

            // System.out.println("Handling connection with client: " + clientSocket.getRemoteSocketAddress());

            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            int clientLamportTime = 0;

            StringBuilder headers = new StringBuilder();
            String line;

            // Read headers
            // System.out.println("Reading headers from client: " + clientSocket.getRemoteSocketAddress());
            Map<String, String> headerMap = new HashMap<>();
            while (!(line = in.readUTF()).isEmpty()) {
                System.out.println(line);
                headers.append(line).append("\r\n");
                // Split the header into key and value
                String[] headerParts = line.split(": ", 2);
                if (headerParts.length == 2) {
                    headerMap.put(headerParts[0], headerParts[1]);
                }
            }

            // Extract headers from the client request
            System.out.println("Request headers: " + headers.toString());
            String clientLamportStr = headerMap.get("Lamport-Time");
            if (clientLamportStr != null) {
                clientLamportTime = Integer.parseInt(clientLamportStr);
                System.out.println("Client Lamport time: " + clientLamportTime);
            } else {
                System.out.println("No Lamport-Time header found.");
            }

            // Check if PUT or GET request
            String requestType = headerMap.get("Request-Type");
            String requestBody = "";
            if (!"PUT".equals(requestType) || !"GET".equals(requestType)) {
                // Invalid request type
                System.out.println("""
                        Invalid request type. Please use PUT or GET.
                        Closing connection with client: """ + clientSocket.getRemoteSocketAddress());
            } else if ("PUT".equals(requestType)) {
                // Validate the PUT request body if JSON
                requestBody = in.readUTF();
                if (!(requestBody.isEmpty())) { // If request is not empty
                    try {
                        @SuppressWarnings("unused")
                        JsonElement jsonElement = JsonParser.parseString(requestBody);
                    } catch (JsonSyntaxException e) {
                        System.out.println("Invalid JSON format in PUT request.");
                        out.writeUTF("Invalid JSON format in PUT request.");
                        out.flush();
                        return;
                    }
                }
            }

            // Sync the Lamport time
            synchronized (AggregationServer.class) {
                lamportClock.updateClock(clientLamportTime);
            }

            // Add request to queue
            ClientRequests request = new ClientRequests(clientSocket, requestType, headers.toString(), requestBody,
                    clientLamportTime);
            synchronized (requests) {
                requests.add(request);

            }
        } catch (IOException e) {
            System.err.println("Error handling connection: " + e.getMessage());
            // e.printStackTrace();
        }
    }

    // Method to update the weather data
    static boolean updateData(String stationID, JsonObject data) throws IOException {
        // If weather.json file does not exist, create it
        boolean fileExists = Files.exists(Paths.get(WEATHER));
        if (!fileExists) {
            Files.createFile(Paths.get(WEATHER));
        }

        lock.lock(); // Acquire the lock to prevent concurrent access
        try {
            weatherData.put(stationID, data); // Update the weather data
            lastConnection.put(stationID, System.currentTimeMillis()); // Update the last connection time

            // If more than 20 entries, remove the oldest entry
            if (weatherData.size() > 20) {
                String oldestKey = lastConnection.entrySet().stream().min(Map.Entry.comparingByValue()).get().getKey();
                weatherData.remove(oldestKey);
                lastConnection.remove(oldestKey);
            }

            // Write the updated data to the file
            writeData();
        } catch (IOException e) {
            System.err.println("Error updating weather data: " + e.getMessage());
            // e.printStackTrace();
        } finally {
            lock.unlock(); // Release the lock
        }

        return !fileExists;
    }

    static void writeData() throws IOException {
        // Create temp.json to write temporarily
        Path tempFile = Paths.get(TEMP);

        // Try to write the data to the temp file
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            for (String update : weatherData.keySet()) {
                writer.write(weatherData.get(update).toString());
                writer.newLine();
            }

            // Replace the original file with the temp file
            Files.move(tempFile, Paths.get(TEMP),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error writing data to file: " + e.getMessage());
            // e.printStackTrace();
        }
    }

    static int getClock() {
        return lamportClock.getTime();
    }
}