import java.io.*;
import java.net.*;

public class PaxosClient {
    private final int port;

    // Constructor for a new PaxosClient
    // Input: The identifer of PaxosNode that will receive the sent message
    // Output: The PaxosClient class object
    public PaxosClient(int recipient_node_id) {
        this.port = 8000 + recipient_node_id;
    }

    // Method to send a Paxos message to the recipient node
    // Input: The PaxosMessage
    // Output: None`
    public void sendMessage(PaxosMessage message) {
        try {
            Socket socket = new Socket("localhost", port);
            ObjectOutputStream output_stream = new ObjectOutputStream(socket.getOutputStream());

            // Send the serialised message
            output_stream.writeObject(message);

        } catch (IOException e) {
            System.err.println("Failed to send message to member node: M" + (port-8000) + " - " + e.getMessage());
        }
    }
}