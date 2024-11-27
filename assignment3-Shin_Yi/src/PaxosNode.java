import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class PaxosNode {
    private final int node_id; // Unique id for this PaxosNode
    private final PaxosProtocol protocol; // Instance to handle Paxos protocol logic
    private final ExecutorService executor_service; // For handling incoming tasks

    // Variables to set member's behaviour profiles
    private boolean online = true;
    private int response_delay = 0;
    private Predicate<PaxosMessage> message_filter;

    // Constructor for a new PaxosNode
    // Inputs: Its unique identifier 
    // Output: The PaxosNode class object
    public PaxosNode(int node_id) {
        this.node_id = node_id;
        this.protocol = new PaxosProtocol(node_id);
        this.executor_service = Executors.newCachedThreadPool();
    }

    // Method to start the PaxosNode
    // Input: None
    // Output: None
    public void start() {
        // Start listening for incoming messages
        new Thread(() -> new PaxosServer(this, node_id).start()).start();

        System.out.println("Member node: M" + node_id + " started and ready.");
    }

    // Method to receive incoming messages from node's own PaxosServer
    // Input: The Paxos message
    // Output: None
    public void receiveMessage(PaxosMessage message) {
        // If there is a message, & node is online, & if no filter is set, the message will always be processed
        if (message != null && online && (message_filter == null || message_filter.test(message))) {
            executor_service.submit(() -> {
                try {
                    Thread.sleep(response_delay); // Simulate delay based on responsiveness of member

                } catch (InterruptedException e) {
                    System.err.println("Error simulating response delay for member node: M" + node_id + " - " + e.getMessage());
                    Thread.currentThread().interrupt();
                }

                processMessage(message);
            });
        } else {
            System.out.println("M" + node_id + " is offline or ignoring message.");
        }
    }

    // Method to process the message based on its type
    // Input: The Paxos message
    // Output: None
    private void processMessage(PaxosMessage message) {
        switch(message.getType()) {
            case "PREPARE":
                protocol.handlePrepare(message);
                break;
            case "PROMISE":
                protocol.handlePromise(message);
                break;
            case "PROPOSE":
                protocol.handlePropose(message);
                break;
            case "ACCEPTED":
                protocol.handleAccepted(message);
                break;
            default:
                System.err.println("Unknown message type: " + message.getType());
        }
    }

    // Method to initiate a proposal for a candidate
    // Input: The candidate for president
    // Output: None
    public void startProposal(String candidate) {
        protocol.initiateProposal(candidate);
    }

    // Method to set the behaviour profile of member node
    // Inputs: Whether it is online and its delay time in responding
    // Output: None
    public void setProfile(boolean online, int response_delay) {
        this.online = online;
        this.response_delay = response_delay;
    }

    // Method to set if member node will filter messages, only responding to a select few
    // Input: The custom filter
    // Output: None
    public void setMessageFilter(Predicate<PaxosMessage> filter) {
        this.message_filter = filter;
    }

    // Method to shutdown the node gracefully
    // Input: None
    // Output: None
    public void stop() {
        executor_service.shutdown();
        protocol.shutdown();
        System.out.println("Member node: M" + node_id + " successfully shut down.");
    }
}