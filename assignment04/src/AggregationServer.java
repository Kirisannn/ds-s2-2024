import java.io.*;
import java.net.*;
// import java.io.ObjectInputStream;
// import java.util.*;

// import com.google.gson.JsonElement;
import com.google.gson.*;

public class AggregationServer {
    static final int DEFAULT_PORT = 4567;
    private static ServerSocket server_socket = null;
    @SuppressWarnings("unused")
    private static JsonArray weatherData;
    static LamportClock clock = new LamportClock();

    public static void main(String[] args) {
        // Read arguments
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
            if (port < 0 || port > 65535) {
                System.err.println("Invalid port number (Out of range 0-65535)");
                System.exit(1);
            }
        }

        // Create server socket
        try {
            server_socket = new ServerSocket(port);
            System.out.println("Starting Server...\nListening on port " + port + "...");
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + port + ".\n" + e);
        } catch (Exception e) {
            System.err.println("Could not connect to server. Unknown error\n" + e);
            System.exit(1);
        }

        // Shutdown hook for SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            if (server_socket != null && !server_socket.isClosed()) {
                // Delete weather.json file
                try {
                    File file = new File("src/runtimeFiles/weather.json");
                    if (file.exists()) {
                        file.delete();
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
        }));

        // Load weather data
        loadWeatherData();

        // Listening for incoming connections
        while (true) {
            try {
                Socket client_socket = server_socket.accept();

                // Create a new thread for the client
                new Thread(() -> handleClient(client_socket)).start();
            } catch (IOException e) {
                System.err.println("Error accepting connection:\n" + e);
                break;
            }
        }
    }

    private static void loadWeatherData() {
        File file = new File("src/runtimeFiles/weather.json");

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                System.out.println("Data file exists. Loading weather data from file...\n");
                weatherData = JsonParser.parseReader(reader).getAsJsonArray();
            } catch (IOException | JsonSyntaxException e) {
                System.err.println("Error loading weather data from weather.json:\n" + e);
                weatherData = new JsonArray();
            } catch (Exception e) {
                System.err.println("Error loading weather data from weather.json. Unknown error:\n" + e);
                weatherData = new JsonArray();
            } finally {
                System.out.println("Weather data loaded successfully.\n");
            }
        } else {
            weatherData = new JsonArray();
            // Write the empty array to the file
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("[]");
                System.out.println("Data file does not exist. Creating new empty weather data file...\n");
            } catch (IOException e) {
                System.err.println("Error creating weather data file:\n" + e);
            } catch (Exception e) {
                System.err.println("Error creating weather data file. Unknown error:\n" + e);
            }
        }
    }

    private static void handleClient(Socket client_socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
                PrintWriter out = new PrintWriter(client_socket.getOutputStream(), true)) {
            // Read the request
            String requestLine = in.readLine();
            System.out.println("---------------------------------------------------------------------------");
            System.out.println("Request received: " + requestLine);

            // Parse the request
            if (requestLine == null) {
                System.err.println("Invalid request received. Ignoring...");
                return;
            }

            // Check request type
            if (requestLine.startsWith("GET")) {
                // Handle GET request
                handleGetRequest(in, out);
            } else if (requestLine.startsWith("PUT")) {
                // Handle PUT request
                handlePutRequest(in, out);
            } else {
                // Handle unsupported request
                handleUnsupportedRequests(out);
            }
        } catch (IOException e) {
            System.err.println("Error handling client request:\n" + e);
        }
    }

    private static JsonArray[] parseHeaders(BufferedReader in) {
        try {
            JsonArray[] headersBody = new JsonArray[2];

            // Read in as String
            StringBuilder input = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                input.append(line).append("\n");
            }

            System.out.println("\n" + input); // Uncomment for debugging

            // Get headers as JsonArray
            String headerLines = input.toString();
            String[] headerLinesArray = headerLines.split("\n");

            Boolean hasBody = false;
            if (headerLinesArray.length > 4) {
                hasBody = true;
            }

            // Get headers
            JsonArray headers = new JsonArray();
            for (int i = 0; i < 4; i++) {
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

            if (hasBody) {
                JsonArray body = new JsonArray();
                body.add(headerLinesArray[4]);
            }

            return headersBody;
        } catch (IOException e) {
            System.err.println("Error parsing request headers:\n" + e);
        }

        return null;
    }

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

    private static void handleUnsupportedRequests(PrintWriter out) {
        String responseBody = "[{\"Error\": \"Unsupported request method. Only GET and PUT requests are supported.\"}]";
        sendResponse(out, 400, responseBody, "Bad Request");

        System.err.println("Unsupported request type. Only GET and PUT requests are supported.");
        System.out.println("---------------------------------------------------------------------------\n");

    }

    private static void handlePutRequest(BufferedReader in, PrintWriter out) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handlePostRequest'");
    }

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

        // Now to get required data

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