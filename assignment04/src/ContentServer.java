import java.io.*;
import java.net.*;
import java.nio.file.*;

import com.google.gson.*;

public class ContentServer {
    static final LamportClock clock = new LamportClock();
    static JsonArray weatherData;
    static int port;
    static String host;

    public static void main(String[] args) {
        // Check args
        if (args.length > 0) {
            // If host_port doesn't start with "http://"
            if (!args[0].startsWith("http://")) {
                // Get the host and port from the host_port
                String[] host_port = getHostPort(args[0]);
                host = host_port[0];
                port = Integer.parseInt(host_port[1]);
            } else {
                // Get the host and port from the host_port
                String[] host_port = getHostPort(args[0].substring(7));
                // Host from index 6 to the end
                host = host_port[0];
                port = Integer.parseInt(host_port[1]);
            }

            // Check if port is in range
            if (port < 0 || port > 65535) {
                System.err.println("Invalid port number (Out of range 0-65535)");
                System.exit(1);
            }
        }

        // Print host and port
        // System.out.println("Host: " + host); // Comment out
        // System.out.println("Port: " + port); // Comment out

        String filepath = null;
        if (args.length > 1) {
            // Check if file path is provided
            filepath = args[1];
            System.out.println("File path: " + filepath);
            // Read content from file
            try {
                weatherData = readContent(filepath);
            } catch (Exception e) {
                System.err.println("Error reading file: " + filepath);
                System.exit(1);
            }
        } else {
            System.err.println("File path not provided. Please provide a file path.");
            System.exit(1);
        }

        // Create socket connection to AggregationServer while not more than 5 retries.
        int retries = 0;
        Boolean success = false; // Indicates if successful update/creation of weather data on Aggregator
        while (!success && retries < 5) {
            // Create socket connection to AggregationServer
            try (Socket socket = new Socket(host, port)) {
                // Create input and output streams
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                String[] message = preparePut();

                System.out.println("Sending:\n" + message[0] + "\n" + message[1]);

                // Send weather data to AggregationServer
                writer.print(message[0]);
                writer.print("\n");
                writer.print(message[1]);

                writer.flush();

                // Read server response
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                // Read server response
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line).append("\n");
                }

                // Receive message from server
                String serverResponse = responseBuilder.toString().trim();

                // Split the server response into headers and body
                String[] responseParts = serverResponse.split("\n\n");

                // Receive response from AggregationServer
                success = printResponse(responseParts);
                if (success) {
                    break;
                }
            } catch (UnknownHostException e) {
                System.err.println("Unknown host: " + host + "\n" + e);
                System.exit(1);
            } catch (ConnectException e) {
                System.err.println("Connection refused. Please check if server is running on address (" +
                        host + ":" + port + ")\n" + e);
            } catch (IOException e) {
                System.err.println("Server IO error: " + host + "\n" + e);
            } catch (Exception e) {
                System.err.println("Unknown Error: " + e);
                System.exit(1);
            } finally {
                retries++;
            }

            // Sleep for 5 seconds before retrying
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.err.println("Error sleeping thread: " + e);
                System.exit(1);
            } finally {
                System.out.println("Retrying connection to server...");
            }
        }

        // If retries are more than 5, print error message
        if (retries >= 5) {
            System.err.println("Failed to connect to server after 5 retries. Please check server status.");
            System.exit(1);
        }
    }

    private static String[] preparePut() {
        // Format headers
        clock.increment(); // Increment Lamport clock as sending message

        String headers = "PUT /weather.json HTTP/1.1\n" +
                "Content-Type: application/json\n" +
                "Content-Length: " + weatherData.toString().getBytes().length + "\n" +
                "Lamport-Time: " + clock.getTime() + "\n";

        String[] message = { headers, weatherData.toString() };

        return message;
    }

    private static Boolean printResponse(String[] responseParts) {
        try {
            // Extract headers
            String headers = responseParts[0];
            System.out.println("\nServer Response:\n" + headers);
            String[] headerParts = responseParts[0].split("\n");
            // Get Lamport-Time from headers
            int receivedTime = Integer.parseInt(headerParts[4].split(": ")[1]);
            clock.receive(receivedTime);
            String body = responseParts[1];
            JsonArray dataArray = new JsonArray();
            try {
                dataArray = JsonParser.parseString(body).getAsJsonArray();
                printArray(dataArray);
            } catch (JsonSyntaxException e) {
                System.err.println("Syntax error when parsing server response as JSON Array\n" + e);
                System.exit(1);
            } catch (Exception e) {
                System.err.println("Unknown error when parsing server response as JSON Array\n" + e);
                System.exit(1);
            }

            System.out.println("---------------------------------------------------------------------------");
            return true;
        } catch (Exception e) {
            System.err.println("Unknown error parsing server response: " + e);
            System.exit(1);
        }

        return false;
    }

    static String[] getHostPort(String host_port) {
        String[] host_port_arr = host_port.split(":");
        return host_port_arr;
    }

    // Check file provided
    static JsonArray readContent(String filepath) {
        System.out.println("Reading content from file: " + filepath);
        try {
            // Read file as string
            String content = new String(Files.readAllBytes(Paths.get(filepath)));
            // Print content
            System.out.println("Content:\n\n" + content);

            // Take each line, extract key and pair, and add to JsonObject
            JsonArray jsonArray = new JsonArray();
            JsonObject jsonObject = new JsonObject();
            String[] lines = content.split("\n");
            for (String line : lines) {
                String[] key_value = line.split(":");
                if (key_value.length == 2) { // Common case
                    jsonObject.addProperty(key_value[0], key_value[1]);
                } else if (key_value.length == 1) { // Empty value
                    jsonObject.addProperty(key_value[0], "");
                } else if (key_value.length == 3) { // Short date format
                    jsonObject.addProperty(key_value[0], key_value[1] + ":" + key_value[2]);
                } else { // Invalid key-value pair
                    throw new JsonSyntaxException("\nInvalid key-value pair. Please check file for proper format.\n" +
                            "Error in line: " + line);
                }
            }
            jsonArray.add(jsonObject);

            // Print the JsonArray
            // System.out.println("JsonArray:\n\n" + jsonArray); // Comment out

            return jsonArray;
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filepath + "\n" + e);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error reading file: " + filepath + "\n" + e);
            System.exit(1);
        } catch (JsonSyntaxException e) {
            System.err.println("\nInvalid JSON syntax in file: " + filepath + "\n(Error) " + e);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error reading file. Unknown Error: " + filepath + "\n" + e);
            System.exit(1);
        }

        return null;
    }

    static void printArray(JsonArray dataArray) {
        StringBuilder sb = new StringBuilder();
        for (JsonElement element : dataArray) {
            JsonObject obj = element.getAsJsonObject();
            // append key-value pairs to the string builder without renaming, remove all "
            for (String key : obj.keySet()) {
                sb.append(key).append(": ").append(obj.get(key)).append("\n");
            }
            sb.append("\n");
        }

        String out = sb.toString();

        System.out.println(out);
    }
}
