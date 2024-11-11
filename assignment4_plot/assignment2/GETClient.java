import java.io.*;
import java.net.*;
import org.json.JSONObject;

public class GETClient {
    private static int lamport_clock = 0; // Client's Lamport clock

    // Main method
    // Input: string arguments from command line
    // Output: None
    public static void main(String[] args) {
        // Check if number of arguments from command line is correct
        if (args.length < 1 || args.length > 2) {
            System.err.println("Incorect arguments, correct format: GETClient <server>:<port> [station_id]");
            return;
        }

        // Get server URL (name & port) and optional station ID
        String server_address = args[0];
        String station_id = args.length == 2 ? args[1] : null;

        // Get server name and port
        String[] name_port = server_address.split(":");
        if (name_port.length != 2) {
            System.err.println("Incorrect server URL, correct format: server_name:port_number");
            return;
        }

        String server_name = name_port[0];
        int port_number;
        try {
            port_number = Integer.parseInt(name_port[1]);
        } catch (NumberFormatException e) {
            System.err.println("Incorrect port number");
            return;
        }

        // Initialize connection with aggregation server
        try {
            Socket socket = new Socket(server_name, port_number);
            DataOutputStream output_stream = new DataOutputStream(socket.getOutputStream());
            BufferedReader input_stream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            socket.setSoTimeout(5000);

            // Send the GET request
            sendGetRequest(output_stream, input_stream, station_id);

            input_stream.close();
            output_stream.close();
            socket.close();

        } catch (IOException e) {
            System.err.println("Failed to send GET request:  " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to send a GET request
    // Input: DataOutputStream object, BufferedReader object and string of station_id if available
    // Output: None
    public static void sendGetRequest(DataOutputStream output_stream, BufferedReader input_stream, String station_id) {
        // Retry sending request if previous time failed for a maximum of 3 times
        int max_retries = 3;
        while (max_retries != 0) {
            try {
                // Lamport clock incremented before sending GET request
                lamport_clock++;

                // Create HTTP GET request
                output_stream.writeUTF("GET / HTTP/1.1\r\n");

                // If station_id is provided, add to request
                if (station_id != null) {
                    output_stream.writeUTF("Station-ID: " + station_id + "\r\n");
                }
                output_stream.writeUTF("Lamport-Clock: " + lamport_clock + "\r\n");
                output_stream.writeUTF("\r\n"); // End of headers
                output_stream.flush();

                // Add a small delay after sending the request
                Thread.sleep(100);

                // Get server response
                StringBuilder response_builder = new StringBuilder();
                String response_line;
                while ((response_line = input_stream.readLine()) != null) {
                    response_builder.append(response_line).append("\r\n");
                }

                // Check the status code
                String full_response = response_builder.toString();
                String[] server_response = full_response.split("\r\n");
                if (server_response[0].contains("200 OK")) {
                    // Check if the JSON data is empty
                    if (server_response[server_response.length - 1].trim().equals("{}")) {
                        System.out.println("Failed to send GET request. No data available.");
                        break;
                    }

                    // Update GET client's lamport clock
                    String clock_val = server_response[1].substring("Lamport-Clock: ".length());
                    int server_lamport_clock = Integer.parseInt(clock_val);
                    lamport_clock = Math.max(lamport_clock, server_lamport_clock) + 1;

                    System.out.println("GET request successful. Updated GET client Lamport clock: " + lamport_clock);
                    displayWeatherData(server_response[server_response.length - 1]);
                    break;
                } else {
                    System.err.println("Failed to send GET request. Server response: " + server_response[0]);
                    System.err.println("Retrying...");
                    max_retries--;
                }
                
            } catch (Exception e) {
                System.err.println("Error sending GET request: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        } 

        // If retried 3 times, report failure
        if (max_retries == 0) {
            System.err.println("Maximum retry attempts reached. Failed to send GET request.");
        }
    }

    // Method to display returned weather data
    // Input: String of JSON data from server response
    // Output: Display of weather data for that specific station or the latest update in terminal
    public static void displayWeatherData(String weather_data) {
        System.out.println("Displaying returned weather data:");

        try {
            // Parse the weather data from the JSON string
            JSONObject json = new JSONObject(weather_data);

            // Iterate through each key-value pair in the JSON object
            for (String key : json.keySet()) {
                Object value = json.get(key);
                System.out.println(key + ": " + value.toString());
            }

        } catch (Exception e) {
            System.err.println("Error parsing or displaying weather data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}