import java.util.*;
import java.util.concurrent.*;

public class Test2 {
    // Helper method to start nodes
    // Input: Number of nodes (council members)
    // Output: A list of Paxos nodes
    private static List<PaxosNode> startNodes(int num_nodes) {
        List<PaxosNode> nodes = new ArrayList<>();
        for (int i = 1; i <= num_nodes; i++) {
            PaxosNode node = new PaxosNode(i);
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

    // Test 2: Concurrent Proposals
    public static void testConcurrentProposals() throws InterruptedException {
        System.out.println("---Test 2: 2 Councillors Send Voting Proposals at the Same Time---");

        // Start nodes
        List<PaxosNode> nodes = startNodes(9);

        // M5 and M6 initiate proposals concurrently
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> nodes.get(4).startProposal("M2")); // M5 proposes M2
        executor.submit(() -> nodes.get(5).startProposal("M3")); // M6 proposes M3

        // Wait for consensus
        Thread.sleep(2000);

        // Stop nodes
        stopNodes(nodes);
        executor.shutdown();

        System.out.println("---Test 2: Concurrent Proposals Completed.---");
    }

    public static void main(String[] args) throws InterruptedException {
        testConcurrentProposals();
    }
}