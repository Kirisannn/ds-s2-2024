import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.*;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.*;

public class Election {
    private static final Logger logger = LoggerFactory.getLogger(Election.class); // A logger
    private static final Gson gson = new Gson(); // A Gson object for JSON parsing
    private static final List<String> member_ids = Collections
            .unmodifiableList(List.of("M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9"));
    private static List<Member> members = new ArrayList<Member>(); // List of members
    private static List<Thread> threads = new ArrayList<Thread>(); // List of threads for each member
    private static String president = ""; // Majority chosen president
    private static Boolean consensus_reached = false; // Indicates if consensus has been reached
    private static ServerSocket serverSocket; // ServerSocket for the election
    private static int total_votes = 0; // Should not exceed 9

    // Main method
    public static void main(String[] args) {
        // New thread for consensus checking

        try { // Set up the server socket for listening to final votes from members
            serverSocket = new ServerSocket(8000);

            // Start consensus checking thread
            Thread consensus_thread = new Thread(Election::consensusCheck);
            consensus_thread.start();

            // Consensus reached, close the server socket
            try {
                serverSocket.close();
                logger.info("Server socket closed successfully.");
            } catch (IOException e) {
                logger.error("IOException closing server socket: " + e.getMessage());
            } catch (NullPointerException e) {
                logger.error("NullPointerException closing server socket: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unknown exception closing server socket: " + e.getMessage());
            }

            // Join consensus thread
            try {
                consensus_thread.join();
            } catch (InterruptedException e) {
                logger.error("Error joining consensus thread: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unknown exception joining consensus thread: " + e.getMessage());
            }

            // Finally, print the president
            logger.info("Election completed. The elected president is: " + president);
        } catch (IOException e) {
            logger.error("IOException initialising server socket: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unknown exception initialising server socket: " + e.getMessage());
        }
    }

    // Method to check if consensus has been reached
    private static void consensusCheck() {
        Map<String, Integer> voteCounts = new HashMap<String, Integer>(); // Map to store vote counts
        Map<String, String> votes = new HashMap<String, String>(); // Map to store votes
        for (String id : member_ids) { // Initialise all vote counts to 0
            voteCounts.put(id, 0);
        }
        ExecutorService executor = Executors.newCachedThreadPool(); // Threadpool for handling member connections

        try {
            while (!consensus_reached) {
                // Accept connections from members
                Socket socket = serverSocket.accept();
                logger.info("Accepted connection from:\t{" + socket.getInetAddress().getHostName() + ":"
                        + socket.getPort() + "}");

                // Handle the connection in a new thread
                executor.submit(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                        String message = reader.readLine();
                        logger.info("Received message: " + message);
                        JsonObject json = JsonParser.parseString(message).getAsJsonObject();

                        // Extract member_id and vote from the message
                        String member_id = json.get("member_id").getAsString();
                        String vote = json.get("vote").getAsString();

                        synchronized (Election.class) {
                            // Check if member has already voted
                            if (voteCounts.containsKey(member_id) && voteCounts.get(member_id) > 0) {
                                // If yes, then stop everything
                                logger.error("Member " + member_id + " has already voted.");
                                return;
                            }

                            // Otherwise, increment the vote count & put the vote in the votes map
                            voteCounts.put(member_id, 1);
                            total_votes++;
                            votes.put(member_id, vote);

                            // If president has not been set, set it
                            if (president.isEmpty()) {
                                president = vote;
                                logger.info("President set to: " + president);
                            }

                            // Validate votes
                            if (total_votes == 9) {
                                consensus_reached = validateVotes(votes);
                            }
                        }
                    } catch (IOException e) {
                        logger.error("IOException reading from member socket: ", e);
                    } catch (Exception e) {
                        logger.error("Unknown exception reading from member socket: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Unknown exception in consensusCheck: " + e.getMessage());
        }
    }

    private static Boolean validateVotes(Map<String, String> votes) {
        for (Map.Entry<String, String> entry : votes.entrySet()) {
            if (!entry.getValue().equals(president)) {
                logger.error("Vote mismatch: " + entry.getKey() + " voted for " + entry.getValue());
                return false; // False if there's a mismatch
            }
        }
        return true; // True if no mismatches
    }

    // Create members M1-M9
    private void createMembers() {

    }

    // Start the election, create threads for each member and start them
    public void startElection() {

    }

    // Finish the election, and join all threads
    private void stopElection() {

    }

}