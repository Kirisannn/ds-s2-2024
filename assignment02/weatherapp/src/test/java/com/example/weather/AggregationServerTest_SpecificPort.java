package com.example.weather;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class AggregationServerTest_SpecificPort {
    // Call setup method before running any tests other than alternate port testing
    public static void setup(int port) {
        // Start the server on the default port
        new Thread(() -> {
            String[] args = { String.valueOf(port) };
            AggregationServer.main(args);
        }).start();
    }

    private static int getRandomPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get a random free port", e);
        }
    }

    /**
     * Test to verify the server starts successfully on the test port.
     * Expected output: The server should start without throwing exceptions.
     */
    @Test
    public void testServerStartOnTestPort() {
        int port = getRandomPort();
        setup(port);

        // Allow some time for the server to start
        try {
            Thread.sleep(2000); // Wait for 2 seconds to allow the server to start
        } catch (InterruptedException e) {
            Assertions.fail("Thread interrupted: " + e.getMessage());
        }

        // Check if the server is running on the test port by making a GET request
        String resBody = given()
                .header("Lamport-Timestamp", 1) // Add the Lamport timestamp header
                .when()
                .get("http://localhost:" + port + "/")
                .then()
                .statusCode(200) // Should return 200 OK
                .extract().asString();

        assert resBody.equals("[]"); // Should return an empty JSON array
    }
}