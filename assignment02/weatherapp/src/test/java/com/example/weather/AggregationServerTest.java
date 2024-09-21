package com.example.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

// Test class for the AggregationServer, checks for multi-client support
public class AggregationServerTest {

    private static final String BASE_URL = "http://localhost:4567/weather";

    @Test
    public void testMultipleClients() throws InterruptedException {
        int numberOfClients = 10; // Number of simulated clients
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfClients);

        for (int i = 0; i < numberOfClients; i++) {
            int clientId = i; // Capture the client ID for the thread
            executorService.submit(() -> {
                try {
                    // Simulate a GET request
                    URI uri = new URI(BASE_URL);
                    URL url = uri.toURL();
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    int responseCode = connection.getResponseCode();
                    assertEquals(HttpURLConnection.HTTP_OK, responseCode);

                    // Read response (optional, you can assert on response content too)
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        System.out.println("Client " + clientId + " received");
                    }
                } catch (IOException e) {
                    System.err.println("Error connecting to the server: " + e.getMessage());
                } catch (URISyntaxException e) {
                    System.err.println("Invalid URL: " + e.getMessage());
                }
            });
        }

        // Shutdown the executor service
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            // Wait for all tasks to finish
        }

        System.out.println("All clients have completed their requests.");
    }
}
