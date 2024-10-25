import java.io.*;
import java.net.*;
import java.nio.file.*;

import com.google.gson.*;

public class ContentServer {
    static final LamportClock clock = new LamportClock();
    static JsonArray weatherData;
    static int port;
    static String host;
    static Socket socket;
    static int client_port;

    public static void main(String[] args) {
        // Parse host and port from arguments
        if (args.length > 0) {
            String[] host_port = getHostPort(args[0]);
            host = host_port[0];
            port = Integer.parseInt(host_port[1]);
        }

        // Shutdown hook to close socket
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e);
            } catch (Exception e) {
                System.err.println("Unknown error closing socket: " + e);
            } finally {
                System.out.println("Socket closed.");
            }
        }));

        // Bind to a randomly selected available port only once, outside the loop
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            client_port = serverSocket.getLocalPort(); // Assign a random available port
            System.out.println("Selected client port for binding: " + client_port);

            // Close the server socket after getting the port
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Could not find an available port to bind: " + e);
            System.exit(1);
        }

        String filepath = args.length > 1 ? args[1] : null;
        if (filepath == null) {
            System.err.println("File path not provided. Please provide a file path.");
            System.exit(1);
        }

        // Main loop to continuously send data to the server
        while (true) {
            // Load file content into weatherData
            try {
                weatherData = readContent(filepath);
            } catch (Exception e) {
                System.err.println("Error reading file: " + filepath);
                System.exit(1);
            }

            boolean success = false;
            int retries = 0;

            while (!success && retries < 5) {
                try {
                    // Create and bind socket once per retry attempt
                    socket = new Socket();
                    socket.bind(new InetSocketAddress("localhost", client_port));
                    socket.connect(new InetSocketAddress(host, port));

                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    String[] message = preparePut();
                    System.out.println("Sending:\n" + message[0] + "\n" + message[1]);

                    // Send weather data to AggregationServer
                    writer.print(message[0]);
                    writer.print("\n");
                    writer.print(message[1]);
                    writer.flush();

                    // Read server response
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line).append("\n");
                    }
                    String serverResponse = responseBuilder.toString().trim();
                    String[] responseParts = serverResponse.split("\n\n");

                    success = printResponse(responseParts);

                } catch (IOException e) {
                    System.err.println("Connection error: " + e.getMessage());
                } finally {
                    retries++;
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.err.println("Error closing socket: " + e);
                    }
                }

                // Retry delay
                if (!success && retries < 5) {
                    try {
                        System.out.println("Retrying connection...");
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted during retry delay: " + e);
                    }
                }
            }

            if (!success) {
                System.err.println("Failed to connect after 5 retries. Exiting.");
                System.exit(1);
            }

            // Wait before next send
            try {
                Thread.sleep(30000); // 30 seconds
            } catch (InterruptedException e) {
                System.err.println("Main loop interrupted: " + e);
            }
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
