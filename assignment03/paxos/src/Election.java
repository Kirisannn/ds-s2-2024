import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.sleep;

class Election {
    private static final Logger logger = LoggerFactory.getLogger(Election.class);
    private static List<Member> members = new ArrayList<Member>();

    public static void main(String[] args) throws InterruptedException {
        switch (args[0]) {
            case "1":
                test1();
                break;
            case "2":
                test2();
                break;
            case "3":
                test3();
                break;
            default:
                logger.error("Invalid test number.");
                break;
        }

        logger.info("Exiting...");
        System.exit(0);
    }

    // Start all members
    private static void startMembers(int testNumber) {
        for (int i = 1; i <= 9; i++) {
            Member member = null;
            if (testNumber == 3) { // Test 3, varied delays & responsiveness
                if (i == 1) { // Member 1
                    member = new Member("M" + i, 0, false, false);
                } else if (i == 2) { // Member 2
                    member = (new Random().nextBoolean()) ? new Member("M" + i, 0, true, false)
                            : new Member("M" + i, 5000, false, false);
                } else if (i == 3) { // Member 3
                    member = (new Random().nextBoolean()) ? new Member("M" + i, 0, false, false)
                            : new Member("M" + i, 1000, false, true);
                } else { // Members 4-9
                    member = new Member("M" + i, (new Random().nextInt(3) + 1) * 1000, true, false);
                }
            } else { // Test 1 or 2, all members respond instantly and respond
                member = new Member("M" + i, 0, true, false);
            }
            members.add(member);
            member.start();
        }
    }

    private static void stop() {
        members.forEach(Member::stop);

        members.clear();
    }

    /*
     * Test 1: 1 Proposer, all members respond instantly.
     */
    public static void test1() throws InterruptedException {
        logger.info("Starting test 1: 1 Proposer, all members respond instantly.");

        // Start members
        startMembers(1);

        sleep(3000);

        // M4 proposes
        members.get(5).beginProposal("M1");

        sleep(5000); // Wait for test to finish

        // Stop all members
        stop();

        logger.info("Test 1 completed.");
    }

    /*
     * Test 2: 2 Proposers, all members respond instantly.
     */
    public static void test2() throws InterruptedException {
        logger.info("Starting test 2: 2 Proposers, all members respond instantly.");
    }

    /*
     * Test 3: 1 Proposer, delays & responsiveness are varied.
     * M1: Responds instantly, always.
     * M2: Responds instantly, always if at work. Otherwise, delays by 5 seconds.
     * M3: Responds when not camping, delays by 1-4 seconds. Otherwise, doesn't
     * respond.
     * M4-M9: Responds always, with a delay of 1-3 seconds.
     */
    public static void test3() throws InterruptedException {
        logger.info("Starting test 3: 1 Proposer, delays & responsiveness are varied.");
    }
}
