import java.util.*;

public class Member {
    private String member_id;
    private Proposer proposer;
    private Acceptor acceptor;
    private final int response_delay;
    private Boolean cafe_camp;

    public Member(String member_id) {
        this.member_id = member_id;
        this.proposer = new Proposer(member_id);
        this.acceptor = new Acceptor(member_id);

        // Set response delay based on the specified member
        Random rand = new Random();
        if (member_id.equals("M1")) {
            this.response_delay = 0; // Fastest, no delay
        } else if (member_id.equals("M2")) {
            cafe_camp = rand.nextBoolean();
            if (cafe_camp) { // If M2 at cafe, instant response
                this.response_delay = 0;
            } else { // If at home, slow response
                this.response_delay = 5000; // Slowest, 5 seconds delay
            }
        } else if (member_id.equals("M3")) {
            cafe_camp = rand.nextBoolean(); // If at camping, no response at all
            this.response_delay = 3000; // Middle, 3 seconds delay
        } else {
            this.response_delay = rand.nextInt(6) * 1000; // Random between 0 and 5 seconds
        }
    }
}
