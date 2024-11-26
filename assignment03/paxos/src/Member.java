import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.sleep;

public class Member {
    private final String memberId; // Indicates which member this is
    private final int delay; // The response delay of the member
    private boolean working = false; // Indicates if M2 is working at the cafe
    private boolean camping = false; // Indicates if M3 is camping
    private boolean isProposing; // Indicates if the member is proposing
    private final Acceptor acceptor; // Acceptor part of the member
    private Proposer proposer; // Proposer part of the member
    private Thread proposerThread; // Thread for proposer
    private Thread acceptorThread; // Thread for acceptor

    private static final Logger logger = LoggerFactory.getLogger(Proposer.class); // A logger

    public Member(String memberId) {
        this.memberId = memberId;
        this.delay = calculateDelay();
        this.isProposing = toPropose();
        if (isProposing && !camping) { // Create proposer if member is proposing, and not camping (only if M3)
            proposer = new Proposer(memberId, delay, working);
            proposerThread = new Thread(() -> proposer.run(), memberId + "-Proposer-Thread");
        }
        acceptor = new Acceptor(memberId, delay, working);
        acceptorThread = new Thread(() -> acceptor.run(), memberId + "-Acceptor-Thread");
    }

    /**
     * Determines the message response delay for this member.
     *
     * @return The delay in milliseconds.
     */
    private int calculateDelay() {
        if (memberId.equals("M1")) { // Always instant response
            return 0;
        } else if (memberId.equals("M2")) {
            if (ThreadLocalRandom.current().nextBoolean()) { // M2 is working at the cafe
                working = true;
                return 0;
            }
            return 5000; // M2 has a 5 second delay
        } else if (memberId.equals("M3")) {
            if (ThreadLocalRandom.current().nextBoolean()) { // M3 is camping
                camping = true;
                return -1;
            }
            return 3000; // M3 has a 3 second delay
        } else { // M4-M9 have random delays between 0-5 seconds
            return ThreadLocalRandom.current().nextInt(6) * 1000;
        }
    }

    /**
     * Determines if this member should make proposals.
     * M1-M3 always make proposals; others do so randomly.
     *
     * @return True if the member should make proposals; false otherwise.
     */
    private boolean toPropose() {
        // M1-M3 always propose
        if (memberId.equals("M1") || memberId.equals("M2") || memberId.equals("M3")) {
            return true;
        }
        return ThreadLocalRandom.current().nextBoolean(); // M4-M9 propose randomly
    }

    public void start() {
        logger.info("Starting member " + memberId + " with delay " + delay + "ms...");

        // Start Acceptor thread
        acceptorThread.start();
        logger.info(memberId + " acceptor started.");

        // Start Proposer thread
        if (isProposing && !camping && proposerThread != null) {
            proposerThread.start();
            logger.info(memberId + " proposer started.");
        }

        // Wait for proposer and acceptor threads to complete
        while (!Thread.currentThread().isInterrupted()) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("InterruptedException in member " + memberId + ":\n", e);
                break;
            }
        }

        // Ensure stopping of threads when exiting
        stop();
    }

    /**
     * Stops the proposer (if it exists) and acceptor threads, and joins them.
     */
    public void stop() {
        logger.info("Stopping member " + memberId + "...");

        // Stop proposer thread
        if (isProposing && !camping && proposerThread != null) {
            proposer.stopProposer();
            try {
                proposerThread.join();
                logger.info(memberId + " proposer thread joined.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("InterruptedException joining proposerThread for " + memberId + ":\n", e);
            }
        }

        // Stop acceptor thread
        acceptor.stop();
        try {
            acceptorThread.join();
            logger.info(memberId + " acceptor thread joined.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("InterruptedException joining acceptorThread" + memberId + ":\n", e);
        }

        logger.info("Member " + memberId + " stopped.");
    }

    // Getters
    public String getId() {
        return memberId;
    }

    public int getDelay() {
        return delay;
    }
}
