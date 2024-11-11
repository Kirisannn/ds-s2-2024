import java.io.*;
import java.net.*;
import org.json.*;
import java.util.*;

public class GETClient {
    // Maximum number for retry attempts on connecting to the server
    private static final int RETRY_COUNT = 4;
    // Define weather parameters to retrieved
    private static final String[] weatherParameters = { "id", "name", "state", "time_zone", "lat", "lon",
            "local_date_time", "local_date_time_full", "air_temp", "apparent_t", "cloud", "dewpt",
            "press", "rel_hum", "wind_dir", "wind_spd_kmh", "wind_spd_kt" };

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("java GETClient <server:port> [id1 id2 ...]");
            return;
        }
        // Parse the server name and port number from the first argument
        String[] serverInfo = args[0].split(":");
        String serverName = serverInfo[0];
        int portNumber = Integer.parseInt(serverInfo[1]);
        // Parse the list of IDs
        List<String> ids = args.length == 2 ? Arrays.asList(args[1].split(" ")) : null;

        int retryCounter = 0;
        boolean connection = false;
        
        while (retryCounter <= RETRY_COUNT && !connection) {
            if (retryCounter > 0) {
                System.out.println("Connection failed, retry attempt " + retryCounter + " of " + RETRY_COUNT);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // Try to establish a connection to the server
            try (Socket socket = new Socket(serverName, portNumber);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                // Build and send the GET request    
                StringBuilder request = new StringBuilder();
                request.append("GET / HTTP/1.1\r\n");
                request.append("\r\n");
                writer.print(request.toString());
                writer.flush();
                
                // Read the response from the server
                StringBuilder response = new StringBuilder();
                String line;
                boolean isResponseBody = false;
                while ((line = reader.readLine()) != null) {
                    if (isResponseBody) {
                        response.append(line);
                    }
                    if (line.isEmpty()) {
                        isResponseBody = true;
                    }
                }
                // Parse response into a JSONArray
                JSONArray jsonArray = new JSONArray(response.toString());
                if (jsonArray.isEmpty()) {
                    System.out.println("There is no weather data currently.");
                } else {
                    Map<String, JSONObject> weatherDataMap = new HashMap<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        weatherDataMap.put(jsonObject.getString("id"), jsonObject);
                    }
                    System.out.println("\n###################################\n");
                    if (ids != null) {
                        for (String id : ids) {
                            if (weatherDataMap.containsKey(id)) {
                                printWeatherData(weatherDataMap.get(id));
                                System.out.println("\n-------------------------\n");
                            }
                        }
                    } else {
                        for (JSONObject jsonObject : weatherDataMap.values()) {
                            printWeatherData(jsonObject);
                            System.out.println("\n-------------------------\n");
                        }
                        
                    }
                    System.out.println("\n###################################\n");
                }
                connection = true;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                retryCounter++;
            }
        }

        if (!connection) {
            System.out.println("Failed to connect after " + RETRY_COUNT + " attempts. Exiting.");
        }
    }

    private static void printWeatherData(JSONObject jsonObject) {
        for (String attribute : weatherParameters) {
            if (jsonObject.has(attribute)) {
                System.out.println(attribute + ": " + jsonObject.get(attribute));
            }
        }
    }
}