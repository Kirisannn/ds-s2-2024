import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import static java.lang.Thread.sleep;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proposer class implements the Paxos proposer functionality.
 * It initiates proposals, sends "prepare" messages to acceptors, collects
 * "promise" messages,
 * and sends "propose" messages to finalize the proposal.
 */
public class Proposer implements Runnable {
    private static final AtomicBoolean elected = new AtomicBoolean(false); // Flag to indicate if president is elected
    private static AtomicInteger proposalCount = new AtomicInteger(0); // Count of total proposals
    private AtomicInteger promisedCount = new AtomicInteger(0); // Count of promised proposals
    private AtomicInteger acceptedCount = new AtomicInteger(0); // Count of accepted proposals

    private final String memberId; // Member ID
    private final int delay; // Delay in ms
    private final boolean working; // If true, its M2, and randomly decide to respond to responses or not

    private int currentProposalNumber = 0; // Current proposal number
    private String currentProposalCandidate = ""; // Current proposal candidate

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

    public Proposer(String memberId, int delay, boolean working) {
        this.memberId = memberId;
        this.delay = delay;
        this.working = working;
    }

    @Override
    public void run() {
        // While consensus has yet to be reached, keep proposing
        while (!elected.get()) {
            try {
                promisedCount.set(0); // Reset promised count
                acceptedCount.set(0); // Reset accepted count
                currentProposalNumber = getProposalNumber(); // Get proposal number
                currentProposalCandidate = getCandidate(); // Get candidate

                // If it is M2 and not working, randomly decide to send prepare message
                if (memberId.equals("M2") && !working) {
                    // Send prepare message to all acceptors
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        sleep(delay); // Sleep for delay
                        prepareAndPropose();
                    }
                } else { // If other members, or M2 working, send prepare message
                    sleep(delay); // Sleep for delay
                    prepareAndPropose();
                }
            } catch (InterruptedException e) {
                logger.error("InterruptedException in proposer thread:", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Exception in proposer thread:", e);
            }
        }
    }

    /**
     * Get proposal number.
     */
    public int getProposalNumber() {
        return proposalCount.incrementAndGet();
    }

    /**
     * Decide the candidate for the proposal.
     */
    private String getCandidate() {
        // If M1, M2, or M3, return the member ID, else return a random candidate
        if (memberId.equals("M1") || memberId.equals("M2") || memberId.equals("M3")) {
            return memberId;
        }
        return "M" + Integer.toString(ThreadLocalRandom.current().nextInt(1, 10));
    }

    /**
     * Send prepare message to all acceptors.
     */
    private void prepareAndPropose() {
        logger.info(
                memberId + " proposer sending PREPARE messages for proposal " + currentProposalNumber + ", candidate "
                        + currentProposalCandidate);

        for (Map.Entry<String, Integer> acceptorEntry : acceptorPorts.entrySet()) {
            if (elected.get()) { // If already elected, break
                break;
            }

            String acceptorId = acceptorEntry.getKey();
            int acceptorPort = acceptorEntry.getValue();
            new Thread(() -> sendPrepare(acceptorId, acceptorPort)).start();

        }
    }

