import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.sleep;

class Election {
    private static final Logger logger = LoggerFactory.getLogger(Election.class);
    private static List<Member> members = new ArrayList<Member>();

    /**
     * Main method to run tests based on the provided test number.
     * Valid test numbers are 1 through 5.
     *
     * @param args the command-line arguments. Expects a single argument specifying
     *             the test number.
     * @throws InterruptedException if any thread is interrupted during sleep
     *                              operations.
     */
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
            case "4":
                test4();
                break;
            case "5":
                test5();
                break;
            default:
                logger.error("Invalid test number.");
                break;
        }

        logger.info("Exiting...");
        System.exit(0);
    }

    /**
     * Starts the members for the election. Each member is assigned a role
     * and behavior based on the test scenario.
     *
     * @param testNumber the test number determining member configurations.
     */
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

    /**
     * Stops all members and clears the member list.
     */
    private static void stop() {
        members.forEach(Member::stop);

        members.clear();
    }

    /**
     * Test 1: Single proposer scenario where all members respond instantly.
     * 
     * @throws InterruptedException if the thread is interrupted during sleep.
     */
    public static void test1() throws InterruptedException {
        logger.info("Starting test 1: 1 Proposer, all members respond instantly.");

        // Start members
        startMembers(1);

        sleep(3000);

        // M4 proposes
        members.get(5).beginProposal("M1");

        // Wait for proposal to complete
        sleep(15000);

        // Stop all members
        stop();

        logger.info("Test 1 completed.");
    }

    /**
     * Test 2: Two proposers scenario where all members respond instantly.
     * 
     * @throws InterruptedException if the thread is interrupted during sleep.
     */
    public static void test2() throws InterruptedException {
        logger.info("Starting test 2: 2 Proposers, all members respond instantly.");

        // Start members
        startMembers(2);

        sleep(3000);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> members.get(0).beginProposal("M1")); // M4 proposes M1
        executor.submit(() -> members.get(5).beginProposal("M2")); // M6 proposes M2

        // Wait for proposal to complete
        sleep(30000);

        // Stop all members
        stop();
        executor.shutdown();

        logger.info("Test 2 completed.");
    }

    /**
     * Test 3: Single proposer scenario with varied delays and responsiveness.
     * 
     * @throws InterruptedException if the thread is interrupted during sleep.
     */
    public static void test3() throws InterruptedException {
        logger.info("Starting test 3: 1 Proposer, delays & responsiveness are varied.");

        startMembers(3);

        sleep(3000);

        // M4 proposes
        members.get(5).beginProposal("M1");

        // Wait for proposal to complete
        sleep(30000);

        // Stop all members
        stop();

        logger.info("Test 3 completed.");
    }

    /**
     * Test 4: Two proposers scenario with varied delays and responsiveness.
     * 
     * @throws InterruptedException if the thread is interrupted during sleep.
     */
    public static void test4() throws InterruptedException {
        logger.info("Starting test 4: 2 Proposers, delays & responsiveness are varied.");

        startMembers(3);

        sleep(3000);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> members.get(0).beginProposal("M1")); // M4 proposes M1
        executor.submit(() -> members.get(5).beginProposal("M2")); // M6 proposes M2

        // Wait for proposal to complete
        sleep(30000);

        // Stop all members
        stop();
        executor.shutdown();

        logger.info("Test 4 completed.");
    }

    /**
     * Test 5: Three proposers scenario with varied delays and responsiveness.
     * 
     * @throws InterruptedException if the thread is interrupted during sleep.
     */
    public static void test5() throws InterruptedException {
        logger.info("Starting test 5: 3 Proposers, delays & responsiveness are varied.");

        startMembers(3);

        sleep(3000);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.submit(() -> members.get(0).beginProposal("M1")); // M4 proposes M1
        executor.submit(() -> members.get(5).beginProposal("M3")); // M6 proposes M3
        executor.submit(() -> members.get(8).beginProposal("M2")); // M9 proposes M2

        // Wait for proposal to complete
        sleep(60000);

        // Stop all members
        stop();
        executor.shutdown();

        logger.info("Test 5 completed.");
    }
}
