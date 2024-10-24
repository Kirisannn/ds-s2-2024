import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;

import com.google.gson.*;

public class ContentServer {
    static final LamportClock clock = new LamportClock();
    static JsonArray weatherData;

    public static void main(String[] args) {
        // Read arguments
        int port = 0;
        String host = null;
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

        if (args.length > 1) {
            // Check if file path is provided
            String filepath = args[1];
            System.out.println("File path: " + filepath);
            try {
                weatherData = readContent(filepath);
            } catch (Exception e) {
                System.err.println("Error reading file: " + filepath);
                System.exit(1);
            }
        }

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
                if (key_value.length == 2) {
                    jsonObject.addProperty(key_value[0], key_value[1]);
                } else {
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
}
