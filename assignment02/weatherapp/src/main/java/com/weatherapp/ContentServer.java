package com.weatherapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;

import com.google.gson.Gson;

/**
 * The ContentServer class is responsible for reading weather data from a file
 * and sending it to the AggregationServer via HTTP PUT requests. It accepts
 * two command-line arguments: the server URL and the weather data file path.
 */
public class ContentServer {
    // Lamport clock for timestamping requests
    private static final LamportClock lamportClock = new LamportClock();
    private static volatile boolean running = true; // Flag to control the main loop

    /**
     * The main method serves as the entry point for the ContentServer. It verifies
     * if the required arguments (server URL and file path) are provided, then
     * checks if the URL and file path are valid before proceeding with the
     * operations.
     *
     * @param args Command-line arguments where:
     *             args[0] is the server URL (e.g., "http://servername:portnumber")
     *             args[1] is the file path to the weather data (txt file only)
     */
    public static void main(String[] args) throws IOException, InterruptedException, ConnectException {
        if (args.length != 2) {
            System.err.println("Usage: java ContentServer <serverURL> <filePath>");
            System.exit(1);
        }
        String serverURL = args[0]; // Server URL
        String filePath = args[1]; // File path to weather data

        if (!validateURL(serverURL)) {
            System.err.println("""
                    Invalid server URL format.
                    Please follow the formats:
                    "http://servername.domain.domain:portnumber",
                    "http://servername:portnumber", or
                    "servername:portnumber" (with implicit protocol information).
                    """);
            System.exit(1);
        }

        if (!validateFilePath(filePath)) {
            System.err.println("""
                        File not found in resources directory. Please provide a txt file path to the weather data at:
                        src/main/resources/<filename>.txt
                    """);
            System.exit(1);
        }

        // Extract host and port
        String[] hostPort = serverURL.split(":");
        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);

