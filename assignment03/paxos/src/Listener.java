import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.*;

public class Listener {
    private static final Logger logger = LoggerFactory.getLogger(Election.class);
    private final Gson gson = new Gson(); // Gson instance for JSON parsing
    private final int port;
    private final Member member;
    private ServerSocket serverSocket; // ServerSocket listening for messages

    public Listener(Member member, int port) {
        this.member = member;
        this.port = port;
    }

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

    private void handleConnection(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String message = in.readLine();
            Message msg = parseMessage(message);

            member.receive(msg);

        } catch (IOException e) {
            logger.error(member.getMemberId() + " encountered IOException while handling connection: ", e);
        }
    }

    private Message parseMessage(String message) {
        JsonObject jsonObject = gson.fromJson(message, JsonObject.class);
        String senderId = jsonObject.get("Sender-ID").getAsString();
        String msgType = jsonObject.get("Message-Type").getAsString();
        int proposalNumber = jsonObject.get("Proposal-Number").getAsInt();
        String candidate = jsonObject.get("Candidate").getAsString();

        return new Message(senderId, msgType, proposalNumber, candidate);
    }
}