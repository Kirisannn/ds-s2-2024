import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
// import java.io.ObjectInputStream;
// import java.util.*;

// import com.google.gson.JsonElement;
// import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonArray;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class AggregationServer {
    private static final int DEFAULT_PORT = 4567;
    private static ServerSocket server_socket = null;
    private static JsonArray weatherData;

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

        // Listening for incoming connections
        while (true) {
            try {
                Socket client_socket = server_socket.accept();
                String client_ip = client_socket.getInetAddress().toString();
                String client_port = Integer.toString(client_socket.getPort());
                System.out.println("Client connected: " + client_ip + ":" + client_port);

                // Respond to client
                OutputStream output = client_socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                String server_ip = client_socket.getLocalAddress().getHostAddress(); // Get server's IP address
                writer.println("Connected to server at " + server_ip + ":" + port); // Send the formatted message

                // Close the connection with the client
                try {
                    client_socket.close();
                    System.out.println("Client " + client_ip + ":" + client_port + " disconnected.");
                } catch (IOException e) {
                    System.err.println("Error closing client connection:\n" + e);
                }
            } catch (IOException e) {
                System.err.println("Error accepting client connection:\n" + e);
                break;
            }
        }
    }

    private static void loadWeatherData() {
        File file = new File("runtimeFiles/weather.json");

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

}