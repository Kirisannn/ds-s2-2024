import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import org.json.JSONObject;
import java.util.concurrent.locks.ReentrantLock;
import java.util.PriorityQueue;

public class AggregationServer{
    private static int lamport_clock = 0; // Server's Lamport clock
    private static ConcurrentHashMap<String, Long> last_communication = new ConcurrentHashMap<>(); // To store time of last communication from each content server
    private static ConcurrentHashMap<String, String> weather_updates = new ConcurrentHashMap<>(); // List to store all the weather updates
    private static ReentrantLock lock = new ReentrantLock(); // Lock to enforce concurrency and prevent race conditions
    private static PriorityQueue<ClientRequest> request_queue = new PriorityQueue<>(); // To store requests ordered by their Lamport clock

    // Main method
    // Input: String arguments from command line
    // Output: None
    public static void main(String[] args) {
        // Initialize default port (4567) or port from user input
        int port = 4567; 
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, server will use default port 4567.");
            }
        }

        // Restore any existing weather data
        restoreWeatherData();

        // Start thread to periodically remove old content
        new Thread(AggregationServer::removeOldData).start();

        // Start thread to process requests in the queue
        new Thread(AggregationServer::processRequests).start();

        // Create server socket
        try (ServerSocket server_socket = new ServerSocket(port)) {
            System.out.println("Aggregation Server started on port: " + port);

            // Continuously listen for client or content server requests
            while (true) {
                Socket client_socket = server_socket.accept();
                System.out.println("Connected to client: " + client_socket.getRemoteSocketAddress());

                // Handle each client request in a separate thread to maintain concurrency
                Thread client_thread = new Thread(() -> clientHandler(client_socket));
                client_thread.start();
            }

        } catch (IOException e) {
            System.err.println("Could not connect on port: " + port);
            e.printStackTrace();
        }
    }

    // Method to get the current value of Lamport clock in the server
    public static synchronized int getLamportClock() {
        return lamport_clock;
    }

    // Method to restore weather data from the main file if available upon server startup
    private static void restoreWeatherData() {
        lock.lock();  // Acquire the lock to prevent race conditions during restoration process
        try {
            if (Files.exists(Paths.get("WeatherData.txt"))) {
                List<String> lines = Files.readAllLines(Paths.get("WeatherData.txt"));
                for (String line : lines) {
                    line = line.trim();
                    System.out.println("Reading line: " + line);

                    if (!line.isEmpty()) {
                        JSONObject json = new JSONObject(line);
                        String station_id = json.getString("id");
                        weather_updates.put(station_id, line);
                        last_communication.put(station_id, System.currentTimeMillis());
                    }
                }
            } else {
                System.out.println("No existing weather data.");
            }
        } catch (IOException e) {
            System.err.println("Error restoring weather data: " + e.getMessage());
        } finally {
            lock.unlock();  // Release the lock
        }
    }

    // Method to periodically remove content from content servers that haven't communicated in last 30 seconds
    private static void removeOldData() {
        while (true) {
            long current_time = System.currentTimeMillis();
            last_communication.entrySet().removeIf(entry -> {
                boolean expired = (current_time - entry.getValue()) > 30000;
                if (expired) {
                    System.out.println("Removing expired data for station: " + entry.getKey());
                    weather_updates.remove(entry.getKey());
                    writeWeatherData(); // Remove from main weather data file
                }
                return expired;
            });

            try {
                Thread.sleep(5000); // Check every 5 seconds
            } catch (InterruptedException e) {
                System.out.println("Thread Interrupted: Process of removing old data stopped.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Method to manage individual client connections and add them to the queue
    public static void clientHandler(Socket client_socket) {
        try {
            DataInputStream input_stream = new DataInputStream(client_socket.getInputStream());
            DataOutputStream output_stream = new DataOutputStream(client_socket.getOutputStream());

            client_socket.setSoTimeout(10000); // Timeout after 10 seconds to prevent deadlocks
            
            // Read the request headers
            StringBuilder request_headers = new StringBuilder();
            String line;
            int client_clock = -1;
            
            while (!(line = input_stream.readUTF()).isEmpty()) {
                request_headers.append(line).append("\n");
                if (line.startsWith("Lamport-Clock: ")) {
                    client_clock = Integer.parseInt(line.split(": ")[1].trim());
                    break;
                }
            }

            // Read the JSON data after headers
            StringBuilder request_data = new StringBuilder();
            while (input_stream.available() > 0) {
                request_data.append(input_stream.readUTF()).append("\n");
            }

            // If it's a PUT request
            if (request_headers.toString().contains("PUT")) {
                // Check if no content was sent
                if (request_data.toString().trim().isEmpty()) {
                    output_stream.writeUTF("HTTP /1.1 204 No Content\r\n"); // Send error 204
                    output_stream.flush();
                    return;
                }

                // Try parsing the JSON data
                JSONObject json = null;
                try {
                    json = new JSONObject(request_data.toString().trim());
                } catch (Exception e) {
                    output_stream.writeUTF("HTTP/1.1 500 Internal Server Error\r\n"); // Send error 500
                    output_stream.flush();
                    System.err.println("Malformed JSON: " + e.getMessage());
                    return;
                }
            }

            // Synchronize Lamport clocks
            synchronized (AggregationServer.class) {
                lamport_clock = Math.max(lamport_clock, client_clock) + 1;
            }

            // System.out.println("Request Headers: " + request_headers);
            // System.out.println("Lamport Clock: " + lamport_clock);
            // System.out.println("JSON Data: " + request_data);

            // Add request to queue
            ClientRequest client_request = new ClientRequest(lamport_clock, client_socket, request_headers.toString(), request_data.toString());
            synchronized (request_queue) {
                request_queue.add(client_request);
                System.out.println("Client Request: " + client_request + " has been added to queue with Lamport clock: " + lamport_clock);
            }

        } catch (IOException e) {
            System.err.println("Error getting input and output stream: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to process requests from the queue
    public static void processRequests() {
        while (true) {
            ClientRequest request = null;

            lock.lock();
            try {
                request = request_queue.poll(); // Poll a request from the queue
            } finally {
                lock.unlock();
            }

            if (request != null) {
                request.process();
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    System.out.println("Request processing interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }
    }

    // Method to get weather data from every station or a specific station
    // Input: String of station's id
    // Output: String of weather updates in JSON format
    public static String getWeatherData(String station_id) {
        // Return specific data if station_id is provided
        if (station_id != null && weather_updates.containsKey(station_id)) {
            return weather_updates.get(station_id);
        }

        // Else return the most recent update from content server that communicated last
        String latest_station = null;
        long latest_time = Long.MIN_VALUE;

        // Find the station with the most recent communication
        for (Map.Entry<String, Long> entry : last_communication.entrySet()) {
            if (entry.getValue() > latest_time) {
                latest_time = entry.getValue();
                latest_station = entry.getKey();
            }
        }

        // Return the weather update of the most recently communicated content server
        if (latest_station != null) {
            return weather_updates.get(latest_station);
        } else {
            return weather_updates.toString(); // Return null
        }
    }

    // Method to update weather data and the last communication time of content server
    // Inputs: String of station's id, and string of weather data to be updated
    // Output: Boolean to check if it's the server's first time creating main weather data file
    public static Boolean updateWeatherData(String station_id, String data) {
        // If main weather data file hasn't been created, create a temp file and write to it
        boolean is_new_file = !Files.exists(Paths.get("WeatherData.txt"));

        lock.lock();
        try {
            weather_updates.put(station_id, data);
            last_communication.put(station_id, System.currentTimeMillis());

            // If there are more than 20 stations' data stored, remove the oldest one
            if (weather_updates.size() > 20) {
                // Find the station with the oldest communication time
                String oldest_station = null;
                long oldest_time = Long.MAX_VALUE;

                for (Map.Entry<String, Long> entry : last_communication.entrySet()) {
                    if (entry.getValue() < oldest_time) {
                        oldest_time = entry.getValue();
                        oldest_station = entry.getKey();
                    }
                }

                // Remove the oldest station's data
                if (oldest_station != null) {
                    weather_updates.remove(oldest_station);
                    last_communication.remove(oldest_station);
                    System.out.println("Removed station with ID: " + oldest_station + " due to exceeding capacity of 20 stations.");
                }
            }

            writeWeatherData();
            
        } catch (Exception e) {
            System.err.println("Error updating weather data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        
        return is_new_file;
    }

    // Method to write weather data to main file
    public static void writeWeatherData() {
        // Create a temporary file to write the weather data
        Path tempFilePath = Paths.get("TempData.tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tempFilePath)) {
            // Write each weather update to the temp file in JSON format
            for (String update : weather_updates.values()) {
                writer.write(update);
                writer.newLine();  // Add a newline after each JSON object
            }

            // Move the temp file to the main file after writing is complete
            Files.move(tempFilePath, Paths.get("WeatherData.txt"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error updating weather data file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}