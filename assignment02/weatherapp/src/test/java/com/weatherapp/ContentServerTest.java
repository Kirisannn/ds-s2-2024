package com.weatherapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ContentServerTest {

    /**
     * Tests the validateURL method with valid URL inputs.
     * 
     * This test checks various valid URL formats to ensure that the
     * validateURL method correctly identifies them as valid. It includes
     * full URLs with HTTP, as well as simplified localhost formats.
     */
    @Test
    public void testValidateURL_ValidURL() {
        assertTrue(ContentServer.validateURL("http://localhost:8080"));
        assertTrue(ContentServer.validateURL("localhost:8080"));
        assertTrue(ContentServer.validateURL("http://example.com:80"));
        assertTrue(ContentServer.validateURL("http://example.domain.com:8080"));
    }

    /**
     * Tests the validateURL method with invalid URL inputs.
     * 
     * This test checks various invalid URL formats to ensure that the
     * validateURL method correctly identifies them as invalid. It includes
     * malformed URLs and improper port numbers.
     */
    @Test
    public void testValidateURL_InvalidURL() {
        assertFalse(ContentServer.validateURL("invalid_url"));
        assertFalse(ContentServer.validateURL("http://example"));
        assertFalse(ContentServer.validateURL("http://.com:8080")); // Invalid domain
        assertFalse(ContentServer.validateURL("localhost:abc")); // Invalid port
    }

    /**
     * Tests the validateFilePath method with a valid file path.
     * 
     * This test checks the validateFilePath method to confirm that it
     * correctly recognizes a valid file path in the resources directory.
     * It assumes that the specified file exists in the resources directory
     * for the test to pass successfully.
     */
    @Test
    public void testValidateFilePath_ValidPath() {
        assertTrue(ContentServer.validateFilePath("IDS60901.txt"));
    }

    /**
     * Tests the validateFilePath method with an invalid file path.
     * 
     * This test checks the validateFilePath method to ensure that it
     * correctly identifies a non-existent file path as invalid. This helps
     * verify the method's ability to handle file paths that do not correspond
     * to actual files.
     */
    @Test
    public void testValidateFilePath_InvalidPath() {
        assertFalse(ContentServer.validateFilePath("nonexistent_file.txt"));
    }

    /**
     * Tests the readData method with a valid JSON file.
     * 
     * This test verifies that the readData method successfully reads
     * a valid JSON file and correctly populates the resulting JSON
     * object. It checks for the presence of expected key fields in the output,
     * ensuring that the data is formatted correctly.
     */
    @Test
    public void testReadData_ValidJSON() {
        String jsonResult = ContentServer.readData("IDS60901.txt");
        JsonObject jsonObject = JsonParser.parseString(jsonResult).getAsJsonObject();
        assertNotNull(jsonObject);
        assertTrue(jsonObject.has("id"));
        assertTrue(jsonObject.has("name"));
    }

    /**
     * Tests the readData method with an empty file.
     * 
     * This test checks that the readData method returns an appropriate
     * error message when attempting to read from an empty file. This
     * ensures that the method can gracefully handle the case of missing data.
     */
    @Test
    public void testReadData_EmptyFile() {
        String result = ContentServer.readData("empty_file.txt");
        JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
        assertEquals("File is empty.", jsonObject.get("error").getAsString());
    }

    /**
     * Tests the readData method with a file containing invalid line format.
     * 
     * This test ensures that the readData method returns an error message
     * when it encounters a line that does not conform to the expected key-value
     * format. It validates the method's robustness against malformed input.
     */
    @Test
    public void testReadData_InvalidLineFormat() {
        String result = ContentServer.readData("invalid_line.txt");
        assertEquals("{\"error\": \"Invalid line format: this_line_is_invalid\"}", result);
    }

    /**
     * Tests the formatPutRequest method.
     * 
     * This test verifies that the formatPutRequest method generates the
     * expected formatted PUT request string when provided with valid
     * input parameters, including the host, JSON string, and Lamport time.
     */
    @Test
    public void testFormatPutRequest() {
        // Given
        String host = "localhost:4567";
        String jsonString = "{\"id\":\"123\",\"temperature\":25.5,\"humidity\":60}";
        int lamportTime = 5;

        // When
        String result = ContentServer.formatPutRequest(host, jsonString, lamportTime);

        // Expected output
        String expected = """
                PUT /weather.json HTTP/1.1
                Host: localhost:4567
                Content-Type: application/json
                Content-Length: 54
                Lamport-Time: 5

                {"id":"123","temperature":25.5,"humidity":60}""";

        // Then
        assertEquals(expected, result); // Assert that the result matches the expected output
    }
}
