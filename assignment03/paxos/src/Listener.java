import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.*;

/**
 * The Listener class represents a server component that listens for incoming
 * messages from other members in the Paxos system.
 * It processes received messages and delegates them to the associated member.
 */
public class Listener {
    private static final Logger logger = LoggerFactory.getLogger(Election.class);
    private final Gson gson = new Gson(); // Gson instance for JSON parsing
    private final int port;
    private final Member member;
    private ServerSocket serverSocket; // ServerSocket listening for messages

    /**
     * Constructs a Listener for a specific member and port.
     *
     * @param member the Member instance associated with this listener.
     * @param port   the port on which this listener will accept incoming messages.
     */
    public Listener(Member member, int port) {
        this.member = member;
        this.port = port;
    }

    /**
     * Starts the Listener to accept incoming connections on the specified port.
     * Each connection is handled in a separate thread.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleConnection(clientSocket)).start();
            }
        } catch (IOException e) {
            logger.error(member.getMemberId() + " encountered IOException while listening: ", e);
        }
    }

    /**
     * Handles an incoming connection from a client.
     * Reads the incoming message, parses it, and passes it to the associated member
     * for processing.
     *
     * @param clientSocket the socket representing the connection to the client.
     */
    private void handleConnection(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String message = in.readLine();
            Message msg = parseMessage(message);

            member.receive(msg);

        } catch (IOException e) {
            logger.error(member.getMemberId() + " encountered IOException while handling connection: ", e);
        }
    }

    /**
     * Parses a JSON-formatted message string into a Message object.
     *
     * @param message the JSON string representing a Paxos message.
     * @return a Message object constructed from the parsed JSON data.
     */
    private Message parseMessage(String message) {
        JsonObject jsonObject = gson.fromJson(message, JsonObject.class);
        String senderId = jsonObject.get("Sender-ID").getAsString();
        String msgType = jsonObject.get("Message-Type").getAsString();
        int proposalNumber = jsonObject.get("Proposal-Number").getAsInt();
        String candidate = jsonObject.get("Candidate").getAsString();

        return new Message(senderId, msgType, proposalNumber, candidate);
    }
}