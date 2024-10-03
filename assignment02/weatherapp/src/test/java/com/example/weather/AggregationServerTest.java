package com.example.weather;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;

public class AggregationServerTest {
    private static final int DEFAULT_PORT = 4567;
    private static final int TEST_PORT = 8080;

    // Call setup method before running any tests other than alternate port testing
    public static void setUp() {
        // Start the server on the default port
        String[] args = { String.valueOf(DEFAULT_PORT) };
        new Thread(() -> {
            AggregationServer.main(args);
        }).start();
        RestAssured.port = DEFAULT_PORT;
    }

    /**
     * Test to verify the server starts successfully on the test port.
     * Expected output: The server should start without throwing exceptions.
     */
    @Test
    public void testServerStartOnTestPort() {
        new Thread(() -> {
            String[] args = { String.valueOf(TEST_PORT) };
            AggregationServer.main(args);
        }).start();

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
                .get("http://localhost:" + TEST_PORT + "/")
                .then()
                .statusCode(200) // Should return 200 OK
                .extract().asString();

        assert resBody.contains("[]"); // Should return an empty JSON array
    }

    /**
     * Test to verify the server responds with status 201 for a valid first PUT
     * request.
     * Expected output: HTTP status 201 and a success message.
     */
    @Test
    public void testPutFirstWeatherData() {
        setUp();
        String jsonData = """
                    {
                        "id": "123",
                        "air_temp": 22,
                        "apparent_t": 23,
                        "cloud": "clear",
                        "dewpt": 10,
                        "press": 1012,
                        "rel_hum": 30,
                        "wind_dir": "N",
                        "wind_spd_kmh": 15,
                        "wind_spd_kt": 8,
                        "local_date_time": "2024-10-03T12:00:00Z",
                        "local_date_time_full": "2024-10-03T12:00:00.000Z"
                    }
                """;

        String resBody = given()
                .header("Lamport-Timestamp", "1")
                .body(jsonData)
                .when()
                .put("/")
                .then()
                .statusCode(201)
                .extract().asString();

        assert resBody.contains("{\"Success\": \"Weather data created!\"}");
    }

}