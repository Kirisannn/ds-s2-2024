import java.io.*;
import java.net.*;
import org.json.*;

public class ContentServer {
    // Maximum times for retry attempts for connnecting to server
    private static final int RETRY_COUNT = 4;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("java ContentServer <server:port> <inputFileName>");
            return;
        }
        // Parse server information from the first argument exp: "localhost:8080"
        String[] serverInfo = args[0].split(":");
        String serverName = serverInfo[0];
        int portNumber = Integer.parseInt(serverInfo[1]);
        // Get the input file name from the second argument
        String inputFileName = args[1];
        // Create LamportClock instance and load current clock value 
        LamportClock lamportClock = new LamportClock();
        lamportClock.loadClock("lamportclock.txt");
         // Load the JSON array from the input file
        JSONArray jsonArray = loadJsonArray(inputFileName);
        if (jsonArray == null) {
            return;
        }

        int retryCounter = 0;
        boolean connection = false;
        // Attempt connection to server
        while (retryCounter <= RETRY_COUNT && !connection) {
            if (retryCounter > 0) {
                System.out.printf("Retrying connection, attempt %d of %d%n", retryCounter, RETRY_COUNT);
                try {
                    // Waits 10 seconds before next attempt
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Restore interrupt status
                    Thread.currentThread().interrupt();
                    System.err.println("Thread was interrupted, Failed to complete operation");
                }
            }
            // Attempt to establish a socket connection with the server
            try (Socket socket = new Socket()) {
                // Set connection timeout and read timeout 
                socket.connect(new InetSocketAddress(serverName, portNumber), 1000);
                socket.setSoTimeout(1000);

                // Open streams to send & receive data through socket
                try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    // Increment the Lamport clock and save the updated value to the file
                    lamportClock.tick();
                    lamportClock.saveClock("lamportclock.txt");
                    // Build the PUT request
                    String putRequest = String.format(
                            "PUT /weather.json HTTP/1.1\r\n" +
                            "User-Agent: ATOMClient/1/0\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: %d\r\n" +
                            "Lamport-Clock: %d\r\n\r\n",
                            jsonArray.toString().length(), lamportClock.getTime()
                    );
                    // Send PUT request and JSON data
                    writer.print(putRequest);
                    writer.print(jsonArray.toString());
                    writer.flush();

                    ResponseHeaders(reader, lamportClock);
                }
                connection = true;
            } catch (SocketTimeoutException e) {
                andleRetryAttempts(retryCounter, "Connection Failed after %d retries. Aborting.");
                retryCounter++;
            } catch (IOException e) {
                andleRetryAttempts(retryCounter, "Connect to server Error: " + e.getMessage());
                retryCounter++;
            }
        }

        if (!connection) {
            System.out.printf("Connection Failed after %d attempts. Exiting.%n", RETRY_COUNT);
        }
    }

    private static JSONArray loadJsonArray(String inputFileName) {
        JSONArray jsonArray = new JSONArray();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFileName))) {
            String line;
            JSONObject jsonObject = null;
            while ((line = br.readLine()) != null) {
                // If new ID is found, add previous JSON object to array
                if (line.startsWith("id:")) {
                    if (jsonObject != null) {
                        jsonArray.put(jsonObject);
                    }
                    // Create new JSON object and add the ID
                    jsonObject = new JSONObject();
                    jsonObject.put("id", line.substring(3).trim());
                } else if (jsonObject != null) {
                    // Add other key-value pairs to JSON object
                    int colonIndex = line.indexOf(':');
                    if (colonIndex >= 0) {
                        String key = line.substring(0, colonIndex).trim();
                        String value = line.substring(colonIndex + 1).trim();
                        jsonObject.put(key, value);
                    }
                }
            }
            if (jsonObject != null) {
                jsonArray.put(jsonObject);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return jsonArray;
    }

    private static void ResponseHeaders(BufferedReader reader, LamportClock lamportClock) throws IOException {
        String responseHeader;
        // Read each line from the BufferedReader until an empty line is encountered
        while ((responseHeader = reader.readLine()) != null && !responseHeader.isEmpty()) {
            if (responseHeader.startsWith("Lamport-Clock: ")) {
                updateLamportClock(responseHeader, lamportClock);
                // Print specific status codes for errors and stop reading further hreaders
            } else if (responseHeader.startsWith("status ")) {
                System.out.println(responseHeader);
                if (responseHeader.equals("status 500") || responseHeader.equals("status 400") || responseHeader.equals("status 204")) {
                    break;
                }
            }
        }
    }

    private static void updateLamportClock(String responseHeader, LamportClock lamportClock) {
        try {
            // Parse Lamportclock value from the header and set it to the local clock
            int lamportClockValue = Integer.parseInt(responseHeader.substring(15).trim());
            lamportClock.setValue(lamportClockValue);
            lamportClock.saveClock("lamportclock.txt");
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private static void andleRetryAttempts(int retryCounter, String errorMessage) {
        // If retry limit is reached then prinnt the error message
        if (retryCounter >= RETRY_COUNT) {
            System.err.printf(errorMessage, RETRY_COUNT);
        }
    }
}