import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Test3 {
    // Helper method to initialise and start nodes based on their behaviour profiles
    // Inputs: The number of nodes (council members) and a map to their behaviour definitions
    // Output: A list of Paxos nodes
    private static List<PaxosNode> startNodes(int num_nodes, Map<Integer, Consumer<PaxosNode>> behaviours) {
        List<PaxosNode> nodes = new ArrayList<>();
        for (int i = 1; i <= num_nodes; i++) {
            PaxosNode node = new PaxosNode(i);

            // If no behaviour defined for this node, assign default (online, no response delay), else apply behaviour
            behaviours.getOrDefault(i, (n) -> n.setProfile(true, 0)).accept(node);
            node.start();
            nodes.add(node);
        }

        return nodes;
    }

    // Helper method to stop nodes
    // Input: The list of Paxos nodes
    // Output: None
    private static void stopNodes(List<PaxosNode> nodes) {
        nodes.forEach(PaxosNode::stop);
    }

    // Test 3: Varying Responsiveness & Offline Nodes
    public static void testBehaviours() throws InterruptedException {
        System.out.println("---Test 3: Different Response Profiles for All Members---");

        // Define profiles
        Map<Integer, Consumer<PaxosNode>> behaviours = new HashMap<>();

        // M1 is always online and responds instantly
        behaviours.put(1, (node) -> node.setProfile(true, 0));

        // M2 is always online but with varying delays, and sometimes ignores messages
        behaviours.put(2, (node) -> {
            Random rand = new Random();
            int delay = rand.nextInt(2) == 0 ? 0 : 5000; // 50% chance of either responding instantly or 5s delay
            boolean responds = rand.nextBoolean(); // If true, M2 will respond to all messages, else ignores most messages unless additional conditions met 
            node.setProfile(true, delay);
            node.setMessageFilter(message -> responds || rand.nextInt(4) == 0); // Sets a filter: if 'responds' is false, M2 will only respond to ~25% of the time
        });

        // M3 is sometimes online and has less delayed responses than M2
        behaviours.put(3, (node) -> {
            Random rand = new Random();
            boolean online = rand.nextBoolean(); // 50% chance to be online
            int delay = rand.nextInt(4000) + 1000; // 1-4s delay
            node.setProfile(online, delay);
        });

        // M4-M9 are always online but also have varying response times (1-3s delay)
        for (int i = 4; i <= 9; i++) {
            behaviours.put(i, (node) -> {
                Random rand = new Random();
                int delay = rand.nextInt(3000);
                node.setProfile(true, delay);
            });
        }

        // Start nodes with behaviour profiles
        List<PaxosNode> nodes = startNodes(9, behaviours);

        // M7 initiates a proposal for candidate M1
        nodes.get(6).startProposal("M1");

        // Wait for consensus
        Thread.sleep(10000);

        // Stop nodes
        stopNodes(nodes);

        System.out.println("---Test 3: Different Behaviours Completed.---");
    }

    public static void main(String[] args) throws InterruptedException {
        testBehaviours();
    }
}