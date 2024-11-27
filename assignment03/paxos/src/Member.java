import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.sleep;

public class Member {
    private static final Logger logger = LoggerFactory.getLogger(Election.class);
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final Paxos paxos;
    private String memberId;
    private int delay;
    private boolean working = false;
    private boolean camping = false;
    private boolean responding;

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

    public Member(String memberId, int delay, boolean working, boolean camping) {
        this.memberId = memberId;
        this.paxos = new Paxos(memberId);
        this.delay = delay;
        responding = true;
        if (working) {
            this.working = true;
        }
        if (camping) {
            this.camping = true;
        }
    }

    public void start() {
        // Start listening
        new Thread(() -> new Listener(this, listenerPorts.get(memberId)).start(), memberId + "-Listener").start();
        logger.info(memberId + " started listening on port " + listenerPorts.get(memberId) + "...");
    }

    public void beginProposal(String candidate) {
        paxos.initialiseProposal(candidate);
    }

    public void receive(Message msg) {
        // If message is not empty and member is responding
        if (working) {
            responding = true;
            logger.error(memberId + " is working. Responding: " + responding);
        } else if (!working && !camping) {
            responding = new Random().nextBoolean();
            logger.error(memberId + " is at home. Responding: " + responding);
        } else if (camping) {
            responding = false;
            logger.error(memberId + " is camping. Responding: " + responding);
        } else if (!camping) {
            responding = true;
            logger.error(memberId + " is not working. Responding: " + responding);
        }
        if (msg != null && responding) {
            logger.info(memberId + " received message: " + msg);
            executor.submit(() -> {
                try {
                    sleep(delay);
                } catch (InterruptedException e) {
                    logger.error(memberId + " encountered InterruptedException while responding: ", e);
                    Thread.currentThread().interrupt();
                }
                processMessage(msg);
            });
        } else {
            logger.warn(memberId + " is not responding.");
        }
    }

    private void processMessage(Message msg) {
        switch (msg.getType()) {
            case "Prepare":
                paxos.receivePrepare(msg);
                break;
            case "Promise":
                paxos.receivePromise(msg);
                break;
            case "Propose":
                paxos.receivePropose(msg);
                break;
            case "Accept":
                paxos.receiveAccept(msg);
                break;
            case "Fail":
                logger.error(memberId + " received a Fail message from " + msg.getSender());
                break;
            default:
                logger.error(
                        memberId + " received an unknown message type: " + msg.getType() + " from " + msg.getSender());
        }
    }

    public void stop() {
        executor.shutdown();
        paxos.shutdown();
        System.out.println("Member node: M" + memberId + " successfully shut down.");
    }

    // Getters
    public String getMemberId() {
        return memberId;
    }
}