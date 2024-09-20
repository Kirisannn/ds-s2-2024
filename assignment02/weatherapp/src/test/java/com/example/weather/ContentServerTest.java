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

public class ContentServerTest {

    @Test
    public void testPutRequest() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Start the AggregationServer
        executor.submit(() -> {
            try {
                AggregationServer.main(new String[] { "8080" });
            } catch (Exception e) {
                System.err.println("Error starting server: " + e.getMessage());
            }
        });

        Thread.sleep(1000); // Allow time for the server to start

        // Send a PUT request
        try (Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("PUT");
            out.println("Weather data: Sample data from ContentServer");
            String response = in.readLine();
            assertEquals("PUT request received and data stored.", response);
        } catch (IOException e) {
            fail("Could not connect to server: " + e.getMessage());
        } finally {
            executor.shutdownNow(); // Stop the server thread
        }
    }
}
