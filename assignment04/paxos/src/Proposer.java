public class Proposer {
    private static int proposal_count = 0;
    private String member_id;

    /**
     * Constructor for Proposer class
     * 
     * @param member_id : String ID of the member
     */
    public Proposer(String member_id) {
        this.member_id = member_id;
    }

    /**
     * Method to increment proposal count, and return the new proposal number
     * 
     * @return proposal_count
     */
    public static synchronized int getNextProposalNumber() {
        proposal_count++;
        return proposal_count;
    }

    public void prepare() {
        // To do
    }
}
