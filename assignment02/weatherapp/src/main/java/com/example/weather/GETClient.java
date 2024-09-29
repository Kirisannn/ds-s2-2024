package com.example.weather;

import java.net.URI;
import java.net.URISyntaxException;

public class GETClient {
    public static void main(String[] args) {
        String serverURL = args[0];
        String stationID = null;

        // If args[1] not provided, default to "/weather", else use provided stationID
        // as argument
        // e.g. args[1] = "IDS12345", stationID = "IDS12345"
        if (args.length > 1) {
            stationID = "?id=" + args[1];
        }
        // System.out.println("Station ID: " + stationID); // Print for debugging

        serverURL = serverURL + stationID;
        // System.out.println("Server URL: " + serverURL); // Print for debugging

        // Pad the URL with "http://" if missing
        if (!serverURL.startsWith("http://")) {
            serverURL = "http://" + serverURL;
        }

        // URL validation
        try {
            URI uri = new URI(serverURL);
            String host = uri.getHost();
            Integer port = uri.getPort();
            String endpoint = uri.getPath();

            System.out.println("Host: " + host
                    + "\nPort: " + port
                    + "\nEndpoint: " + endpoint); // Debug output

            // If endpoint is empty, default to "/weather"
            if (endpoint.isEmpty()) {
                endpoint = "/weather";
            }

            System.out.println("\nHost: " + host
                    + "\nPort: " + port
                    + "\nEndpoint: " + endpoint); // Debug output

            // If host empty or numbers only, print error message
            if (host == null || host.matches(".*\\d.*")) {
                System.err.println("Invalid URL format: Missing host.");
            }

            // If port is invalid, print error message
            if (port == -1) {
                System.err.println("Invalid URL format: Missing port.");
            }
        } catch (URISyntaxException e) {
            System.err.println("Invalid URL format: " + e.getMessage());
            System.exit(1);
        }
    }
}