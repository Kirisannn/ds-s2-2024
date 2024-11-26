import java.io.Serializable;

public class PaxosMessage implements Serializable {
    private String type;
    private int proposal_id; // Unique id for the proposal
    private String candidate; // Candidate president for proposal messages
    private int sender_id; 

    // Constructor to initialise a new Paxos message
    // Inputs: The type of message, the proposal's id, and the candidate president
    // Output: The PaxosMessage class object
    public PaxosMessage(String type, int proposal_id, String candidate, int sender_id) {
        this.type = type;
        this.proposal_id = proposal_id;
        this.candidate = candidate;
        this.sender_id = sender_id;
    }

    // Method to get type of message
    // Input: None
    // Output: "PREPARE" / "PROMISE" / "PROPOSE" / "ACCEPTED"
    public String getType() {
        return type;
    }

    // Method to get message's proposal id
    // Input: None
    // Output: The proposal id
    public int getProposalId() {
        return proposal_id;
    }

    // Method to get message's candidate
    // Input: None
    // Output: The candidate
    public String getCandidate() {
        return candidate;
    }

    // Method to get the id of sender
    // Input: None
    // Output: The message sender's unique identifier
    public int getSenderId() {
        return sender_id;
    }
}