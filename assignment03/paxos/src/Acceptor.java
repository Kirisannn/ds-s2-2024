import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.sleep;

public class Acceptor implements Runnable {
    private final String memberId;
    private final int delay;
    private final boolean working;

    private AtomicInteger currentMaxProposal = new AtomicInteger(0);
    private AtomicReference<String> currentMaxCandidate = new AtomicReference<>("");
    private AtomicBoolean proposalAccepted = new AtomicBoolean(false);
    private ServerSocket memberListener; // ServerSocket listening for messages
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final Map<String, Integer> acceptorPorts = Map.ofEntries( // All acceptor ports
            Map.entry("M1", 6001),
            Map.entry("M2", 6002),
            Map.entry("M3", 6003),
            Map.entry("M4", 6004),
            Map.entry("M5", 6005),
            Map.entry("M6", 6006),
            Map.entry("M7", 6007),
            Map.entry("M8", 6008),
            Map.entry("M9", 6009));

    private final Gson gson = new Gson(); // Gson instance for JSON parsing
    private static final Logger logger = LoggerFactory.getLogger(Proposer.class); // A logger

    public Acceptor(String memberId, int delay, boolean working) {
        this.memberId = memberId;
        this.delay = delay;
        this.working = working;
    }

    @Override
    public void run() {
        startListening();
    }

    /**
     * Starts listening for messages from proposers
     */
    private void startListening() {
        int port = acceptorPorts.get(memberId);
        try (ServerSocket newListener = new ServerSocket(port)) {
            initialiseSocket(newListener, port); // Initialise the socket

            // While thread not interrupted, listen for messages
            while (!Thread.currentThread().isInterrupted()) {
                listen(); // Accept connections from proposer
            }

        } catch (IOException e) {
            logger.error("IOException creating ServerSocket for Acceptor " + memberId + " on port " + port + ":", e);
        }
    }

    /**
     * Helper method to initialise the socket
     */
    private void initialiseSocket(ServerSocket newListener, int port) {
        this.memberListener = newListener; // Set the memberListener to the newListener
        logger.info("Acceptor " + memberId + " listening on port " + port);
    }

