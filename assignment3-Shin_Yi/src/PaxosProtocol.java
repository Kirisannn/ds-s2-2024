import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;

public class PaxosProtocol {
    private final int node_id;
    private final int num_nodes = 9; // 9 council members
    private final Map<Integer, PaxosClient> clients = new ConcurrentHashMap<>();

    // Variables for node acting as Proposer
    private final AtomicInteger proposal_counter = new AtomicInteger(0); // For generating unique proposal ids
    private final Map<Integer, String> promises = new ConcurrentHashMap<>(); // To track number of promises for that candidate
    private String proposed_candidate = null;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    // Variables for node acting as Acceptor
    private int highest_proposal_id = -1;
    private int accepted_proposal_id = -1;
    private String accepted_candidate = null;

    // Variables for node acting as a Learner
    private final Map<String, AtomicInteger> accepted = new ConcurrentHashMap<>(); // To track number of acceptance for that candidate
    private AtomicBoolean consensus_reached = new AtomicBoolean(false);

    // Constructor for a new PaxosProtocol
    // Input: The PaxosNode's identifier
    // Output: The PaxosProtocol class object
    public PaxosProtocol(int node_id) {
        this.node_id = node_id;
    }

    // Method to broadcast a message to all nodes
    // Input: The Paxos message
    // Output: None
    private void broadcastMessage(PaxosMessage message) {
        for (int i = 1; i <= num_nodes; i++) {
            // Except for itself
            if (i != node_id) {
                int recipient = i;
                executor.submit(() -> sendTo(recipient, message));
            }
        }
    }

    // Method to reply/send a message to a single node using PaxosClient
    // Inputs: The identifier of recipient node and the Paxos message
    // Output: None
    private void sendTo(int recipient_node_id, PaxosMessage message) {
        clients.computeIfAbsent(recipient_node_id, PaxosClient::new).sendMessage(message);
    }

    // Method to initiate a proposal as the Proposer
    // Input: The candidate member for president
    // Output: None
    public void initiateProposal(String candidate) {
        int proposal_id = (int) System.currentTimeMillis() + node_id;
        proposed_candidate = candidate;

        // Send "PREPARE" message to all nodes
        PaxosMessage prepare_message = new PaxosMessage("PREPARE", proposal_id, null, node_id);
        System.out.println("----------------------------------");
        System.out.println("Member M" + node_id + " has broadcasted PREPARE message with proposal ID: " + proposal_id);
        System.out.println("----------------------------------");
        broadcastMessage(prepare_message);
    }

    // Method to handle a PREPARE message as an Acceptor
    // Input: The Paxos message
    // Output: None
    public void handlePrepare(PaxosMessage message) {
        // If received proposal id is higher than Acceptor's current highest seen id
        if (message.getProposalId() > highest_proposal_id) {
            // Update highest seen proposal id
            highest_proposal_id = message.getProposalId();

            // If a candidate was accepted prior to this PREPARE message, reply PROMISE with accepted candidate
            PaxosMessage promise_message;
            if (accepted_proposal_id != -1) {
                promise_message = new PaxosMessage("PROMISE", highest_proposal_id, accepted_candidate, node_id);
            
            // Else, just respond with a PROMISE message
            } else {
                promise_message = new PaxosMessage("PROMISE", highest_proposal_id, "NO_CANDIDATE", node_id);
            }

            sendTo(message.getSenderId(), promise_message);
            System.out.println("Member M" + node_id + " has replied with a PROMISE message for proposal ID: " + highest_proposal_id);
        
        // Else Acceptor ignores PREPARE message
        } else {
            System.out.println("Member M" + node_id + " ignored PREPARE message with proposal ID: " + message.getProposalId() + " (already seen higher proposal ID: " + highest_proposal_id + ")");
            return;
        }
    }

    // Method to handle a PROMISE message as a Proposer
    // Input: The Paxos message
    // Output: None
    public void handlePromise(PaxosMessage message) {
        // Store the promise
        synchronized (promises) {
            // System.out.println("Sender id: " + message.getSenderId() + ", candidate: " + message.getCandidate());
            promises.put(message.getSenderId(), message.getCandidate());

            // System.out.println("IM HERE");

            // If a majority of Acceptors have responded with PROMISE
            if (promises.size() >= (num_nodes/2)+1) {
                // If a response contains an accepted candidate, propose that candidate instead
                String candidate = promises.values().stream()
                                                    .filter(val -> !"NO_CANDIDATE".equals(val))
                                                    .findFirst()
                                                    .orElse(proposed_candidate);
                proposed_candidate = candidate;

                // Send PROPOSE message to all nodes
                PaxosMessage propose_message = new PaxosMessage("PROPOSE", message.getProposalId(), proposed_candidate, node_id);
                System.out.println("----------------------------------");
                System.out.println("Member M" + node_id + " has broadcasted PROPOSE message for proposal ID: " + message.getProposalId() + " with candidate " + proposed_candidate);
                System.out.println("----------------------------------");
                broadcastMessage(propose_message);

                promises.clear(); // Clear map to avoid duplicate handling
            }
        }
    }

    // Method to handle a PROPOSE message as an Acceptor
    // Input: The Paxos message
    // Output: None
    public void handlePropose(PaxosMessage message) {
        // If this proposal id is highest seen so far
        if (message.getProposalId() >= highest_proposal_id) {
            // Update highest seen proposal id and accepted candidate
            highest_proposal_id = message.getProposalId();
            accepted_proposal_id = message.getProposalId();
            accepted_candidate = message.getCandidate();

            // Respond with ACCEPTED message to Proposer and all Learners
            PaxosMessage accepted_message = new PaxosMessage("ACCEPTED", accepted_proposal_id, accepted_candidate, node_id);
            broadcastMessage(accepted_message);
        }
    }

    // Method to handle an ACCEPTED message as a Learner
    // Input: The Paxos message
    // Output: None
    public void handleAccepted(PaxosMessage message) {
        // Store the acceptance
        String candidate = message.getCandidate();
        accepted.computeIfAbsent(candidate, k -> new AtomicInteger(0)).incrementAndGet();

        // If this candidate has received a majority of acceptances
        if (accepted.get(candidate).get() >= (num_nodes/2)+1) {
            // Atomically check and update consensus status
            if (consensus_reached.compareAndSet(false, true)) {
                System.out.println("Consensus reached on member: " + candidate + " to be our new President!");
            }
        }
    }

    // Method to shutdown the thread pool gracefully
    // Input: None
    // Output: None
    public void shutdown() {
        executor.shutdown();
    }
}