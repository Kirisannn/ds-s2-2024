import java.io.*;

/**
 * Abstract Message class to be extended by all message types
 * Message types include:
 * 1. Prepare (Phase 1: Prepare-Promise)
 * 2. Promise (Phase 1: Prepare-Promise)
 * 3. Propose (Phase 2: Propose-Acccept)
 * 4. Accept (Phase 2: Propose-Accept)
 * 5. Fail
 * 
 * @param sender_id:       ID of the sender
 * @param msg_type:        Msg type: Prepare, Promise, Propose, Accept, Fail
 * @param proposal_number: Proposal number (ever increasing)
 * @param candidate:       The candidate proposed
 */
public class Message implements Serializable {
    private String sender_id;
    private String msg_type;
    private int proposal_number;
    private String candidate; // Not necessary for Prepare & Fail

    public Message(String sender_id, String msg_type, int proposal_number, String candidate) {
        this.sender_id = sender_id;
        this.msg_type = msg_type;
        this.candidate = candidate;
        this.proposal_number = proposal_number;
    }

    // Override toString method to print message in JSON format
    @Override
    public String toString() {
        return String.format(
                "{\"Sender-ID\": \"%d\", \"Message-Type\": \"%s\", \"Proposal-Number\": \"%d\", \"Candidate\": \"%s\"}",
                sender_id, msg_type, proposal_number, candidate);
    }

    // Getters
    public String getSender() {
        return sender_id;
    }

    public String getType() {
        return msg_type;
    }

    public int getProposalNumber() {
        return proposal_number;
    }

    public String getCandidate() {
        return candidate;
    }
}

class Prepare extends Message {
    public Prepare(String sender_id, int proposal_number) {
        super(sender_id, "Prepare", proposal_number, null);
    }
}

/**
 * Promise message. Contains only proposal number if no proposal has been
 * accepted
 * Contains both proposal number and candidate if a proposal HAS been accepted
 */
class Promise extends Message {
    public Promise(String sender_id, int proposal_number, String candidate) {
        super(sender_id, "Promise", proposal_number, candidate);
    }
}

class Propose extends Message {
    public Propose(String sender_id, int proposal_number, String candidate) {
        super(sender_id, "Propose", proposal_number, candidate);
    }
}

class Accept extends Message {
    public Accept(String sender_id, int proposal_number, String candidate) {
        super(sender_id, "Accept", proposal_number, candidate);
    }
}

class Fail extends Message {
    public Fail(String sender_id) {
        super(sender_id, "Fail", -1, null);
    }
}