    private void sendPrepare(String acceptorId, int port) {
        try (Socket socket = new Socket("localhost", port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String prepareMessage = gson.toJson(new Prepare(memberId, currentProposalNumber));
            out.println(prepareMessage);

            logger.info("Proposer " + memberId + " sent PREPARE message to acceptor " + acceptorId);

            String response = in.readLine();
            if (response != null) {
                handleResponse(response, "PREPARE", acceptorId);
            }
        } catch (IOException e) {
            logger.error("IOException: " + memberId + " proposer failed to send PREPARE message to " + acceptorId + ":",
                    e);
        }
    }

    /**
     * Send propose message to all acceptors.
     */
    private void propose() {
        logger.info(
                memberId + " proposer sending PROPOSE messages for proposal " + currentProposalNumber + ", candidate "
                        + currentProposalCandidate);

        for (Map.Entry<String, Integer> acceptorEntry : acceptorPorts.entrySet()) {
            if (elected.get()) { // If already elected, break
                break;
            }

            String acceptorId = acceptorEntry.getKey();
            int acceptorPort = acceptorEntry.getValue();
            new Thread(() -> sendPropose(acceptorId, acceptorPort)).start();
        }
    }

    /**
     * Helper method to send propose message to individual acceptors.
     */
    private void sendPropose(String acceptorId, int acceptorPort) {
        try (Socket socket = new Socket("localhost", acceptorPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String proposeMessage = gson.toJson(new Propose(memberId, currentProposalNumber, currentProposalCandidate));
            out.println(proposeMessage);

            logger.info("Proposer " + memberId + " sent PROPOSE message to acceptor " + acceptorId);

            String response = in.readLine();
            if (response != null) {
                handleResponse(response, "PROPOSE", acceptorId);
            }
        } catch (IOException e) {
            logger.error("IOException: " + memberId + " proposer failed to send PROPOSE message to " + acceptorId + ":",
                    e);
        }
    }

    /**
     * Handle response from acceptors. PROMISE, ACCEPTED, and FAIL are handled.
     */
    private void handleResponse(String response, String phase, String acceptorId) {
        JsonObject responseJson = gson.fromJson(response, JsonObject.class);
        String senderId = responseJson.get("Sender-ID").getAsString();
        String responseType = responseJson.get("Message-Type").getAsString();
        int proposalNumber = responseJson.get("Proposal-Number").getAsInt();
        String candidate = responseJson.get("Candidate").getAsString();

        if (!acceptorId.equals(senderId)) {
            logger.error(memberId + " proposer received message from unknown acceptor " + acceptorId);
            return;
        }

        switch (responseType) {
            case "Promise":
                promisedCount.incrementAndGet(); // Increment promised count
                logger.info(
                        memberId + " received PROMISE from " + acceptorId + ". Total promises: " + promisedCount.get());
                // logger.info(
                // memberId + " proposer received PROMISE message from acceptor " + acceptorId
                // + " for proposal "
                // + proposalNumber + ", candidate " + candidate);

                // If candidate is not empty, update current proposal candidate. If proposal
                // number is higher, update current proposal number too
                if ((candidate != null && !candidate.isEmpty()) && proposalNumber == currentProposalNumber) {
                    if (promisedCount.get() > 4) { // If majority promises received, propose
                        // If M2 is not working, randomly decide if connected to network
                        if (memberId.equals("M2") && !working) {
                            if (ThreadLocalRandom.current().nextBoolean()) {
                                try {
                                    sleep(delay);
                                } catch (InterruptedException e) {
                                    logger.error("InterruptedException trying to sleep before PROPOSE:", e);
                                } // Sleep for delay
                                propose();
                            }
                        } else {
                            try {
                                sleep(delay);
                            } catch (InterruptedException e) {
                                logger.error("InterruptedException trying to sleep before PROPOSE:", e);
                            } // Sleep for delay
                            propose();
                        }
                    }
                }
                break;
            case "Accepted":
                acceptedCount.incrementAndGet(); // Increment accepted count
                logger.info(
                        memberId + " received ACCEPTED from " + acceptorId + ". Total accepts: " + acceptedCount.get());
                // logger.info(
                // memberId + " proposer received ACCEPTED message from acceptor " + acceptorId
                // + " for proposal " + proposalNumber + ", candidate " + candidate);

                // If majority accepted, set president
                if (acceptedCount.get() > 4) {
                    elected.set(true); // Set elected = true, `run()` will exit loop
                    // Send result to election at port 8000
                    try (Socket resultSocket = new Socket("localhost", 8000);
                            PrintWriter out = new PrintWriter(resultSocket.getOutputStream(), true);) {
                        String result = "{\"Candidate\": \"" + currentProposalCandidate + "\"}";
                        out.println(result);
                        resultSocket.close();
                    } catch (IOException e) {
                        logger.error("IOException sending result to election:", e);
                    } finally {
                        logger.info("President elected: " + currentProposalCandidate);
                    }
                }
                break;
            case "Fail":
                logger.info(
                        memberId + " received FAIL message from acceptor " + acceptorId + " in " + phase + " phase");
                break;
            default:
                logger.error(
                        memberId + " unknown message type from acceptor " + acceptorId + ": " + responseType + " in "
                                + phase + " phase");
                break;
        }
    }

    public void stopProposer() {
        logger.info(memberId + " proposer stopping...");
        elected.set(true); // Set elected = true, `run()` will exit loop
        logger.info(memberId + " proposer stopped.");
    }
}
