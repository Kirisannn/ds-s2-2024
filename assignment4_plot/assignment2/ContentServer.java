import java.io.*;
import java.net.*;
import java.nio.file.*;
import org.json.JSONObject;

public class ContentServer {
    private static int lamport_clock = 0; // Content server's Lamport clock

    // Main method
    public static void main(String [] args) {
        // Check if number of arguments from command line is correct
        if (args.length != 2) {
            System.err.println("Incorrect arguments, correct format: ContentServer <server_name>:<port_number> <file_path>");
            return;
        }

        // Get server URL and file path to content server's local weather data
        String server_address = args[0];
        String file_path = args[1];

        // Get server name and port number
        String[] name_port = server_address.split(":");
        if (name_port.length != 2) {
            System.err.println("Incorrect server URL, correct format: <server>:<port>");
            return;
        }

        String server_name = name_port[0];
        int port_number;
        try {
            port_number = Integer.parseInt(name_port[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number.");
            return;
        }

        // Initialize connection with aggregation server
        try {
            Socket socket = new Socket(server_name, port_number);
            DataOutputStream output_stream = new DataOutputStream(socket.getOutputStream());
            BufferedReader input_stream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            socket.setSoTimeout(5000);

            // Send the PUT request
            sendPutRequest(output_stream, input_stream, file_path);

            input_stream.close();
            output_stream.close();
            socket.close();

        } catch (IOException e) {
            System.err.println("Failed to send PUT request:  " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to send a PUT request, retrying maximum of 3 times if necessary
    // Inputs: DataOutputStream object and BufferedReader object
    // Output: None
    private static void sendPutRequest(DataOutputStream output_stream, BufferedReader input_stream, String file_path) throws IOException {
        // Read weather data from the file
        String weather_data;
        try {
            weather_data = readFromFile(file_path);
        } catch (IOException e) {
            System.err.println("Failed to read weather data from the file: " + e.getMessage());
            return;
        }

        // Retry sending request if previous time failed for a maximum of 3 times
        int max_retries = 3;
        while (max_retries != 0) {
            try {
                // Lamport clock incremented before sending PUT request
                lamport_clock++;

                // Create HTTP PUT request
                output_stream.writeUTF("PUT /weather.json HTTP/1.1\r\n");
                output_stream.writeUTF("User-Agent: ATOMClient/1/0\r\n");
                output_stream.writeUTF("Content-Type: application/json\r\n");
                output_stream.writeUTF("Content-Length: " + weather_data.length() + "\r\n");
                output_stream.writeUTF("Lamport-Clock: " + lamport_clock + "\r\n");
                output_stream.writeUTF("\r\n"); // End of headers
                output_stream.writeUTF(weather_data);
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
                if (server_response[0].contains("200 OK") || server_response[0].contains("201 CREATED")) {
                    System.out.println(server_response[0]);
                    
                    // Update content server's lamport clock
                    String clock_val = server_response[1].substring("Lamport-Clock: ".length());
                    int server_lamport_clock = Integer.parseInt(clock_val);
                    lamport_clock = Math.max(lamport_clock, server_lamport_clock) + 1;

                    System.out.println("PUT request successful. Updated content server Lamport clock: " + lamport_clock);
                    break;
                } else {
                    System.err.println("Failed to send PUT request. Server response: " + server_response[0]);
                    System.err.println("Retrying...");
                    max_retries--;
                }

            } catch (IOException e) {
                // To catch connection related issues
                System.err.println("Connection error during PUT request: " + e.getMessage());
                max_retries--;
                if (max_retries > 0) {
                    System.err.println("Retrying after " + 2000 / 1000 + " seconds...");
                    try {
                        Thread.sleep(2000);  // Wait before retrying
                    } catch (InterruptedException ie) {
                        // Handle any interruption while sleeping
                        Thread.currentThread().interrupt();
                        System.err.println("Retry interrupted. Exiting.");
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to send PUT request: " + e.getMessage());
                max_retries--;
            }
        }

        // If retried 3 times, report failure
        if (max_retries == 0) {
            System.err.println("Maximum retry attempts reached. Failed to send PUT request.");
        }
    }

    // // Method to read weather data from a file, parse it into JSON format and then return as String
    // Input: String of file path to local weather data file
    // Output: String of the weather data from JSON format
    private static String readFromFile(String file_path) throws IOException {
        JSONObject json_data = new JSONObject();
        try (BufferedReader reader = new BufferedReader(new FileReader(file_path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] key_value = line.split(":", 2); // To get key-value pairs
                
                // Ensure they are in pairs only
                if (key_value.length == 2) {
                    String key = key_value[0].trim();
                    String value = key_value[1].trim();
                    
                    // Parse the value as a double or integer
                    try {
                        if (value.contains(".")) {
                            json_data.put(key, Double.parseDouble(value));
                        } else {
                            json_data.put(key, Integer.parseInt(value));
                        }
                    } catch (NumberFormatException e) {
                        // If value is not a number, store as string
                        json_data.put(key, value);
                    }
                } else {
                    System.err.println("Invalid input file format: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return null;
        }
    
        if (!json_data.has("id")) {
            System.err.println("Error: Missing 'id' in weather data.");
            return null;
        }
    
        return json_data.toString();
    }
}