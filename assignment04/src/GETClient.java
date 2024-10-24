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

            if (port < 0 || port > 65535) {
                System.err.println("Invalid port number (Out of range 0-65535)");
                System.exit(1);
            }
        }

        // If stationID provided
        String stationID = "";
        if (args.length > 1) {
            stationID = args[1];
            System.out.print("""
                    ---------------------------------------------------------------------------
                    Getting Station: """ + stationID + ", ");
        } else {
            System.out.print("""
                    ---------------------------------------------------------------------------
                    Getting Station: All Stations, """);
        }
        System.out.println("Connecting to { " + host + ":" + port + " }");

        // Retry logic
        int maxRetries = 5;
        int attempts = 0;
        boolean connected = false;

        while (attempts < maxRetries && !connected) {
            try (Socket client_socket = new Socket(host, port)) {
                connected = true;
                System.out.println("Successfully connected to server: { " + host + ":" + port + " }\n");

                OutputStream output = client_socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                // Send the formatted GET request message to the server
                writer.print(sendGET(stationID));
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

                // Split the server response into headers and body
                String[] responseParts = serverResponse.split("\n\n");
                String body = responseParts[1];

                // Extract first line as it is the status line
                String[] headerParts = responseParts[0].split("\n");
                String response = headerParts[0];
                System.out.println("Response: " + response);

                // Extract Lamport-Time from the headers
                JsonArray headers = new JsonArray();
                String headerLine;
                for (int i = 1; i < headerParts.length; i++) {
                    headerLine = headerParts[i];
                    String[] headerPartsArr = headerLine.split(": ");
                    JsonObject header = new JsonObject();
                    header.addProperty(headerPartsArr[0], headerPartsArr[1]);
                    headers.add(header);
                }

                // Get lamport time from headers
                for (JsonElement header : headers) {
                    JsonObject headerObj = header.getAsJsonObject();
                    if (headerObj.has("Lamport-Time")) {
                        int srcTime = headerObj.get("Lamport-Time").getAsInt();
                        clock.receive(srcTime);
                    }
                }

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
            } catch (IOException e) {
                System.err.println("Could not connect to server: " + host + ":" + port + ".\n" + e);
            } catch (Exception e) {
                System.err.println("Could not connect to server. Unknown error\n" + e);
            }

            if (!connected) {
                attempts++;
                if (attempts < maxRetries) {
                    System.out.println("Retrying connection... (" + attempts + "/" + maxRetries + ")\n");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.err.println("Failed to connect after " + maxRetries + " attempts.\n");
                    System.exit(1);
                }
            }
        }

        System.out.println("---------------------------------------------------------------------------");
    }

    static String[] getHostPort(String host_port) {
        String[] host_port_arr = host_port.split(":");
        return host_port_arr;
    }

    static String sendGET(String id) {
        String requestLine = "GET /weather.json HTTP/1.1\n";
        clock.increment();
        String headers = "User-Agent: ATOMClient/1/0\n" +
                "Accept: application/json" + "\n" +
                "id: " + id + "\n" +
                "Lamport-Time: " + clock.getTime() + "\n\n";

        return requestLine + headers;
    }

    static String sendUnsupported() {
        String requestLine = "POST /weather.json HTTP/1.1\n";
        clock.increment();
        String headers = "User-Agent: ATOMClient/1/0\n" +
                "Accept: application/json" + "\n" +
                "Lamport-Time: " + clock.getTime() + "\n\n";

        return requestLine + headers;
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