        // Install shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown signal received. Cleaning up...");
            running = false; // Stop the main loop
        }));

        try (Socket socket = new Socket(host, port);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(5000); // In case server times out

            sendPutRequest(input, output, host, filePath);

        } catch (Exception ex) {
            System.err.println("An error occurred: " + ex.getMessage());
        }
    }

    /**
     * Validates if the provided URL is in the correct format.
     * 
     * This method checks if the server URL follows one of the valid formats:
     * "http://servername.domain.domain:portnumber", "http://servername:portnumber",
     * or "servername:portnumber" (with implicit protocol information).
     *
     * @param url The server URL to be validated.
     * @return true if the URL format is valid, false otherwise.
     */
    static boolean validateURL(String url) {
        // If the URL does not start with http:// or https://,
        // we assume it's a shorthand version (like localhost:8080)
        boolean hasProtocol = url.startsWith("http://") || url.startsWith("https://");

        // If no protocol, treat the whole URL as domain and port
        String domainAndPort;
        if (hasProtocol) {
            // Remove the protocol prefix
            domainAndPort = url.substring(url.indexOf("://") + 3);
        } else {
            domainAndPort = url; // Use the original URL for shorthand
        }

        // Split the domain part into potential domain and port
        String[] parts = domainAndPort.split(":");
        if (parts.length > 2 || parts.length < 2) {
            return false; // Invalid format
        }
        String domain = parts[0];

        // Check that the domain is not empty and contains valid characters
        if (domain.isEmpty() || !domain.matches("^[a-zA-Z0-9.-]+$") || domain.startsWith(".") || domain.endsWith("-")) {
            return false;
        }

        // If there's a port, ensure it's a valid number
        if (parts.length > 1) {
            String port = parts[1];
            if (!port.matches("\\d+")) {
                return false; // Port must be a number
            }
        }

        return true; // Passed all checks
    }

    /**
     * Validates if the provided file path is correct and points to an existing
     * file.
     * 
     * This method checks if the file path is valid and if the file exists on the
     * local system. The method will return true if the file is found and can be
     * read.
     *
     * @param filePath The file path to the weather data.
     * @return true if the file exists and is valid, false otherwise.
     */
    static boolean validateFilePath(String filePath) {
        if (!filePath.endsWith(".txt")) { // Check if the file is a text file
            return false;
        }

        // Attempt to find the resource in the classpath
        InputStream inputStream = ContentServer.class.getResourceAsStream("/" + filePath);
        return inputStream != null; // Returns true if the file exists
    }

    /**
     * Reads the weather data from the file and returns it as a JSON object.
     * 
     * This method reads the weather data from the provided file path and converts
     * it into a JSON object. The JSON object will be used to send the data to the
     * AggregationServer.
     * 
     * If the file contains non-JSON format, the method will return a JSON object
     * with an appropriate error message in the format:
     * {
     * "error": "Data not JSON."
     * }
     * 
     * or if the file is empty:
     * {
     * "error": "File is empty."
     * }
     *
     * @param filePath The file path to the weather data.
     * @return A JSON object containing the weather data or an error message.
     */
    static String readData(String filePath) {
        WeatherData weatherData = new WeatherData();

        try (InputStream inputStream = ContentServer.class.getResourceAsStream("/" + filePath);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            // Check if the InputStream is null (file not found)
            if (inputStream == null) {
                return "{\"error\": \"File not found.\"}";
            }

            // Check if the file is empty
            String firstLine = reader.readLine();
            if (firstLine == null) {
                return "{\"error\": \"File is empty.\"}";
            }

            // Process the first line
            processLine(firstLine, weatherData);

            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Process subsequent lines
                processLine(line, weatherData);
            }
        } catch (Exception e) {
            // Handle exceptions
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }

        // Use Gson to convert the WeatherData object to a JSON string
        Gson gson = new Gson();
        return gson.toJson(weatherData); // Return the constructed JSON string
    }

    /**
     * Processes a line from the file and updates the WeatherData object
     * accordingly.
     *
     * This method splits a line into a key-value pair and updates the provided
     * WeatherData object based on the key. If the key is unexpected or in an
     * invalid format, an IllegalArgumentException will be thrown.
     *
     * @param line        The line to be processed.
     * @param weatherData The WeatherData object to update.
     * @throws IllegalArgumentException if the line format is invalid or the key is
     *                                  unknown.
     */
    private static void processLine(String line, WeatherData weatherData) {
        // Split the line into key and value
        String[] parts = line.split(":", 2);
        if (parts.length == 2) {
            String key = parts[0].trim();
            String value = parts[1].trim();

            // Set the corresponding field in the WeatherData object
            switch (key) {
                case "id" -> weatherData.setId(value);
                case "name" -> weatherData.setName(value);
                case "state" -> weatherData.setState(value);
                case "time_zone" -> weatherData.setTimeZone(value);
                case "lat" -> weatherData.setLat(Double.parseDouble(value));
                case "lon" -> weatherData.setLon(Double.parseDouble(value));
                case "local_date_time" -> weatherData.setLocalDateTime(value);
                case "local_date_time_full" -> weatherData.setLocalDateTimeFull(value);
                case "air_temp" -> weatherData.setAirTemp(Double.parseDouble(value));
                case "apparent_t" -> weatherData.setApparentTemp(Double.parseDouble(value));
                case "cloud" -> weatherData.setCloud(value);
                case "dewpt" -> weatherData.setDewpt(Double.parseDouble(value));
                case "press" -> weatherData.setPress(Double.parseDouble(value));
                case "rel_hum" -> weatherData.setRelHum(Integer.parseInt(value));
                case "wind_dir" -> weatherData.setWindDir(value);
                case "wind_spd_kmh" -> weatherData.setWindSpdKmh(Double.parseDouble(value));
                case "wind_spd_kt" -> weatherData.setWindSpdKt(Double.parseDouble(value));
                default -> {
                    // Handle unexpected keys
                    throw new IllegalArgumentException("Invalid key: " + key);
                }
            }
        } else {
            // Handle the case where the line does not have the expected format
            throw new IllegalArgumentException("Invalid line format: " + line);
        }
    }

    /**
     * Sends an HTTP PUT request to the AggregationServer with the weather data.
     * 
     * This method reads the weather data, establishes a connection to the server,
     * and attempts to send the PUT request. It handles retries in case of
     * connection
     * failures and processes the server's response to ensure the data is
     * successfully updated.
     *
     * @param input  The BufferedReader for reading the server's response.
     * @param output The DataOutputStream for sending the PUT request to the server.
     * @param host   The host of the AggregationServer.
     * @param path   The path to the weather data file.
     */
    static void sendPutRequest(BufferedReader input, DataOutputStream output, String host, String path) {
        String data = readData(path); // Read the weather data from the file

        // Infinite loop to keep sending requests, controlled by the running flag
        while (running) { // Check if shutdown was requested
            int retries = 1;
            boolean successfulRequest = false;

            while (retries > 0 && !successfulRequest && running) { // Retry loop with running flag
                try {
                    lamportClock.incrementTime();

                    // Format the PUT request
                    String request = formatPutRequest(host, data, lamportClock.getTime());

                    // Send the request
                    output.writeUTF(request);
                    output.flush();

                    // Read the response
                    String responseLine;
                    StringBuilder responseBuilder = new StringBuilder();

                    while ((responseLine = input.readLine()) != null) {
                        responseBuilder.append(responseLine).append("\n");
                    }

                    String[] response = responseBuilder.toString().split("\n");
                    String statusLine = response[0];
                    String statusCode = statusLine.split(" ")[1];

                    if ("200".equals(statusCode) || "201".equals(statusCode)) {
                        lamportClock.updateClock(Integer.parseInt(response[1]));
                        System.out.println("Data updated successfully. Server Response: " + statusLine);
                        successfulRequest = true;
                    } else {
                        retries--;
                        System.err.println("Connection failed. Server Response: " + statusLine + "\n"
                                + "Attempts left: " + retries + "\nRetrying...");
                    }

                } catch (IOException e) {
                    retries--;
                    System.err.println("Connection failed. Attempts left: " + retries
                            + "\nRetrying...");
                    if (retries == 0 && running) {
                        System.err.println(
                                "Failed to update data. Max retries used. Waiting 5 seconds before next attempt...");
                        sleep(5000);
                    }
                }
            }

            if (running) {
                sleep(15000); // Sleep for 15 seconds between requests
            }
        }

        System.out.println("ContentServer shut down gracefully.");
    }

    /**
     * Formats an HTTP PUT request with the given host and JSON data.
     *
     * This method constructs a valid HTTP PUT request string using the provided
     * host, JSON data, and Lamport timestamp.
     *
     * @param host        The host to which the request is sent.
     * @param jsonString  The JSON data to be sent in the body of the request.
     * @param lamportTime The current time of the Lamport clock.
     * @return A formatted HTTP PUT request as a String.
     */
    static String formatPutRequest(String host, String jsonString, int lamportTime) {
        return String.format("""
                PUT /weather.json HTTP/1.1\r
                Host: %s\r
                Content-Type: application/json\r
                Content-Length: %d\r
                Lamport-Time: %d\r
                \r
                %s""", host, jsonString.length(), lamportTime, jsonString);
    }

    /**
     * Sleep for the specified duration.
     * 
     * @param milliseconds Duration in milliseconds.
     */
    static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
    }

}
