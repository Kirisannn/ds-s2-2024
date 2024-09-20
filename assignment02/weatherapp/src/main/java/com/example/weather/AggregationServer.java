package com.example.weather;

// // Hello world!
// public class AggregationServer 
// {
//     public static void main( String[] args )
//     {
//         System.out.println( "Hello World!" );
//     }
// }

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class AggregationServer {
    public static void main(String[] args) {
        int port = 8080;

        // Accepts command-line argument for port number if provided
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Aggregation Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("""
                        ========================================
                        New client connected
                        InetAddress: """ + clientSocket.getInetAddress()
                        + ", Port: " + clientSocket.getPort() + "\n"
                        + "HostAddress: " + clientSocket.getInetAddress().getHostAddress());

                // Start a new thread to handle the request
                new RequestHandler(clientSocket).start();
            }
        } catch (Exception e) {
            System.err.println("Error Starting Server: " + e.getMessage());
        }
    }

}

// Thread to handle client connections (GET and PUT)
class RequestHandler extends Thread {
    private final Socket clientSocket;

    public RequestHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Read the request type (GET or PUT) from the client
            String requestType = in.readLine();
            if (requestType.equalsIgnoreCase("GET")) {
                // Handle GET request
                out.println("Weather data: Sample weather data.");
                System.out.println("Handled GET request.");
            } else if (requestType.equalsIgnoreCase("PUT")) {
                // Handle PUT request
                String weatherData = in.readLine(); // Read weather data
                System.out.println("Received PUT data: " + weatherData);
                out.println("PUT request received and data stored.");
            } else {
                out.println("Invalid request.");
            }

            System.err.println("========================================\n");

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Socket failed to close: " + e.getMessage());
            }
        }
    }
}