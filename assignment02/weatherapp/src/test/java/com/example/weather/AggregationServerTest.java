package com.example.weather;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class AggregationServerTest {
    private static final int DEFAULT_PORT = 4567;

    @BeforeEach
    public void setup() {
        // Start the server on the default port before each test
        new Thread(() -> {
            String[] args = { String.valueOf(DEFAULT_PORT) };
            AggregationServer.main(args);
        }).start();

        // Wait for the server to start up
        try {
            Thread.sleep(2000); // Allow some time for the server to start
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterEach
    public void tearDown() {
        // Stop the server after each test
        AggregationServer.stopServer();
    }

    /**
     * Initial test to verify the server responds with status 201 for a valid first
     * PUT request.
     * This test must run first before concurrent updates.
     */
    @Test
    public void testPutFirstWeatherData() {
        String initialData = """
                {
                    "id": "IDS60901",
                    "name": "Adelaide (West Terrace / ngayirdapira)",
                    "state": "SA"
                }
                """;

        // Send the first PUT request to initialize the server with some data
        String resBody = given()
                .header("Lamport-Timestamp", "1")
                .body(initialData)
                .when()
                .put("http://localhost:" + DEFAULT_PORT + "/")
                .then()
                .statusCode(201)
                .extract().asString();

        // Assert that the response body contains the expected success message
        assert resBody.contains("{\"Success\": \"Weather data created!\"}");
    }

    /**
     * Test to verify 5 concurrent PUT requests update the server successfully.
     * This test runs after the initial PUT.
     */
    @Test
    public void testConcurrentPutWeatherData() throws InterruptedException {
        // Create a thread pool with 5 threads for concurrent requests
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // Data for concurrent PUT requests
        String[] weatherData = {
                """
                        {
                            "id": "IDS60901",
                            "name": "Adelaide (West Terrace / ngayirdapira)",
                            "state": "SA"
                        }
                        """,
                """
                        {
                            "id": "IDS60902",
                            "name": "Brisbane",
                            "state": "QLD"
                        }
                        """,
                """
                        {
                            "id": "IDS60903",
                            "name": "Sydney (Observatory Hill)",
                            "state": "NSW"
                        }
                        """,
                """
                        {
                            "id": "IDS60904",
                            "name": "Melbourne (Olympic Park)",
                            "state": "VIC"
                        }
                        """,
                """
                        {
                            "id": "IDS60905",
                            "name": "Perth",
                            "state": "WA"
                        }
                        """
        };

        // Submit 5 concurrent tasks
        for (int i = 0; i < 5; i++) {
            final int index = i; // To be used in the lambda
            executor.submit(() -> {
                String resBody = given()
                        .header("Lamport-Timestamp", String.valueOf(index + 2)) // Different timestamps for each request
                        .body(weatherData[index])
                        .when()
                        .put("http://localhost:" + DEFAULT_PORT + "/")
                        .then()
                        .statusCode(200) // Assuming updates return 200 OK
                        .extract().asString();

                assert resBody.contains("{\"Success\": \"Weather data updated!\"}");
            });
        }

        // Shutdown the executor and wait for the tasks to complete
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS); // Wait max 10 seconds for completion
    }

    /**
     * Test to verify the server responds with status 400 for malformed data in the
     * PUT request.
     * This test sends invalid JSON data to the server and expects a "Bad Request"
     * response.
     */
    @Test
    public void testPutMalformedWeatherData() {
        String malformedData = """
                {
                    "id": "IDS60906",
                    "name": "Hobart",
                    // Missing "state" field and extra trailing comma making it invalid JSON
                }
                """;

        // Send the malformed PUT request and expect a 400 Bad Request response
        String resBody = given()
                .header("Lamport-Timestamp", "3")
                .body(malformedData)
                .when()
                .put("http://localhost:" + DEFAULT_PORT + "/")
                .then()
                .statusCode(400) // Expecting the server to reject the malformed data
                .extract().asString();

        // Assert that the response body contains an error message
        assert resBody.equals("{\"Error\": \"Invalid JSON format.\"}");
    }
}