    /**
     * Helper to listen for proposer messages
     */
    private void listen() {
        try {
            Socket proposerSocket = memberListener.accept(); // Accept connection from proposer
            logger.info("Acceptor " + memberId + " accepted connection from proposer");

            // Handle the proposer request in a new thread
            // new Thread(() -> receiveProposerRequest(proposerSocket)).start();
            executor.submit(() -> receiveProposerRequest(proposerSocket));
        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()) {
                logger.error("Acceptor " + memberId + " interrupted, stopping...");
                Thread.currentThread().interrupt();
            }
            logger.error("IOException in acceptor " + memberId + " while accepting connection:\n", e);
        }
    }

    /**
     * Helper to receive proposer request
     */
    private void receiveProposerRequest(Socket proposerSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(proposerSocket.getInputStream()));
                PrintWriter out = new PrintWriter(proposerSocket.getOutputStream(), true)) {
            String request = in.readLine(); // Read the request from the proposer

            if (request != null && !request.isEmpty()) {
                processRequest(request, out); // Process the request
            } else {
                logger.error("Empty request from proposer " + memberId);
            }
        } catch (IOException e) {
            logger.error("IOException in acceptor " + memberId + " while reading proposer request:\n", e);
        }
    }

    /**
     * Method to process the request from the proposer
     */
    private void processRequest(String request, PrintWriter out) {
        JsonObject requestJson = gson.fromJson(request, JsonObject.class); // Parse the request
        String senderId = requestJson.get("Sender-ID").getAsString(); // Get the sender ID
        String msgType = requestJson.get("Message-Type").getAsString(); // Get the message type
        int proposalNumber = requestJson.get("Proposal-Number").getAsInt(); // Get the proposal number
        String candidate = requestJson.get("Candidate").getAsString(); // Get the candidate

        switch (msgType) {
            case "Prepare":
                logger.info("Acceptor " + memberId + " received PREPARE message from proposer " + senderId);
                // If it is M2 and not working, randomly decide to process PREPARE message
                if (memberId.equals("M2") && !working) {
                    // Send prepare message to all acceptors
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        try {
                            sleep(delay);
                        } catch (InterruptedException e) {
                            logger.error("InterruptedException trying to sleep before processing PREPARE:", e);
                        } // Sleep for delay
                        processPrepare(senderId, proposalNumber, candidate, out);
                    }
                } else { // If other members, or M2 working, process PREPARE message
                    try {
                        sleep(delay);
                    } catch (InterruptedException e) {
                        logger.error("InterruptedException trying to sleep before processing PREPARE:", e);
                    } // Sleep for delay
                    processPrepare(senderId, proposalNumber, candidate, out);
                }
                break;
            case "Propose":
                logger.info("Acceptor " + memberId + " received PROPOSE message from proposer " + senderId);
                // If it is M2 and not working, randomly decide to process PROPOSE message
                if (memberId.equals("M2") && !working) {
                    // Send prepare message to all acceptors
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        try {
                            sleep(delay);
                        } catch (InterruptedException e) {
                            logger.error("InterruptedException trying to sleep before processing PROPOSE:", e);
                        } // Sleep for delay
                        processPropose(senderId, proposalNumber, candidate, out);
                    }
                } else { // If other members, or M2 working, process PROPOSE message
                    try {
                        sleep(delay);
                    } catch (InterruptedException e) {
                        logger.error("InterruptedException trying to sleep before processing PROPOSE:", e);
                    } // Sleep for delay
                    processPropose(senderId, proposalNumber, candidate, out);
                }
                break;
            default:
                logger.error("Invalid message type in request from proposer " + senderId);
        }
    }

    /**
     * Handler for Prepare message (Phase 1B: PREPARE-PROMISE)
     */
    private void processPrepare(String senderId, int receivedProposal, String candidate, PrintWriter out) {
        if (receivedProposal > currentMaxProposal.get()) { // If received proposal is greater than current max proposal
            updateMaxProposal(receivedProposal, candidate); // Update the current max proposal and candidate

            promise(senderId, receivedProposal, candidate, out); // Send a promise message to the proposer
        } else {
            logger.info("Acceptor " + memberId + " received PREPARE message with lower proposal number from proposer "
                    + senderId);

            // Check if accepted proposal already accepted
            if (proposalAccepted.get()) {
                // Send the accepted proposal to the proposer
                promise(senderId, currentMaxProposal.get(), currentMaxCandidate.get(), out);
            } else {
                fail(senderId, out); // Send FAIL message to proposer
            }

        }
    }

    /**
     * Helper to update the current max proposal and candidate
     */
    private void updateMaxProposal(int receivedProposal, String candidate) {
        currentMaxProposal.set(receivedProposal); // Set the current max proposal
        currentMaxCandidate.set(candidate); // Set the current max candidate
        proposalAccepted.set(true); // Set the proposal accepted flag
    }

    /**
     * Send PROMISE message to proposer
     */
    private void promise(String senderId, int receivedProposal, String candidate, PrintWriter out) {
        // Create a new Promise message
        Message promise = new Promise(memberId, receivedProposal, candidate);
        out.println(gson.toJson(promise)); // Send the promise message

        logger.info("Acceptor " + memberId + " sent PROMISE message to proposer of " + senderId);
    }

    /**
     * Send FAIL message to proposer
     */
    private void fail(String senderId, PrintWriter out) {
        // Create a new Fail message
        Message fail = new Fail(memberId);
        out.println(gson.toJson(fail)); // Send the fail message

        logger.warn("Acceptor " + memberId + " sent FAIL message to proposer of " + senderId);
    }

    /**
     * Handler for Propose message (Phase 2B: PROPOSE-ACCEPT)
     */
    private void processPropose(String senderId, int receivedProposal, String candidate, PrintWriter out) {
        if (receivedProposal == currentMaxProposal.get()) { // If received proposal is equal to current max proposal
            accept(senderId, receivedProposal, candidate, out); // Accept the proposal
        } else {
            logger.info("Acceptor " + memberId + " received PROPOSE message with lower proposal number from proposer "
                    + senderId);
            fail(senderId, out); // Send FAIL message to proposer
        }
    }

    /**
     * Send ACCEPT message to proposer
     */
    private void accept(String senderId, int receivedProposal, String candidate, PrintWriter out) {
        proposalAccepted.set(true); // Set the proposal accepted flag
        currentMaxCandidate.set(candidate); // Set the current max candidate
        Message accept = new Accept(memberId, receivedProposal, candidate); // Create a new Accept message
        out.println(gson.toJson(accept)); // Send the accept message
    }

    /**
     * Stop the acceptor
     */
    public void stop() {
        logger.info("Stopping acceptor " + memberId + "...");

        try {
            memberListener.close(); // Close the memberListener
        } catch (IOException e) {
            logger.error("IOException in acceptor " + memberId + " while closing memberListener:\n", e);
        }

        executor.shutdown(); // Shutdown the thread pool
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate in the specified time. Forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("InterruptedException during executor shutdown. Forcing shutdown.");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        Thread.currentThread().interrupt(); // Interrupt the thread

        logger.info("Acceptor " + memberId + " stopped");
    }

}
