import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.PriorityBlockingQueue;

public class AggregationServer {
    static final int DEFAULT_PORT = 4567;
    static ServerSocket server_socket = null;
    static JsonArray weatherData;
    static LamportClock clock = new LamportClock();
    static Socket client_socket;
    static File weather = new File("src/runtimeFiles/weather.json");
    static File backup = new File("src/runtimeFiles/backup_weather.json");
    static Boolean firstPUT = false;

    static final PriorityBlockingQueue<SimpleEntry<JsonArray, Integer>> requestQueue = new PriorityBlockingQueue<>(10,
            new Comparator<SimpleEntry<JsonArray, Integer>>() {
                @Override
                public int compare(SimpleEntry<JsonArray, Integer> o1, SimpleEntry<JsonArray, Integer> o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            });
    private static final Object dataLock = new Object();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        if (args.length > 0) { // Read arguments
            port = Integer.parseInt(args[0]);
            if (port < 0 || port > 65535) {
                System.err.println("Invalid port number (Out of range 0-65535)");
                System.exit(1);
            }
        }

        // Create server socket
        try {
            server_socket = new ServerSocket(port);
            System.out.println("Starting Server on {" + server_socket.getInetAddress().getHostAddress() + ":" + port
                    + "}...\nServer listening...\n");
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + port + ".\n" + e);
        } catch (Exception e) {
            System.err.println("Could not connect to server. Unknown error\n" + e);
            System.exit(1);
        } // End of server socket creation

