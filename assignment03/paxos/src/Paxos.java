import static java.lang.Thread.sleep;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Paxos class implements the Paxos consensus algorithm for distributed
 * systems.
 * It coordinates the proposal, acceptance, and consensus phases of Paxos.
 */
public class Paxos {
    private static final Logger logger = LoggerFactory.getLogger(Election.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final String memberId;

    private AtomicInteger currentMaxProposal = new AtomicInteger(0);
    private int currentProposal = 0;
    private String currentCandidate = "";
    private int acceptedProposal = 0;
    private String acceptedCandidate = "";

    private final Map<String, String> promises = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> accepts = new ConcurrentHashMap<>();
    private static final Map<String, Integer> listenerPorts = Map.ofEntries( // All acceptor ports
            Map.entry("M1", 5001),
            Map.entry("M2", 5002),
            Map.entry("M3", 5003),
            Map.entry("M4", 5004),
            Map.entry("M5", 5005),
            Map.entry("M6", 5006),
            Map.entry("M7", 5007),
            Map.entry("M8", 5008),
            Map.entry("M9", 5009));

    /**
     * Constructs a Paxos instance for the given member.
     *
     * @param memberId the unique identifier of the member running this Paxos
     *                 instance.
     */
    public Paxos(String memberId) {
        this.memberId = memberId;
    }

    /**
     * Gracefully shuts down the executor used for handling tasks.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Sends a message to a specified recipient by connecting to their listener
     * port.
     *
     * @param recipient the ID of the recipient (e.g., "M1").
     * @param msg       the message to send.
     */
    public void sendTo(String recipient, Message msg) {
        int port = listenerPorts.get(recipient);
        // Send message to recipient
        try {
            Socket socket = new Socket("localhost", port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(msg.toString());
            sleep(1000);
            socket.close();
        } catch (IOException e) {
            logger.error(memberId + " encountered IOException while sending" + msg.getType() + "message to " + recipient
                    + ": ", e);
        } catch (InterruptedException e) {
            logger.error(memberId + " encountered InterruptedException while sending" + msg.getType() + "message to "
                    + recipient + ": ", e);
        }
    }

    /**
     * Initializes a proposal with a specified candidate and broadcasts a PREPARE
     * message to all members.
     *
     * @param candidate the candidate being proposed.
     */
    public void initialiseProposal(String candidate) {
        currentProposal = getProposalNumber();
        currentCandidate = candidate;

        // Send "Prepare" message to all nodes
        Prepare prepare = new Prepare(memberId, currentProposal);
        System.out.println(
                "Member " + memberId + " broadcasting PREPARE message, proposal number: " + currentProposal);
        broadcastMessageToAll(prepare);
    }

    /**
     * Generates a unique proposal number based on the current system time and
     * member ID.
     *
     * @return a unique proposal number.
     */
    private int getProposalNumber() {
        // return the timestamp of the current system time
        return (int) System.currentTimeMillis() + Integer.parseInt(memberId.substring(1));
    }

    /**
     * Broadcasts a message to all other members in the system.
     *
     * @param msg the message to broadcast.
     */
    private void broadcastMessageToAll(Message msg) {
        for (int i = 1; i <= 9; i++) {
            if (i != Integer.parseInt(memberId.substring(1))) {
                sendTo("M" + i, msg);
            }
        }
    }

    /**
     * Broadcasts a message only to members that have sent a PROMISE.
     *
     * @param msg the message to broadcast.
     */
    private void broadcastMessageToPromisors(Message msg) {
        for (String promisor : promises.keySet()) {
            sendTo(promisor, msg);
        }
    }

    /**
     * Handles a received PREPARE message.
     * Responds with a PROMISE if the proposal number is valid, otherwise sends a
     * FAIL.
     *
     * @param msg the PREPARE message received.
     */
    public synchronized void receivePrepare(Message msg) {
        if (msg.getProposalNumber() <= currentMaxProposal.get()) { // else send fail
            logger.info(memberId + " rejecting lower proposal: " + msg.getProposalNumber());
            Fail fail = new Fail(memberId);
            logger.info(memberId + " sending FAIL message to " + msg.getSender() + ", proposal number: "
                    + currentMaxProposal);
            sendTo(msg.getSender(), fail);
            return;
        } else if (msg.getProposalNumber() > currentMaxProposal.get()) {
            // if received proposal num > current proposal num
            currentMaxProposal.set(msg.getProposalNumber());
            logger.info("Current max proposal of " + memberId + ": " + currentMaxProposal);

            // If accepted proposal exists, send promise with accepted proposal. Else, send
            // promise with candidate == null
            Promise promise = (acceptedProposal != 0)
                    ? new Promise(memberId, currentMaxProposal.get(), acceptedCandidate)
                    : new Promise(memberId, currentMaxProposal.get(), null);

            // send promise
            logger.info(memberId + " sending PROMISE message to " + msg.getSender() + ", proposal number: "
                    + currentMaxProposal);
            sendTo(msg.getSender(), promise);
        } else { // else send fail
            Fail fail = new Fail(memberId);
            logger.info(memberId + " sending FAIL message to " + msg.getSender() + ", proposal number: "
                    + currentMaxProposal);
            sendTo(msg.getSender(), fail);
            return;
        }
    }

    /**
     * Handles a received PROMISE message.
     * Tracks received PROMISES and, once a majority is reached, sends a PROPOSE
     * message.
     *
     * @param msg the PROMISE message received.
     */
    public void receivePromise(Message msg) {
        synchronized (promises) {
            if (msg.getCandidate() != null) {
                logger.info(memberId + " received PROMISE message from " + msg.getSender() + ", with candidate: "
                        + msg.getCandidate());
                promises.put(msg.getSender(), msg.getCandidate());
            } else {
                logger.info(
                        memberId + " received PROMISE message from " + msg.getSender() + ", no candidate specified.");
                promises.put(msg.getSender(), currentCandidate);
            }

            if (promises.size() > 4) { // Majority of PROMISES received
                // Determine the proposal candidate
                String proposalCandidate = currentCandidate; // Default to current candidate
                logger.info("Current candidate: " + currentCandidate);
                for (String candidate : promises.values()) {
                    // logger.info("candidate: " + candidate);
                    if (!candidate.equals("null")) {
                        proposalCandidate = candidate;
                        break;
                    }
                }

                logger.info(memberId + " selected candidate " + proposalCandidate + " for proposal.");

                // Send PROPOSE message
                Propose propose = new Propose(memberId, currentProposal, proposalCandidate);
                logger.info(
                        "Member: " + memberId + ", Proposal: " + currentProposal + ", Candidate: " + proposalCandidate);
                logger.info(memberId + " broadcasting PROPOSE message, proposal number: " + currentProposal
                        + ", candidate: " + proposalCandidate);

                // Broadcast PROPOSE message to promisors
                broadcastMessageToPromisors(propose);

                // Reset promises
                promises.clear();
            }
        }
    }

    /**
     * Handles a received PROPOSE message.
     * Responds with an ACCEPT message if the proposal number is valid, otherwise
     * sends a FAIL.
     *
     * @param msg the PROPOSE message received.
     */
    public void receivePropose(Message msg) {
        if (msg.getProposalNumber() >= currentMaxProposal.get()) {
            // Update max proposal number, accepted proposal and candidate
            currentMaxProposal.set(msg.getProposalNumber());
            acceptedProposal = currentMaxProposal.get();
            acceptedCandidate = msg.getCandidate();

            // Send accept message
            Accept accept = new Accept(memberId, currentMaxProposal.get(), acceptedCandidate);
            logger.info(
                    memberId + " sending ACCEPT message to " + msg.getSender() + ", proposal number: "
                            + currentMaxProposal + ", candidate: "
                            + acceptedCandidate);
            sendTo(msg.getSender(), accept);
        } else {
            // Send fail message
            Fail fail = new Fail(memberId);
            logger.info(memberId + " sending FAIL message to " + msg.getSender() + ", proposal number: "
                    + currentMaxProposal);
            sendTo(msg.getSender(), fail);
        }
    }

    /**
     * Handles a received ACCEPT message.
     * Tracks ACCEPT messages and determines if a consensus is reached.
     *
     * @param msg the ACCEPT message received.
     */
    public synchronized void receiveAccept(Message msg) {
        // Record accept
        synchronized (accepts) {
            // Put candidate and increment count
            accepts.computeIfAbsent(msg.getCandidate(), count -> new AtomicInteger(0)).incrementAndGet();

            if (accepts.get(msg.getCandidate()).get() > 4) {
                logger.info("\n\nConsensus reached on proposal: " + msg.getProposalNumber() + ", candidate: "
                        + msg.getCandidate() + "!");

                // Exit
                shutdown();
                System.exit(0);
            }
        }
    }
}