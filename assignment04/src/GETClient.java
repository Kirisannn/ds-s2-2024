import java.io.*;
import java.net.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
public class GETClient {
    static final LamportClock clock = new LamportClock();

    public static void main(String[] args) {
        // Read argunts
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

            if (port < 0 || port > 65535) {
                System.err.println("Invalid port number (Out of range 0-65535)");
                System.exit(1);
            }
        }

        // If stationID provided
        if (args.length > 1) {
            String stationID = args[1];
            System.out.print("""
                    ---------------------------------------------------------------------------
                    Updating Station: """ + stationID + ", ");
        }
        System.out.println("Connecting to { " + host + ":" + port + " }");

        // Create client socket
        try (Socket client_socket = new Socket(host, port)) {
            System.out.println("Successfully connected to server: { " + host + ":" + port + " }\n");

            OutputStream output = client_socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            // Send the formatted GET request message to the server
            writer.print(sendUnsupported());
            writer.flush();

            // Read server response
            InputStream input = client_socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            // Read server response
            StringBuilder responseBuilder = new StringBuilder();
            String line;

            // Read the response until the end
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line).append("\n");
            }

            // Receive message from server
            String serverResponse = responseBuilder.toString().trim();
            // System.out.println("Server Response:\n" + serverResponse); // Uncomment for
            // debugging
            // System.out.println(serverResponse); // Uncomment for debugging

            // Split the server response into headers and body
            String[] responseParts = serverResponse.split("\n\n");
            String headers = responseParts[0];
            String body = responseParts[1];

            // Process the server response as JsonArray
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

        } catch (UnknownHostException e) {
            System.err.println("Could not connect to server: " + host + ":" + port + ". Unknown host\n" + e);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Could not connect to server: " + host + ":" + port + ".\n" + e);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Could not connect to server. Unknown error\n" + e);
            System.exit(1);
        }

        System.out.println("---------------------------------------------------------------------------");
    }

    static String[] getHostPort(String host_port) {
        String[] host_port_arr = host_port.split(":");
        return host_port_arr;
    }

    static String sendGET() {
        return """
                GET /weather.json HTTP/1.1
                User-Agent: ATOMClient/1/0
                Accept: application/json
                Lamport-Time: XX

                """;
    }

    static String sendUnsupported() {
        return """
                POST /weather.json HTTP/1.1
                User-Agent: ATOMClient/1/0
                Accept: application/json
                Lamport-Time: XX

                """;
    }

    static void printArray(JsonArray dataArray) {
        StringBuilder sb = new StringBuilder();
        for (JsonElement element : dataArray) {
            JsonObject obj = element.getAsJsonObject();
            // append key-value pairs to the string builder without renaming, remove all "
            for (String key : obj.keySet()) {
                sb.append(key).append(": ").append(obj.get(key)).append("\n");
            }
        }

        String out = sb.toString();

        System.err.println(out);
    }
}
