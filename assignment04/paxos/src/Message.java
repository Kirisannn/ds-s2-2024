/**
 * Abstract Message class to be extended by all message types
 * Message types include:
 * 1. Prepare (Phase 1: Prepare-Promise)
 * 2. Promise (Phase 1: Prepare-Promise)
 * 3. Propose (Phase 2: Propose-Acccept)
 * 4. Accept (Phase 2: Propose-Accept)
 * 5. Fail
 */
public class Message {
    private String from_id;
    private String msg_type;
    private String to_id;
    private int to_port;

    public Message(String from_id, String msg_type, String to_id, int to_port) {
        this.from_id = from_id;
        this.msg_type = msg_type;
        this.to_id = to_id;
        this.to_port = to_port;
    }
}

class Prepare extends Message {
    private int proposal_number;

    public Prepare(String from_id, String to_id, int to_port, int proposal_number) {
        super(from_id, "Prepare", to_id, to_port);
        this.proposal_number = proposal_number;
    }
}