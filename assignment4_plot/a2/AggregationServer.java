import java.io.*;
import java.net.*;
import org.json.*;
import java.util.*;
import org.json.*;
import java.util.concurrent.*;

public class AggregationServer {
    // File that store JSON data
    private static final String WeatherData = "WeatherData.json";
    // Number of threads for the thread pool
    private static final int Threads = 10;
    // Queue to store incoming PUT request
    public static Queue<PutRequest> requestQueue = new ConcurrentLinkedQueue<>();
    // Store the server's condition state
    private static final String BackupServerData = "server.json";

    

    public static void main(String[] args) {
        // Check whether the port number is provided as argunment
        if (args.length < 1) {
            System.out.println("java AggregationServer <port>");
            return;
        }

        // Initialise the Lamport Clock to synchronizing time events between the servers
        LamportClock lamportClock = new LamportClock();

        // Loads the data in the file and set the lamportclock value to the data
        lamportClock.loadClock("lamportclock.txt");
        lamportClock.setValue(lamportClock.getTime());

        // Loads any previous state form file to continue the operations after restarts
        loadState();

        // Parse port number from arguments
        int portNumber = Integer.parseInt(args[0]);

        // Creates a fixed thread pool of 5 threads
        ExecutorService executor = Executors.newFixedThreadPool(Threads); 

        // Start multiple server instances on consecutive ports
        for (int i = 0; i < Threads; i++) {
            // Run servers on separate ports via thread pool
            int currentPort = portNumber + i;
            executor.execute(() -> startServer(currentPort));
        }

        // Shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("...Shutting down server...");
            // Orderly shutdown, allow tasks to complete
            executor.shutdown();
            try {
                // Waits 10 seconds, force shutdown and terminates remaining tasks if unsucessful
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.out.println("...Forcing shutdown...");
                    executor.shutdownNow();
                }
                // Ensure state saved on shutdown
                saveState();  
            } catch (InterruptedException e) {
                logError("Interruption on the executor termination", e);
            }
        }));
        
    }

    private static void logError(String message, Exception e) {
        // Display error message 
        System.err.println(message); 
        if (e != null) {  
            e.printStackTrace();
        }
    }

    private static void startServer(int port) { 
        // Initiate a new thread pool with the thread number for the hadling client requests
        ExecutorService executor = Executors.newFixedThreadPool(Threads);

        // Background thread that runs continuously and periperiodically remove old data save state every 30 secconds.
        Thread bgThread = new Thread(() -> { 
            while (true) {
                RemoveOutdatedData();
                saveState();
                try {
                    // Waits 10 seconds before next cleanup cycle
                    Thread.sleep(30000); 
                } catch (InterruptedException e) {
                    // Restore interrupt status
                    Thread.currentThread().interrupt();
                    System.err.println("Data cleanup thread has been disturbed.");
                }
            }
        });

        // Make it a daemon thread for automatic cleanup on exit
        bgThread.setDaemon(true); 
        // Start the background cleanup thread
        bgThread.start(); 

        // Listens for connection on specified port
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started successfully on port " + port);

            while (true) {
                // Accepts incoming connections 
                Socket Sockets = serverSocket.accept();
                // Handle each requiret in a separate thread
                executor.submit(new HandlingRequests(Sockets));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private static class HandlingRequests implements Runnable {
        // Handle requests from Content Server and GET Client
        private final Socket Sockets;
        private static final LamportClock lamportClock = new LamportClock();

        
        public HandlingRequests(Socket socket) {
            this.Sockets = socket;
        }

        public void run() {
            
            try (
                // Read incoming data fron the client socket input stream
                BufferedReader reader = new BufferedReader(new InputStreamReader(Sockets.getInputStream())); 
                // Sends responses back to the client through socket's output stream
                PrintWriter writer = new PrintWriter(Sockets.getOutputStream(), true)
            ) {
                // Read request from client
                String request = reader.readLine();
                // Check whther request is not null
                if (request != null) {
                    // Handle PUT request
                    if (request.startsWith("PUT")) {
                        HandlingPUTRequests(reader, writer);
                    // Handle GET request
                    } else if (request.startsWith("GET")) {
                        handlingGETRequest(writer);
                    // Handle unknown request types
                    } else {
                        writer.println("status 400 - Neither PUT or GET"); 
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Ensure socket is closed
                try {
                    Sockets.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        /*Handles PUT request, if an input file with normal weather data is inputted it will store the data into the storage.json,
        it will return a response code 201 for the first time it is stored, subsequent data will be status 200, 
        if the file sent is empty,
        a response of status 204 will be returned and incorrect json data will return status 500*/
        private static void HandlingPUTRequests(BufferedReader reader, PrintWriter writer) throws IOException {
            Map<String, String> headers = new HashMap<>();
            String header;
            int lamportClockValue = 0;
            // Read existing JSON array from the file
            JSONArray existingJsonArray = readsExistingJsonArray(WeatherData);
            // Check if its the first upload
            boolean isFirstUpload = existingJsonArray == null || existingJsonArray.length() == 0;

            // Process headers
            HeaderProcesses(reader, headers);

            // Extract Lamport-Clock value from headers
            if (headers.containsKey("Lamport-Clock")) {
                try {
                    lamportClockValue = Integer.parseInt(headers.get("Lamport-Clock").trim());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    writer.println("status 500 - Internal server error");
                    return;
                }
            }

            // Process body
            if (headers.containsKey("Content-Type") && headers.get("Content-Type").equals("application/json")) {
                StringBuilder jsonDataString = new StringBuilder();
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                char[] buffer = new char[contentLength];
                int bytesRead = reader.read(buffer, 0, contentLength);
                // Read JSON data from the request body
                if (bytesRead == contentLength) {
                    jsonDataString.append(buffer, 0, contentLength);
                    // Submit Json data for handling and response based on Lamport Clock value
                    handlingJsonData(writer, jsonDataString.toString(), lamportClockValue, isFirstUpload);
                }
            }
        }

        private static void HeaderProcesses(BufferedReader reader, Map<String, String> headers) throws IOException {
            String header;
            // Read each line from the reader until an empty line is encountered
            while ((header = reader.readLine()) != null && !header.isEmpty()) {
                // Split header line into key and value parts
                String[] parts = header.split(": ", 2);
                // If header line contains both key and value, add them to the headers map
                if (parts.length == 2) {
                    headers.put(parts[0], parts[1]);
                }
            }
        }

        private static void handlingJsonData(PrintWriter writer, String jsonDataString, int lamportClockValue, boolean isFirstUpload) {
            try {
                // Parse JSON data string into a JSONArray
                JSONArray jsonArray = new JSONArray(jsonDataString);
                // Check if JSON array is empty
                if (jsonArray.length() == 0) {
                    writer.println("status 204 : No Content");
                    updateLamportClock(lamportClockValue + 1);
                    return;
                }
                // Validate JSON data, must contain required fields like 'id'
                if (!HaveID(jsonArray) || !correctDataKey(jsonArray)) {
                    writer.println("status 500 - Internal server error (No ID)");
                    updateLamportClock(lamportClockValue + 1);
                    return;
                }
                // Respond whether it's the first upload
                if (isFirstUpload) {
                    writer.println("status 201 - Weather data entry CREATED");
                } else {
                    writer.println("status 200 OK - Updates Successfully");
                }
                // Update JSON array in the file and Lamport clock
                UpdateJSON(jsonArray, WeatherData);
                updateLamportClock(lamportClockValue + 1);
                // Create new PutRequest and add it to request queue
                PutRequest newRequest = new PutRequest(lamportClockValue, jsonArray);
                requestQueue.add(newRequest);
                // Process queued PUT requests
                QueuedPUTRequests(writer);
                // Save the server condition
                saveState();
            } catch (JSONException e) {
                writer.println("status 500 - Internal server error");
            }
        }
        // Update the Lamport clock value
        private static void updateLamportClock(int newLCValue) {
            lamportClock.setValue(newLCValue);
            lamportClock.saveClock("lamportclock.txt");
        }

        private static boolean  HaveID(JSONArray jsonArray) {
            // Check if the JSON array contains the "id" key
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (!jsonObject.has("id") || jsonObject.isNull("id") || jsonObject.optString("id").trim().isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        
        // Check if the JSON array contains all required keys
        private static boolean correctDataKey(JSONArray jsonArray) {
            // List of required fields for JSON validation
            List<String> requiredFields = Arrays.asList(
                "id", "name", "state", "time_zone", "lat", "lon", "local_date_time",
                "local_date_time_full", "air_temp", "apparent_t", "cloud", "dewpt",
                "press", "rel_hum", "wind_dir", "wind_spd_kmh", "wind_spd_kt"
            );
            // Iterate through each JSON object in the array
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                for (String field : requiredFields) {
                    if (!jsonObject.has(field)) {
                        logError("Non-existing required field: " + field, new Exception());
                        return false;
                    }
                }
                // Check if all required fields are present
                for (String key : jsonObject.keySet()) {
                    if (!requiredFields.contains(key)) {
                        logError("Unexpected field: " + key + jsonObject.toString(), null);
                        return false;
                    }
                }
            }
            return true;
        }
        
        // Retrieve & send existing weather data from storage to the client
        private static void handlingGETRequest(PrintWriter writer) throws IOException {
            JSONArray jsonArray = readsExistingJsonArray(WeatherData);
            if (jsonArray != null) {
                // Send the JSON array as a response
                sendResponse(writer, "HTTP/1.1 200 OK", "application/json", jsonArray.toString(4));
            }
        }

        // Send a formatted HTTP response to client
        private static void sendResponse(PrintWriter writer, String status, String contentType, String content) {
            writer.println(status);
            writer.println("Content-Type: " + contentType);
            writer.println("Content-Length: " + content.length());
            writer.println();
            writer.println(content);
        }
    }

    // Process queued PUT requests
    private static void QueuedPUTRequests(PrintWriter writer) {
        // Loop until there are no more requests he queue
        while (!requestQueue.isEmpty()) {
            // Retrieve and remove the head of the queue
            PutRequest request = requestQueue.poll();
            int requestLCValue = request.getLamportClockValue();
            JSONArray requestJsonArray = request.getJsonArray();
            LamportClock lamportClock = new LamportClock();
            lamportClock.loadClock("lamportclock.txt");

            // Check if the request's Lamport clock value matches the current clock value
            if (requestLCValue == lamportClock.getTime()) {
                UpdateJSON(requestJsonArray, WeatherData);
                lamportClock.tick();
                lamportClock.saveClock("lamportclock.txt");
            } else {
                requestQueue.add(request);
                break;
            }
        }
    }

    // Update the JSON array in the specified file
    private static void UpdateJSON(JSONArray jsonArray, String filename) {
        JSONArray existingJsonArray = readsExistingJsonArray(filename);
        if (existingJsonArray == null) {
            existingJsonArray = new JSONArray();
        }
        // Get the current system time
        long currentTime = System.currentTimeMillis();
        // Keep track of updated IDs
        Set<String> updatedIds = new HashSet<>();

        // Updates / add entries based on the ID
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String id = jsonObject.optString("id");
            if (id != null) {
                boolean updated = false;
                // Find matching IDs
                for (int j = 0; j < existingJsonArray.length(); j++) {
                    JSONObject existingObject = existingJsonArray.getJSONObject(j);
                    String existingId = existingObject.optString("id");
                    // Update timestamp of existing entru & mark updated
                    if (existingId != null && existingId.equals(id)) {
                        existingObject.put("timestamp", currentTime);
                        updatedIds.add(id);
                        updated = true;
                        break;
                    }
                }
                // No matching ID, add new entry with timestamp
                if (!updated) {
                    jsonObject.put("timestamp", currentTime);
                    existingJsonArray.put(jsonObject);
                }
            }
        }

        // Add new entries that weren't update
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String id = jsonObject.optString("id");
            // Add entries with valid ID that havent updated
            if (id != null && !updatedIds.contains(id)) {
                jsonObject.put("timestamp", currentTime);
                existingJsonArray.put(jsonObject);
            }
        }
        // Write updated Json array to the file
        writeJsonArray(filename, existingJsonArray);
    }

    // Delete outdated storage based on timestamp threshold
    private static void RemoveOutdatedData() {
        JSONArray jsonArray = readsExistingJsonArray(WeatherData);
        if (jsonArray != null) {
            // Get the current system time
            long currentTime = System.currentTimeMillis();
            JSONArray updatedArray = new JSONArray();
            // Iterate each entry in the loaded Json array
            jsonArray.forEach(item -> {
                JSONObject jsonObject = (JSONObject) item;
                long timestamp = jsonObject.optLong("timestamp", -1);
                // Keep entries that are either missing a timestamp or are not older than 10 seconds
                if (timestamp == -1 || currentTime - timestamp <= 10000) {
                    updatedArray.put(jsonObject);
                }
            });
            // Write filtered array back to the file
            writeJsonArray(WeatherData, updatedArray);
        }
    }

    // Reads existing JSON array from storage.json --> returns it
    private static JSONArray readsExistingJsonArray(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            // Store file contents as string
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            // Read line by line then append to string builder
            while ((line = br.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            // Convert builder to string and trim the whitespaces
            String jsonString = jsonStringBuilder.toString().trim();
            // Empty then return a new Json array
            if (jsonString.isEmpty()) {
                return new JSONArray();
            }
            // Convert string back to Json array
            return new JSONArray(jsonString);
        } catch (IOException | JSONException e) {
            logError("Fail to read JSON array from file " + fileName, e);
        }
        return null;
    }

    // Writes a JSON array to storage.json
    public static void writeJsonArray(String fileName, JSONArray jsonArray) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            // Convert Json array to string with indentation for readability 
            bw.write(jsonArray.toString(4));
            // Ensure all data is written to the file
            bw.flush();
        } catch (IOException e) {
            logError("Fail to write JSON array to file " + fileName, e);
        }
    }


    // Saves server condition
    private static void saveState() {
        JSONObject state = new JSONObject();
        // Add current request queue to the state
        state.put("requestQueue", requestQueue);
        // Write the state JSON object to the backup file
        try (FileWriter file = new FileWriter(BackupServerData)) {
            file.write(state.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Loads the server condition
    private static void loadState() {
        try (FileReader reader = new FileReader(BackupServerData)) {
            // Use a JSONTokener to parse the file contents
            JSONTokener tokener = new JSONTokener(reader);
            // Convert tokener content to a JSON object
            JSONObject state = new JSONObject(tokener);
            // Get the request queue from the saved state & clear any existing entries
            JSONArray queueArray = state.getJSONArray("requestQueue");
            requestQueue.clear();

            // Convert each JSON object in the saved queue array to a PutRequest and add it to the current queue
            for (int i = 0; i < queueArray.length(); i++) {
                JSONObject putRequest = queueArray.getJSONObject(i);
                requestQueue.add(new PutRequest(putRequest));
            }
        } catch (IOException | JSONException e) {
            requestQueue = new LinkedList<>();
        }
    }
}
