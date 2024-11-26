import java.io.*;
import java.net.*;

public class PaxosServer {
    private final int port;
    private final PaxosNode node;

    // Constructor for a new PaxosServer
    // Inputs: The PaxosNode and its identifier
    // Output: The PaxosServer class object
    public PaxosServer(PaxosNode node, int node_id) {
        this.port = 8000 + node_id;
        this.node = node;
    }

    // Method to start the server
    // Input: None
    // Output: None
    public void start() {
        try {
            ServerSocket server_socket = new ServerSocket(port);
            // System.out.println("PaxosServer for member node: M" + (port-8000) + " is running on port " + port);

            // Continuously listen for incoming messages
            while (true) {
                Socket client_socket = server_socket.accept();
                new Thread(() -> clientHandler(client_socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error while starting server or listening for incoming messages: " + e.getMessage());
        }
    }

    // Method to handle incoming client connections
    // Input: The client's socket
    // Output: None
    private void clientHandler(Socket client_socket) {
        try {
            ObjectInputStream input_stream = new ObjectInputStream(client_socket.getInputStream());
            PaxosMessage message = (PaxosMessage) input_stream.readObject();

            // Pass message on to PaxosNode
            node.receiveMessage(message);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error while handling client connection: " + e.getMessage());
        }
    }
}