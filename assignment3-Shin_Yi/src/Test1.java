import java.util.*;
import java.util.concurrent.*;

public class Test1 {
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

    // Test 1: Immediate Responses
    public static void testImmediateResponses() throws InterruptedException {
        System.out.println("---Test 1: M1 to M9 Have Immediate Responses to Voting Queries---");

        // Start nodes
        List<PaxosNode> nodes = startNodes(9);

        // M4 initiates a proposal for candidate M1
        nodes.get(3).startProposal("M1");

        // Wait for consensus
        Thread.sleep(2000);

        // Stop nodes
        stopNodes(nodes);

        System.out.println("---Test 1: Immediate Responses Completed.---");
        System.exit(0);
    }

    public static void main(String[] args) throws InterruptedException {
        testImmediateResponses();
    }
}