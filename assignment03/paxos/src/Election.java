import java.util.*;
import java.net.*;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.*;

public class Election {
    private static volatile String president = ""; // Majority chosen president
    private static List<Member> members = new ArrayList<Member>(); // List of members
    private static List<Thread> memberThreads = new ArrayList<Thread>(); // List of threads for each member
    private static ServerSocket resultListener; // ServerSocket listening for RESULT message
    private static Thread resultListenerThread; // Result Thread
    private static final Logger logger = LoggerFactory.getLogger(Election.class); // A logger

    // Main method
    public static void main(String[] args) {
        // Initialise & start result listener thread
        resultListenerThread = electPresident();
        resultListenerThread.start();

        // Create all member objects for M1-M9, create threads for each member, add them
        // & start them
        for (int i = 1; i < 10; i++) {
            createAndStartMembers("M" + Integer.toString(i));
        }

        // While listener is alive, wait
        waitForCompletion();

        // Print the president
        printPresident();
    }

    private static void createAndStartMembers(String memberId) {
        Member member = new Member(memberId);
        members.add(member);
        Thread memberThread = new Thread(() -> member.start(), memberId + "-Thread");
        memberThreads.add(memberThread);
        memberThread.start();
    }

    /**
     * Waits for the result listener thread to complete
     */
    private static void waitForCompletion() {
        try {
            resultListenerThread.join();
        } catch (InterruptedException e) {
            logger.warn("Result listener thread interrupted during wait:", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Exception waiting for result listener thread:\n", e);
        }
    }

    /**
     * Listens for the result message from the members and extracts the president,
     * then cleans up the election.
     */
    private static Thread electPresident() {
        return new Thread(() -> {
            try {
                resultListener = new ServerSocket(8000);
                logger.info("Listening for results on port 8000");

                while (president.isEmpty()) {
                    try (Socket clientSocket = resultListener.accept();
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(clientSocket.getInputStream()))) {
                        String jsonMessage = in.readLine();
                        if (jsonMessage != null) {
                            extractPresident(jsonMessage);
                        } else {
                            logger.error("Received empty message from clientSocket");
                        }

                    } catch (JsonParseException e) {
                        logger.error("JsonParseException parsing jsonMessage:\n", e);
                    } catch (IOException e) {
                        logger.error("IOException reading from BufferedReader:\n", e);
                    } catch (Exception e) {
                        logger.error("Exception accepting clientSocket:\n", e);
                    }
                }
            } catch (IOException e) {
                logger.error("IOException creating ServerSocket:\n", e);
            } catch (Exception e) {
                logger.error("Error accepting clientSocket:\n", e);
            } finally {
                stopElection();
            }
        }, "ResultListenerThread");
    }

    /**
     * Method to extract the president from the received message
     */
    private static synchronized void extractPresident(String jsonMessage) {
        if (jsonMessage != null) {
            JsonObject jsonObject = JsonParser.parseString(jsonMessage).getAsJsonObject();
            String candidate = jsonObject.get("Candidate").getAsString();

            if (candidate != null && !candidate.isEmpty() && president.isEmpty()) {
                president = candidate;
                logger.info("President elected: " + president);
            }
        }
    }

    /**
     * Calls the stop() method of each Member, and waits for each thread to join.
     * Then closes the ServerSocket.
     */
    private static void stopElection() {
        logger.info("Stopping election...");

        stopMembers(); // Stop each member
        stopThreads(); // Stop each thread
        closeListener(); // Close the ServerSocket
    }

    /**
     * Stops all members
     */
    private static void stopMembers() {
        for (Member member : members) {
            member.stop();
        }
    }

    /**
     * Stops all threads
     */
    private static void stopThreads() {
        for (Thread memberThread : memberThreads) {
            memberThread.interrupt();
            try {
                memberThread.join();
            } catch (InterruptedException e) {
                logger.error("InterruptedException joining memberThread:\n", e);
            } catch (Exception e) {
                logger.error("Exception stopping election:\n", e);
            }
        }
    }

    /**
     * Close the ServerSocket
     */
    private static void closeListener() {
        // Close the ServerSocket
        if (resultListener != null && !resultListener.isClosed()) {
            try {
                resultListener.close();
                logger.info("resultListener closed.");
            } catch (IOException e) {
                logger.error("IOException closing ServerSocket:\n", e);
            } catch (Exception e) {
                logger.error("Exception stopping election:\n", e);
            }
        }
    }

    /**
     * Print the president to the console
     */
    private static void printPresident() {
        logger.info("Election completed. The elected president is: " + president);
    }
}