        // Shutdown hook for clean up
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            if (server_socket != null && !server_socket.isClosed()) {
                try {
                    if (weather.exists()) { // Delete weather.json file
                        weather.delete();
                    }
                    if (backup.exists()) { // Delete backup_weather.json file
                        backup.delete();
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting weather data file:\n" + e);
                } finally {
                    System.out.println("Weather data file deleted successfully.");
                }

                try {
                    server_socket.close();
                    System.out.println("Server stopped successfully. Goodbye!");
                } catch (IOException e) {
                    System.err.println("Error while closing server socket:\n" + e);
                }
            }
        })); // End of shutdown hook

        loadWeatherData(); // Load weather data

        while (true) {
            try { // Listening for incoming connections
                client_socket = server_socket.accept(); // Connection from client

                System.out.println("===========================================================================");
                System.out.println("Connection {" + client_socket.getInetAddress().getHostAddress() + ":"
                        + client_socket.getPort() + "} accepted");

                new Thread(() -> handleClient(client_socket)).start(); // Create a new thread for the client
            } catch (IOException e) {
                System.err.println("Error accepting connection:\n" + e);
                break;
            } // End of connection catch block
        } // End of listening loop
    } // End of main

    private static void loadWeatherData() { // Load weather data from files
        if (backup.exists()) { // Load backup data if it exists
            try (BufferedReader reader = new BufferedReader(new FileReader(backup))) { // Load backup data
                System.out.println("Backup file exists. Loading backup weather data...\n");
                weatherData = JsonParser.parseReader(reader).getAsJsonArray();

                if (weather.exists()) { // If weather.json exists, overwrite it with backup data
                    try (FileWriter writer = new FileWriter(weather)) {
                        writer.write(weatherData.toString()); // Write backup data to weather.json
                    }
                }
            } catch (IOException | JsonSyntaxException e) { // If failed to load backup data, load weather.json if it
                                                            // exists
                System.err.println("Error loading backup data:\n" + e);
                try {
                    if (weather.exists()) {
                        System.out.println("Attempting to load from weather.json...\n");
                        try (BufferedReader reader = new BufferedReader(new FileReader(weather))) {
                            weatherData = JsonParser.parseReader(reader).getAsJsonArray();
                        }
                    }
                } catch (IOException | JsonSyntaxException ex) {
                    System.err.println("Error loading weather data:\n" + ex);
                    weatherData = new JsonArray();
                }
            } catch (Exception e) {
                System.err.println("Error loading backup data. Unknown error:\n" + e);
                try {
                    if (weather.exists()) {
                        System.out.println("Attempting to load from weather.json...\n");
                        try (BufferedReader reader = new BufferedReader(new FileReader(weather))) {
                            weatherData = JsonParser.parseReader(reader).getAsJsonArray();
                        }
                    } else {
                        weatherData = new JsonArray();
                    }
                } catch (IOException | JsonSyntaxException ex) {
                    System.err.println("Error loading weather data:\n" + ex);
                    weatherData = new JsonArray();
                }
            } finally {
                System.out.println("Backup data loaded successfully.\n");
            }
        } else { // If backup file does not exist, weather.json also does not exist
            weatherData = new JsonArray();
            // // Write the empty array to the file
            // try (FileWriter writer = new FileWriter(weather)) {
            // writer.write("[]");
            // System.out.println("Fresh start. Creating new empty weather data file...\n");
            // } catch (IOException e) {
            // System.err.println("Error creating weather file:\n" + e);
            // } catch (Exception e) {
            // System.err.println("Unknown error creating weather file.:\n" + e);
            // }
        }
    }

    private static void handleClient(Socket client_socket) { // Handle incoming requests
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
                PrintWriter out = new PrintWriter(client_socket.getOutputStream(), true)) { // Read the request
            System.out.println("---------------------------------------------------------------------------");
            String requestLine = in.readLine();
            System.out.println("Request received: " + requestLine);

            if (requestLine == null) { // If request is null, ignore
                System.err.println("Invalid request received. Ignoring...");
                return;
            }

            // Check request type
            if (requestLine.startsWith("GET")) { // Handle GET request
                handleGetRequest(in, out);
            } else if (requestLine.startsWith("PUT")) { // Handle PUT request
                handlePutRequest(in, out);
            } else { // Handle unsupported request
                handleUnsupportedRequests(out);
            }
        } catch (IOException e) { // Error handling client request
            System.err.println("Error handling client request:\n" + e);
        } finally {
            try { // Close client socket after handling request
                client_socket.close();
            } catch (IOException e) { // IO error closing client socket
                System.err.println("IO error closing client socket:\n" + e);
            } catch (Exception e) { // Unknown error closing client socket
                System.err.println("Unknown error closing client socket:\n" + e);
            }
        }

        System.out.println("Updated weather successfully.");
        System.out.println("---------------------------------------------------------------------------\n");
    }

    private static JsonArray[] parseHeaders(BufferedReader in) { // Parse headers and body from request
        try {
            JsonArray[] headersBody = new JsonArray[2];

            // Read in the headers as a String
            StringBuilder input = new StringBuilder();
            String line;
            int contentLength = 0;

            while ((line = in.readLine()) != null && !line.isEmpty()) { // Read headers until blank line
                input.append(line).append("\n");

                // Look for Content-Length header
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.split(": ")[1]);
                }
            }

            // System.out.println("\nHeaders:\n" + input); // Comment for debugging

            // Get headers as JsonArray
            String headerLines = input.toString();
            String[] headerLinesArray = headerLines.split("\n");

            // Determine if there is a body based on Content-Length
            boolean hasBody = contentLength > 0;

            // Find the number of headers (all lines before the blank line)
            int numHeaders = headerLinesArray.length;

            // Get headers
            JsonArray headers = new JsonArray();
            for (int i = 0; i < numHeaders; i++) {
                String[] headerParts = headerLinesArray[i].split(": ");
                JsonObject header = new JsonObject();
                if (headerParts.length == 1) {
                    header.addProperty(headerParts[0], "");
                } else if (headerParts.length == 2) {
                    header.addProperty(headerParts[0], headerParts[1]);
                }
                headers.add(header);
            }
            headersBody[0] = headers;

            // If there is a body, read it based on Content-Length
            if (hasBody) {
                char[] bodyChars = new char[contentLength];
                in.read(bodyChars, 0, contentLength);
                String body = new String(bodyChars);

                JsonArray bodyArray = JsonParser.parseString(body).getAsJsonArray();
                headersBody[1] = bodyArray;
            }

            return headersBody;
        } catch (IOException e) {
            System.err.println("Error parsing request headers:\n" + e);
        }

        return null;
    }

    // Send response to client
    private static void sendResponse(PrintWriter out, int statusCode, String responseBody, String statusMessage) {
        String statusLine = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n";
        clock.increment();
        String headers = "Content-Type: application/json\n" +
                "Content-Length: " + responseBody.length() + "\n" +
                "Connection: close\n" +
                "Lamport-Time: " + clock.getTime() + "\n\n";

        out.print(statusLine + headers + responseBody);
        out.flush();
    }

    // Handle unsupported requests
    private static void handleUnsupportedRequests(PrintWriter out) {
        String responseBody = "[{\"Error\": \"Unsupported request method. Only GET and PUT requests are supported.\"}]";
        sendResponse(out, 400, responseBody, "Bad Request");

        System.err.println("Unsupported request type. Only GET and PUT requests are supported.");
        System.out.println("---------------------------------------------------------------------------\n");

    }

    // Handle PUT request
    private static void handlePutRequest(BufferedReader in, PrintWriter out) {
        JsonArray[] headersBody = parseHeaders(in);
        JsonArray headers = headersBody[0];

        // Get lamport time from headers and update clock
        for (JsonElement header : headers) {
            JsonObject headerObj = header.getAsJsonObject();
            if (headerObj.has("Lamport-Time")) {
                int srcTime = headerObj.get("Lamport-Time").getAsInt();
                clock.receive(srcTime);
            }
        }

        // Get body as JsonArray
        JsonArray body = headersBody[1];

        SimpleEntry<JsonArray, Integer> request = new SimpleEntry<>(body, clock.getTime());
        requestQueue.add(request);

        // Attempt to process queue
        try {
            processQueue(out);
        } catch (Exception e) {
            System.err.println("Unknown error processing request queue:\n" + e);
        }
    }

    // Process the request queue
    private static void processQueue(PrintWriter out) {
        // While there are requests in the queue
        while (true) {
            try {
                // Take the next request from the queue
                SimpleEntry<JsonArray, Integer> request = requestQueue.take(); // This will block until an element is
                                                                               // available
                JsonArray body = request.getKey();
                if (body == null) {
                    sendResponse(out, 204,
                            "[{\"Success\": \"Successful connection, but no content was sent.\"}]",
                            "No Content");
                    break;
                }

                // Process the request
                Boolean success;
                synchronized (dataLock) { // Synchronise only on shared resource
                    success = updateData(body);
                }

                // Log the success status
                String resBody;
                if (success) {
                    resBody = "[{\"Success\": \"Weather data updated successfully.\"}]";
                } else {
                    resBody = "[{\"Error\": \"Failed to update weather data.\"}]";
                }

                // Send response
                if (firstPUT) {
                    sendResponse(out, 201, resBody, "Created");
                    firstPUT = false;
                } else {
                    sendResponse(out, 200, resBody, "OK");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                System.err.println("Processing thread interrupted:\n" + e);
                break; // Exit if interrupted
            } catch (Exception e) {
                System.err.println("Error processing request from queue:\n" + e);
            }

            // If there are no more requests in the queue, break
            if (requestQueue.isEmpty()) {
                break;
            }
        }
    }

    // Update weather data
    static synchronized Boolean updateData(JsonArray newData) {
        // Boolean notEmpty = false;

        try {
            if (!backup.exists()) {
                firstPUT = true;
            }

            if (newData != null) {
                // notEmpty = true;
                for (JsonElement newEntry : newData) {
                    JsonObject curr = newEntry.getAsJsonObject();
                    String id = curr.get("id").getAsString();
                    boolean found = false;

                    // Add clock time to entry with key "Lamport-Time"
                    curr.addProperty("Lamport-Time", clock.getTime());

                    // Search weather data for entry with id
                    for (JsonElement old : weatherData) {
                        JsonObject dataObj = old.getAsJsonObject();
                        if (dataObj.get("id").getAsString().equals(id)) {
                            // If entry is found, remove old entry and add new entry
                            weatherData.remove(old);
                            weatherData.add(curr);
                            found = true;
                            break;
                        }
                    }

                    // If entry with id is not found, add it
                    if (!found) {
                        weatherData.add(newEntry);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error updating weather data variable:\n" + e);
        }

        // Rename weather.json file as backup_weather.json
        try {
            if (weather.exists()) {
                weather.renameTo(backup);
            }
        } catch (Exception e) {
            System.err.println("Error renaming weather data file:\n" + e);
        }

        // Recreate weather.json file and write new data
        try (FileWriter writer = new FileWriter("src/runtimeFiles/weather.json")) {
            writer.write(weatherData.toString());
        } catch (IOException e) {
            System.err.println("Error writing to weather data file:\n" + e);
        }

        // Write new data to backup_weather.json
        try (FileWriter writer = new FileWriter("src/runtimeFiles/backup_weather.json")) {
            writer.write(weatherData.toString());
        } catch (IOException e) {
            System.err.println("Error writing to backup weather data file:\n" + e);
        }

        return true;
    }

    // Handle GET request
    private static void handleGetRequest(BufferedReader in, PrintWriter out) {
        JsonArray[] headersBody = parseHeaders(in);
        JsonArray headers = headersBody[0];
        String id = null;

        // Get lamport time and id from headers
        for (JsonElement header : headers) {
            JsonObject headerObj = header.getAsJsonObject();
            if (headerObj.has("Lamport-Time")) {
                int srcTime = headerObj.get("Lamport-Time").getAsInt();
                clock.receive(srcTime);
            }
            if (headerObj.has("id")) {
                id = headerObj.get("id").getAsString();
            }
        }

        // If id not provided, search weather data for entry with id
        if (!id.equals("")) {
            JsonArray responseBody = new JsonArray();
            // Search weather data for entry with id, if found, add to response
            for (JsonElement entry : weatherData) {
                JsonObject entryObj = entry.getAsJsonObject();
                if (entryObj.get("id").getAsString().equals(id)) {
                    responseBody.add(entryObj);
                }
            }

            if (responseBody.size() == 0) {
                // If no entry with id is found, return 404
                sendResponse(out, 404, responseBody.toString(), "Not Found");
            } else {
                // Send response
                sendResponse(out, 200, responseBody.toString(), "OK");
            }
        } else if (id.equals("")) {
            // If no id is provided, return all weather data
            sendResponse(out, 200, weatherData.toString(), "OK");
        }

        return;
    }
}