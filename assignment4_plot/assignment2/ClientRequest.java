import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.net.Socket;
import org.json.JSONObject;

// Class representing client requests (GET or PUT)
public class ClientRequest implements Comparable<ClientRequest> {
    private int lamport_clock;
    private Socket client_socket;
    private String request_headers;
    private String request_type;
    private String request_data;

    public ClientRequest(int lamport_clock, Socket client_socket, String request_headers, String request_data) {
        this.lamport_clock = lamport_clock;
        this.client_socket = client_socket;
        this.request_headers = request_headers;
        this.request_type = request_headers.split(" ")[0];
        this.request_data = request_data;
    }

    // Getter for Lamport clock
    public int getLamportClock() {
        return lamport_clock;
    }

    // Compare requests by Lamport clock for priority queue
    @Override
    public int compareTo(ClientRequest other) {
        return Integer.compare(this.lamport_clock, other.lamport_clock);
    }

    // Getter for client socket
    public Socket getClientSocket() {
        return client_socket;
    }

    // Method to process either the GET or PUT request
    public void process() {
        if (request_type.equals("GET")) {
            handleGetRequest();
        } else if (request_type.equals("PUT")) {
            handlePutRequest();
        } else {
            System.out.println("Status: 400 Bad Request");
            return;
        }
    }

    // Method to handle GET requests
    private void handleGetRequest() {
        // Extract station id if available
        String station_id = null;
        String[] headers = request_headers.split("\r\n");

        // Iterate over the headers to find the "Station-ID" field
        for (String header : headers) {
            if (header.contains("Station-ID: ")) {
                // Split the header to extract the station id
                station_id = header.split(": ")[1];
                break;
            }
        }

        // Retrieve data from AggregationServer's weather_updates map
        String response_data = AggregationServer.getWeatherData(station_id);

        // Send the retrieved data back to the client
        sendResponse("HTTP/1.1 200 OK\r\nLamport-Clock: " + AggregationServer.getLamportClock() + "\r\nContent-Type: application/json\r\n" + response_data);
    }

    // Method to handle PUT requests
    private void handlePutRequest() {
        // Parse the JSON data from request_data and get station's id
        JSONObject json = new JSONObject(request_data);
        String station_id = json.getString("id");

        // Update the AggregationServer's weather_updates map and check if its first time writing data to file
        boolean is_new_file = AggregationServer.updateWeatherData(station_id, request_data);

        // Send acknowledgement back to client
        if (is_new_file) {
            sendResponse("HTTP/1.1 201 CREATED\r\nLamport-Clock: " + AggregationServer.getLamportClock() + "\r\n\r\nData updated succesfully for station: " + station_id);
        } else {
            sendResponse("HTTP/1.1 200 OK\r\nLamport-Clock: " + AggregationServer.getLamportClock() + "\r\n\r\nData updated successfully for station: " + station_id);
        }
    }

    // Method to send response back to client
    private void sendResponse(String response) {
        try (DataOutputStream output_stream = new DataOutputStream(client_socket.getOutputStream())) {
            output_stream.writeBytes(response);
            output_stream.flush();
        } catch (IOException e) {
            System.err.println("Error sending response to client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
