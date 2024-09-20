package com.example.weather;

// Hello world!
// public class GETClient {
//     public static void main(String[] args) {
//         System.out.println("Hello World!");
//     }
// }

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GETClient {
    public static void main(String[] args) {
        String serverName = "localhost"; // Default server name
        int port = 8080; // Default port

        // Accept server name and port as command-line arguments
        if (args.length > 0) {
            serverName = args[0].split(":")[0]; // Extract server name
            port = Integer.parseInt(args[0].split(":")[1]); // Extract port number
        }

        try (Socket socket = new Socket(serverName, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET"); // Indicate GET request

            String serverResponse = in.readLine(); // Read server response
            System.out.println("Server Response: " + serverResponse); // Display server response

        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }
}
