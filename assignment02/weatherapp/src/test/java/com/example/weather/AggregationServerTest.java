package com.example.weather;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class AggregationServerTest {
    // Set the url of the server
    private static final String URL = "http://localhost:4567";
    private static final String SOURCE_FILE_PATH = "src/main/resources/weather.json"; // Relative path to weather.json
    private static final String BACKUP_FILE_PATH = "weather_test_copy.json"; // Backup path, relative to the project
                                                                             // root

    @BeforeAll
    public static void setup() {
        // Start the AggregationServer on a specific port
        AggregationServer.main(new String[] { "4567" });
    }

    @AfterAll
    public static void tearDown() throws IOException {
        // Define paths for source and backup files
        Path sourcePath = Path.of(SOURCE_FILE_PATH).toAbsolutePath(); // Get the absolute path of weather.json
        Path backupPath = Path.of(BACKUP_FILE_PATH).toAbsolutePath(); // Get the absolute path for the backup file

        // Ensure the backup directory exists
        Files.createDirectories(backupPath.getParent());

        // Copy the file
        Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Test the initial PUT request to create a weather resource.
     * This test checks if the server responds with a 201 status code
     * when a valid weather resource is created.
     *
     * Expected Response:
     * - Status code: 201
     */
    @Test
    public void testPutWeather() {
        // Define the JSON body to be sent in the PUT request
        String jsonBody = """
                {
                    "id": "IDS60901",
                    "name": "Adelaide (West Terrace / ngayirdapira)",
                    "state": "SA",
                    "time_zone": "CST",
                    "lat": -34.9,
                    "lon": 138.6,
                    "local_date_time": "21/05:00pm",
                    "local_date_time_full": "20230921170000",
                    "air_temp": 13.3,
                    "apparent_t": 9.5,
                    "cloud": "Partly cloudy",
                    "dewpt": 5.7,
                    "press": 1023.9,
                    "rel_hum": 60,
                    "wind_dir": "S",
                    "wind_spd_kmh": 15,
                    "wind_spd_kt": 8
                }""";

        String resBody = given()
                .baseUri(URL) // Set the base URI for the request
                .contentType("application/json") // Set the content type to JSON
                .header("Lamport-Timestamp", 1) // Add the Lamport timestamp header
                .body(jsonBody) // Set the request body
                .when()
                .put("/weather") // Replace with your actual endpoint for creating weather data
                .then()
                .statusCode(201) // Assert that the response status code is 201
                .extract().asString(); // Extract the response body as a String

        // Assert that the response body contains the expected message
        assert resBody.contains("\"Success\": \"Weather data created!\"");
    }

    /**
     * Test sending a PUT request with a valid body but without the Lamport
     * timestamp.
     * This test checks if the server responds with a 400 status code, indicating a
     * bad request.
     *
     * Expected Response:
     * - Status code: 400
     */
    @Test
    public void testPutWeatherWithoutLamportTimestamp() {
        String jsonBody = "{ \"id\": \"IDS60906\", \"name\": \"Hobart (Salamanca)\", \"state\": \"TAS\", \"time_zone\": \"AEDT\", \"lat\": -42.88, \"lon\": 147.33, \"local_date_time\": \"21/05:00pm\", \"local_date_time_full\": \"20230921220000\", \"air_temp\": 14.0, \"apparent_t\": 13.0, \"cloud\": \"Mostly cloudy\", \"dewpt\": 8.0, \"press\": 1016.0, \"rel_hum\": 75, \"wind_dir\": \"NW\", \"wind_spd_kmh\": 10, \"wind_spd_kt\": 5 }";

        given()
                .baseUri(URL) // Set the base URI for the request
                .contentType("application/json") // Set the content type to JSON
                .body(jsonBody) // Set the request body
                .when()
                .put("/weather") // Replace with your actual endpoint for creating weather data
                .then()
                .statusCode(400); // Assert that the response status code is 400

    }

    /**
     * Test sending a PUT request with incorrect JSON format.
     * This test checks if the server responds with a 400 status code, indicating a
     * bad request.
     *
     * Expected Response:
     * - Status code: 400
     */
    @Test
    public void testPutWeatherWithIncorrectJson() {
        // Invalid JSON: missing quotes for the "id" field and an extra comma
        String invalidJsonBody = "{ id: 'IDS60907', name: 'Hobart (Salamanca)', state: 'TAS', time_zone: 'AEDT', lat: -42.88, lon: 147.33, local_date_time: '21/05:00pm', local_date_time_full: '20230921220000', air_temp: 14.0, apparent_t: 13.0, cloud: 'Mostly cloudy', dewpt: 8.0, press: 1016.0, rel_hum: 75, wind_dir: 'NW', wind_spd_kmh: 10, wind_spd_kt: 5, }";

        given()
                .baseUri(URL) // Set the base URI for the request
                .contentType("application/json") // Set the content type to JSON
                .header("Lamport-Timestamp", 1) // Include a Lamport timestamp
                .body(invalidJsonBody) // Set the request body to the invalid JSON
                .when()
                .put("/weather") // Replace with your actual endpoint for creating weather data
                .then()
                .statusCode(400); // Assert that the response status code is 400
    }

    /**
     * Test sending multiple concurrent PUT requests to create weather resources.
     * This test checks if the server responds with a 200 status code for all
     * requests.
     * Five different weather resources are created concurrently.
     *
     * Expected Response:
     * - Status code: 200 for each request
     */
    @Test
    public void testConcurrentPutWeather() throws InterruptedException, ExecutionException {
        // Define the JSON bodies to be sent in the PUT requests
        String[] jsonBodies = {
                "{ \"id\": \"IDS60901\", \"name\": \"Adelaide (West Terrace / ngayirdapira)\", \"state\": \"SA\", \"time_zone\": \"CST\", \"lat\": -34.9, \"lon\": 138.6, \"local_date_time\": \"21/05:00pm\", \"local_date_time_full\": \"20230921170000\", \"air_temp\": 13.3, \"apparent_t\": 9.5, \"cloud\": \"Partly cloudy\", \"dewpt\": 5.7, \"press\": 1023.9, \"rel_hum\": 60, \"wind_dir\": \"S\", \"wind_spd_kmh\": 15, \"wind_spd_kt\": 8 }",
                "{ \"id\": \"IDS60902\", \"name\": \"Melbourne (CBD)\", \"state\": \"VIC\", \"time_zone\": \"AEDT\", \"lat\": -37.8, \"lon\": 144.96, \"local_date_time\": \"21/05:00pm\", \"local_date_time_full\": \"20230921180000\", \"air_temp\": 15.0, \"apparent_t\": 12.0, \"cloud\": \"Clear\", \"dewpt\": 6.0, \"press\": 1020.0, \"rel_hum\": 55, \"wind_dir\": \"SE\", \"wind_spd_kmh\": 10, \"wind_spd_kt\": 5 }",
                "{ \"id\": \"IDS60903\", \"name\": \"Sydney (Central Park)\", \"state\": \"NSW\", \"time_zone\": \"AEDT\", \"lat\": -33.9, \"lon\": 151.2, \"local_date_time\": \"21/05:00pm\", \"local_date_time_full\": \"20230921190000\", \"air_temp\": 18.0, \"apparent_t\": 16.0, \"cloud\": \"Overcast\", \"dewpt\": 9.0, \"press\": 1015.0, \"rel_hum\": 70, \"wind_dir\": \"N\", \"wind_spd_kmh\": 20, \"wind_spd_kt\": 10 }",
                "{ \"id\": \"IDS60904\", \"name\": \"Brisbane (Fortitude Valley)\", \"state\": \"QLD\", \"time_zone\": \"AEST\", \"lat\": -27.5, \"lon\": 153.0, \"local_date_time\": \"21/05:00pm\", \"local_date_time_full\": \"20230921200000\", \"air_temp\": 22.0, \"apparent_t\": 21.0, \"cloud\": \"Sunny\", \"dewpt\": 11.0, \"press\": 1018.0, \"rel_hum\": 60, \"wind_dir\": \"NE\", \"wind_spd_kmh\": 25, \"wind_spd_kt\": 12 }",
                "{ \"id\": \"IDS60905\", \"name\": \"Perth (City)\", \"state\": \"WA\", \"time_zone\": \"AWST\", \"lat\": -31.9, \"lon\": 115.85, \"local_date_time\": \"21/05:00pm\", \"local_date_time_full\": \"20230921210000\", \"air_temp\": 19.0, \"apparent_t\": 18.0, \"cloud\": \"Partly cloudy\", \"dewpt\": 7.0, \"press\": 1022.0, \"rel_hum\": 65, \"wind_dir\": \"W\", \"wind_spd_kmh\": 15, \"wind_spd_kt\": 8 }"
        };

        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[jsonBodies.length];

        // Create and send concurrent PUT requests
        IntStream.range(0, jsonBodies.length).forEach(i -> {
            futures[i] = CompletableFuture.runAsync(() -> {
                given()
                        .baseUri(URL) // Set the base URI for the request
                        .contentType("application/json") // Set the content type to JSON
                        .header("Lamport-Timestamp", i) // Use the index as the Lamport timestamp
                        .body(jsonBodies[i]) // Set the request body
                        .when()
                        .put("/weather") // Replace with your actual endpoint for creating weather data
                        .then()
                        .statusCode(200); // Assert that the response status code is 201
            });
        });

        // Wait for all requests to complete
        CompletableFuture.allOf(futures).join();
    }
}