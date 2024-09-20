package com.example.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

public class AggregationServerTest {

    @Test
    public void testGetRequest() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Start the server in a separate thread
        executor.submit(() -> {
            try {
                AggregationServer.main(new String[] { "8080" }); // You might need to modify AggregationServer to exit
                                                                 // cleanly
            } catch (Exception e) {
                System.err.println("Error starting server: " + e.getMessage());
            }
        });

        Thread.sleep(1000); // Wait for the server to start

        // Send a GET request
        try (Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET");
            String response = in.readLine();
            assertEquals("Weather data: Sample weather data.", response);
        } catch (IOException e) {
            fail("Could not connect to server: " + e.getMessage());
        } finally {
            executor.shutdownNow(); // Stop the server thread
        }
    }

    // Additional tests for PUT requests can be added here
}
