package com.example.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
    public void testGetClient() {
        String response = "";

        try (Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET"); // Send GET request
            response = in.readLine(); // Read server response
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }

        assertEquals("Weather data: Sample weather data.", response);
    }

    @AfterAll
    public static void stopServer() {
        serverThread.interrupt();
    }
}
