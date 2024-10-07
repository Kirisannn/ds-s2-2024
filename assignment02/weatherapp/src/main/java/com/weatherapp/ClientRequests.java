package com.weatherapp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClientRequests implements Comparable<ClientRequests> {
    private final Socket clientSocket; // Socket for the client
    private final String requestType; // GET or POST
    private final String requestHeaders;
    private final String requestContent; // JSON formatted string
    private final int requestClock; // Lamport clock for timestamping

    ClientRequests(Socket socket, String type, String headers, String content, int timestamp) {
        clientSocket = socket;
        requestType = type;
        requestHeaders = headers;
        requestContent = content;
        requestClock = timestamp;
    }

    @Override
    public int compareTo(ClientRequests other) {
        return Integer.compare(this.requestClock, other.requestClock); // Ascending order based on Lamport time
    }

    public void process() throws IOException {
        // Process the request based on the request type
        switch (requestType) {
            case "GET" -> processGetRequest();
            case "PUT" -> processPUTRequest();
            default -> {
                try {
                    sendResponse(400, "Invalid request type. Please use PUT or GET.");
                } catch (IOException e) {
                    System.err.println("Error sending response: " + e.getMessage());
                    // e.printStackTrace();
                }
            }
        }
    }

    private void processGetRequest() {
        // Process GET request
    }

    private void processPUTRequest() throws IOException {
        // Try to parse the JSON content
        JsonElement jsonElement = JsonParser.parseString(requestContent);

        // If JSON content is valid, process the POST request
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String stationID = jsonObject.get("id").getAsString();
        // System.out.println("Station ID: " + stationID);

        boolean isNew = AggregationServer.updateData(stationID, jsonObject);
        // System.out.println("Data updated: " + isNew);
        // Send response to the client
        if (isNew) {
            // System.out.println("Sending response 201");
            sendResponse(201, "Successfully updated data for station " + stationID);
        } else {
            // System.out.println("Sending response 200");
            sendResponse(200, "Successfully updated data for station " + stationID);
        }
    }

    // Send response to the client

    // Getters
    public Socket getClientSocket() {
        return clientSocket;
    }

    public String getRequestType() {
        return requestType;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public String getRequestContent() {
        return requestContent;
    }

    public int getRequestClock() {
        return requestClock;
    }

    // Send response to the client
    public void sendResponse(int statusCode, String message) throws IOException {
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        String httpResponse = "HTTP/1.1 " + statusCode + "\nLamport-Time: " + AggregationServer.getClock()
                + "\n\n" + message + "\r\n\r\n";
        out.writeUTF(httpResponse);
        out.flush();
    }
}
