import java.io.*;
import java.net.*;

public class ContentServer {
    public static void main(String[] args) {
        String serverName = "localhost"; // Default server name
        int port = 8080; // Default port

        // Accept server name and port as command-line arguments
        if (args.length > 0) {
            serverName = args[0].split(":")[0];
            port = Integer.parseInt(args[0].split(":")[1]);
        }

        try (Socket socket = new Socket(serverName, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send PUT request and weather data
            out.println("PUT"); // Indicate PUT request
            out.println("Weather data: Sample data from ContentServer"); // Send weather data

            // Read server response
            String serverResponse = in.readLine();
            System.out.println("Server Response: " + serverResponse);

        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }
}
