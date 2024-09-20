package com.example.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GETClientTest {
    private static Thread serverThread;

    @BeforeAll
    public static void startServer() {
        serverThread = new Thread(() -> {
            String[] args = { "8080" }; // Start server on port 8080
            AggregationServer.main(args);
        });
        serverThread.start();

        // Allow some time for the server to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testMultipleClients() throws InterruptedException {
        int numberOfClients = 5;
        CountDownLatch latch = new CountDownLatch(numberOfClients);
        String expectedResponse = "Weather data: Sample weather data.";

        for (int i = 0; i < numberOfClients; i++) {
            new Thread(() -> {
                String response = "";
                try (Socket socket = new Socket("localhost", 8080);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("GET"); // Send GET request
                    response = in.readLine(); // Read server response
                } catch (IOException e) {
                    System.err.println("Could not connect to server: " + e.getMessage());
                }

                // Assert the response in the context of the thread
                assertEquals(expectedResponse, response);
                latch.countDown(); // Decrement the latch count
            }).start();
        }

        latch.await(); // Wait for all clients to finish
    }

    @AfterAll
    public static void stopServer() {
        serverThread.interrupt();
    }
}
