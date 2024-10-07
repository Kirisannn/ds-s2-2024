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
     * Ensure that the specified file exists in the resources directory
     * for the test to pass.
     */
    @Test
    public void testValidateFilePath_ValidPath() {
        assertTrue(ContentServer.validateFilePath("IDS60901.txt"));
    }

    /**
     * Tests the validateFilePath method with an invalid file path.
     * 
     * This test checks the validateFilePath method to ensure that it
     * correctly identifies a non-existent file path as invalid.
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
     * object. It checks for the presence of key fields in the output.
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
     * error message when attempting to read from an empty file.
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
     * format.
     */
    @Test
    public void testReadData_InvalidLineFormat() {
        String result = ContentServer.readData("invalid_line.txt");
        assertEquals("{\"error\": \"Invalid line format: this_line_is_invalid\"}", result);
    }

    /**
     * Tests the formatPutRequest method.
     * 
     * This test verifies that the formatPutRequest method generates a
     * correctly formatted HTTP PUT request string based on the provided
     * parameters, including the host, request body, and Lamport time.
     */
    @Test
    public void testFormatPutRequest() {
        String request = ContentServer.formatPutRequest("localhost", "{\"key\":\"value\"}", 1);
        String expected = """
                PUT /weather.json HTTP/1.1\r
                Host: localhost\r
                Content-Type: application/json\r
                Content-Length: 15\r
                Lamport-Time: 1\r
                \r
                {"key":"value"}""";
        assertEquals(expected, request);
    }

    // @Test
    // public void testSendPutRequest_RetryOnFailure() throws Exception {
    //     // Mock the BufferedReader and DataOutputStream
    //     BufferedReader mockInput = mock(BufferedReader.class);
    //     DataOutputStream mockOutput = mock(DataOutputStream.class);

    //     // Simulate a failed response from the server (first four times)
    //     when(mockInput.readLine())
    //             .thenReturn("HTTP/1.1 500 Internal Server Error")
    //             .thenReturn("HTTP/1.1 500 Internal Server Error")
    //             .thenReturn("HTTP/1.1 500 Internal Server Error")
    //             .thenReturn("HTTP/1.1 500 Internal Server Error")
    //             .thenReturn("HTTP/1.1 200 OK"); // Simulate a successful response on the last attempt

    //     // Create a ContentServer instance (if necessary) and call the method
    //     String host = "localhost";
    //     String path = "IDS60901.txt";

    //     ContentServer.sendPutRequest(mockInput, mockOutput, host, path);

    //     // Verify that the method was called with expected behavior
    //     verify(mockOutput, times(5)).writeUTF(anyString()); // Verify that it tried to send the request 5 times
    //     verify(mockOutput, times(5)).flush(); // Verify that flush was called each time
    //     verify(mockInput, times(5)).readLine(); // Ensure readLine was called 5 times
    // }
}
