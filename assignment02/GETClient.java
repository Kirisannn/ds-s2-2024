import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class GETClient {
    private static final String[] dateParts = { "January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December" };
    private static final String[] validAttributes = { "id", "name", "state", "time_zone", "lat", "lon",
            "local_data_time", "local_date_time_full", "air_temp", "apparent_t", "cloud", "dewpt",
            "press", "rel_hum", "wind_dir", "wind_spd_kmh", "wind_spd_kt" };

    public static void main(String[] args) {
        String serverURL = args[0];
        String stationID = null;

        // If args[1] not provided, default to "/weather", else use provided stationID
        // as argument
        // e.g. args[1] = "IDS12345", stationID = "IDS12345"
        if (args.length > 1) {
            stationID = "/?id=" + args[1];
        }

        // Pad the URL with "http://" if missing
        if (!serverURL.startsWith("http://")) {
            serverURL = "http://" + serverURL;
        }

        // If stationID is not null, append it to the serverURL
        if (stationID != null) {
            serverURL += stationID;
        }

        // // Print for debugging
        // System.out.println("Server URL: " + serverURL + "\nStation ID: " +
        // stationID);

        // URL validation
        try {
            URI uri = new URI(serverURL);
            String host = uri.getHost();
            Integer port = uri.getPort();
            // String param = uri.getQuery();

            // System.out.println("Host: " + host
            // + "\nPort: " + port
            // + "\nQueried ID: " + param + "\n"); // Debug output

            // If host empty or numbers only, print error message
            if (host == null || host.matches(".*\\d.*")) {
                System.err.println("Invalid URL format: Missing host.");
                System.exit(1);
            }

            // If port is invalid, print error message
            if (port == -1) {
                System.err.println("Invalid URL format: Missing port.");
            }

            // Should now be ready to make the HTTP request
            try {
                URL url = uri.toURL();
                // System.out.println("URL: " + url); // Debug output

                // Should now be ready to make the HTTP request
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Check the response code
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response from the server
                    StringBuilder jsonResponse = new StringBuilder();
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            jsonResponse.append(inputLine);
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading the server response: " + e.getMessage());
                    }

                    // Display the JSON response formatted
                    displayData(jsonResponse.toString());
                } else {
                    System.err.println("GET request failed. Station does not exist. Response Code: " + responseCode);
                }
            } catch (MalformedURLException e) {
                System.err.println("Invalid URL format: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Error opening connection: " + e.getMessage());
            }
        } catch (URISyntaxException e) {
            System.err.println("Invalid URL format: " + e.getMessage());
            System.exit(1);
        }

    }

    // Function to display JSON data in the required format
    private static void displayData(String jsonData) {
        try {
            // Parse JSON response string into a JsonArray
            JsonElement jsonElement = JsonParser.parseString(jsonData);
            if (!jsonElement.isJsonArray()) {
                throw new JsonSyntaxException("Expected a JSON array.");
            }

            System.out.println("============================================");
            // Iterate through the array and display each object
            for (JsonElement element : jsonElement.getAsJsonArray()) {
                JsonObject jsonObject = element.getAsJsonObject();

                // Store attribute key-value pairs into a map
                Map<String, String> attributeMap = new HashMap<>();
                for (String key : jsonObject.keySet()) {
                    attributeMap.put(key, jsonObject.get(key).getAsString());
                }

                // Display attributes
                displayAttributes(attributeMap);

                System.out.println("--------------------------------------------");
            }
            System.out.println("============================================");

        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON format. Failed to parse: " + e.getMessage());
        }
    }

    private static void displayAttributes(Map<String, String> attributeMap) {
        for (String attribute : validAttributes) {
            // If attribute in map exists in validAttributes, display it
            if (attributeMap.containsKey(attribute)) {
                String value = attributeMap.get(attribute);

                if (attribute.equals("local_date_time")) {
                    continue; // Skip this attribute
                }

                // Format date and time attributes
                if (attribute.equals("local_date_time_full")) {
                    String date = formatDate(value);
                    String time = formatTime(value);
                    System.out.println("Date: " + date);
                    System.out.println("Time: " + time);
                    continue;
                }

                System.out.println(attribute + ": " + value);
            }

        }
    }

    private static String formatDate(String fullDateTime) {
        String month = fullDateTime.substring(4, 6);
        String day = fullDateTime.substring(6, 8);

        // Convert month to int
        int monthInt = Integer.parseInt(month);
        month = dateParts[monthInt - 1]; // Get month name from array

        return day + " " + month;
    }

    private static String formatTime(String fullDateTime) {
        String hour = fullDateTime.substring(8, 10);
        String minute = fullDateTime.substring(10, 12);

        return hour + ":" + minute;
    }
}