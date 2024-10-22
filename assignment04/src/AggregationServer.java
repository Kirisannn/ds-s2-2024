import java.io.*;
import java.net.*;
// import java.io.ObjectInputStream;
// import java.util.*;

// import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class AggregationServer {
    static final int DEFAULT_PORT = 4567;
    private static ServerSocket server_socket = null;
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
                Thread clientThread = new Thread(() -> handleClient(client_socket));

                // Start the thread
                clientThread.start();
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
                handleGetRequest(out);
            } else if (requestLine.startsWith("PUT")) {
                // Handle POST request
                handlePutRequest(in, out);
            } else {
                // Handle unsupported request
                handleUnsupportedRequests(out);
            }
        } catch (IOException e) {
            System.err.println("Error handling client request:\n" + e);
        }
    }

    private static void sendResponse(PrintWriter out, int statusCode, String responseBody, String statusMessage) {
        String statusLine = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n";
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

    private static void handleGetRequest(PrintWriter out) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleGetRequest'");
    }
}