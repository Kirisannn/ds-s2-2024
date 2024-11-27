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

public class Paxos {
    private static final Logger logger = LoggerFactory.getLogger(Election.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final String memberId;

    private int totalProposals = 0;
    private int currentMaxProposal = 0;
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

    public Paxos(String memberId) {
        this.memberId = memberId;
    }

    public void shutdown() {
        executor.shutdown();
    }

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

    public void initialiseProposal(String candidate) {
        currentProposal = incAndGetProposalNumber();
        currentCandidate = candidate;

        // Send "Prepare" message to all nodes
        Prepare prepare = new Prepare(memberId, currentProposal);
        System.out.println(
                "Member " + memberId + " broadcasting PREPARE message, proposal number: " + currentProposal);
        broadcastMessage(prepare);
    }

    private int incAndGetProposalNumber() {
        totalProposals++;
        return totalProposals;
    }

    private void broadcastMessage(Message msg) {
        for (int i = 1; i <= 9; i++) {
            if (i != Integer.parseInt(memberId.substring(1))) {
                sendTo("M" + i, msg);
            }
        }
    }

    public void receivePrepare(Message msg) {
        // if received proposal num > current proposal num
        if (msg.getProposalNumber() > currentMaxProposal) {
            currentMaxProposal = msg.getProposalNumber();

            // If accepted proposal exists, send promise with accepted proposal. Else, send
            // promise with candidate == null
            Promise promise = (acceptedProposal != 0) ? new Promise(memberId, currentMaxProposal, acceptedCandidate)
                    : new Promise(memberId, currentMaxProposal, null);

            // send promise
            logger.info(memberId + " sending PROMISE message to " + msg.getSender() + ", proposal number: "
                    + currentMaxProposal);
            sendTo(msg.getSender(), promise);
        } else { // else send fail
            Fail fail = new Fail(memberId);
            logger.info(memberId + " sending FAIL message to " + msg.getSender() + ", proposal number: "
                    + currentMaxProposal);
            sendTo(msg.getSender(), fail);
        }
    }

    public void receivePromise(Message msg) {
        // Record promise
        synchronized (promises) {
            if (msg.getCandidate() != null) {
                logger.info(memberId + " received PROMISE message from " + msg.getSender() + ",  with candidate: "
                        + msg.getCandidate());
            }
            // Record promise, candidate null if no accepted proposal or an accepted
            // candidate
            promises.put(msg.getSender(), msg.getCandidate());

            if (promises.size() > 4) {
                // Find promise with candidate not null
                String proposalCandidate = null;
                for (String candidate : promises.values()) {
                    if (candidate != null) {
                        proposalCandidate = candidate;
                        break;
                    }
                }

                // Send propose message with candidate
                Propose propose = new Propose(memberId, currentMaxProposal, proposalCandidate);
                logger.info(memberId + " broadcasting PROPOSE message, proposal number: " + currentMaxProposal
                        + ", candidate: " + proposalCandidate);
                broadcastMessage(propose);
            } else if (promises.size() + 1 > 4) {
                // Send propose message with current candidate
                Propose propose = new Propose(memberId, currentMaxProposal, currentCandidate);
                logger.info(memberId + " broadcasting PROPOSE message, proposal number: " + currentMaxProposal
                        + ", candidate: " + currentCandidate);
                broadcastMessage(propose);
            }
            promises.clear(); // Reset promises
        }
    }

    public void receivePropose(Message msg) {
        if (msg.getProposalNumber() >= currentMaxProposal) {
            // Update max proposal number, accepted proposal and candidate
            currentMaxProposal = msg.getProposalNumber();
            acceptedProposal = currentMaxProposal;
            acceptedCandidate = msg.getCandidate();

            // Send accept message
            Accept accept = new Accept(memberId, currentMaxProposal, acceptedCandidate);
            logger.info(
                    memberId + " broadcasting ACCEPT message, proposal number: " + currentMaxProposal + ", candidate: "
                            + acceptedCandidate);
            broadcastMessage(accept);

        } else {
            // Send fail message
            Fail fail = new Fail(memberId);
            logger.info(memberId + " broadcasting FAIL message, proposal number: " + currentMaxProposal);
            broadcastMessage(fail);
        }
    }

    public void receiveAccept(Message msg) {
        // Record accept
        synchronized (accepts) {
            // Put candidate and increment count
            accepts.computeIfAbsent(msg.getCandidate(), count -> new AtomicInteger(0)).incrementAndGet();

            if (accepts.get(msg.getCandidate()).get() > 4) {
                logger.info("Consensus reached on proposal: " + msg.getProposalNumber() + ", candidate: "
                        + msg.getCandidate() + "! New president: " + msg.getCandidate());
            }
        }
    }
}