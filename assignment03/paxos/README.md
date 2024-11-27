___
# Assignment Overview
## Purpose
This project implements a **Paxos voting protocol** to simulate a fault-tolerant election for the Adelaide Suburbs Council president. The goal is to achieve distributed consensus despite challenges such as delayed responses, network failures, and simultaneous proposals.

Nine council members (M1–M9) exhibit varied behaviours:
- **M1**: Always responsive.
- **M2**: Alternates between poor and instant connectivity.
- **M3**: Occasionally unreachable.
- **M4–M9**: Respond with variable delays.

The protocol ensures reliable voting outcomes through socket-based communication, managing failures, and maintaining resilience in distributed systems. Testing validates its correctness across multiple scenarios.

## Tech Stack
- **Development Language & Version:** OpenJDK-22.0.1
- **External Libraries:**
	- Google Java serialisation/de-serialisation for JSON (GSON)
		- Packages: `gson-2.11.0.jar`
	- Simple Logging Facade for Java (SLF4J)
		- Packages: `slf4j-api-2.0.16.jar` & `slf4j-simple-2.0.16.jar`

---
# Setup & Run
## Prerequisites
Before setting up and running the application, ensure the following prerequisites are met:
1. **Java Development Kit (JDK):**
	- Install [OpenJDK 22.0.1](https://openjdk.org/) or a compatible version.
	- Verify installation by running `java --version` and `javac --version` in your terminal.
2. **Build Tool (Make):**
	- Ensure `make` is installed for building and running the project.
	- Verify installation by running `make --version`.
3. **External Libraries:**
	- Ensure the follows JAR files are located in `lib` directory of the project.
		- `gson-2.11.0.jar`
		- `slf4j-api-2.0.16.jar`
		- `slf4j-simple-2.0.16.jar`
4. **Network Configuration:**
	- Ensure that the system allows communication on ports **5001–5009** for socket-based communication.

## Setup
Follow these steps to set up and prepare the project for execution:
1. **Ensure the following directory structure:**
	```
	.
	├── src # Contains all source files 
	├── lib # Contains all external libraries (JAR files)
	├── bin # Contains all compiled classes 
	└── Makefile # Build & run instructions
	```
2. **Check dependencies:**
	- Ensure external library JARs are located in `bin/`
	- Otherwise, download the required JARs and move to `lib/`
3. **Ensure open ports:**
	- Open ports **5001–5009** on your system to allow socket-based communication between nodes.
4. **Build the Project:**
	- Navigate to the project root in your terminal and run the following command:
		```
		make compile
		```
	- This will compile all Java source files in the `src` directory and place the compiled `.class` files in the `bin` directory.
5. **Verify Setup:**
	- Ensure there are no compilation errors. Check the `bin` directory for generated `.class` files.

After completing these steps, proceed to the "Run" section to execute specific test cases.
## Run
To execute the project and observe the results for different scenarios, follow these steps:
1. **Navigate to the Project Root:**
	- Open a terminal and ensure you're in the root directory of the project.
2. **Run Specific Tests:**
	- Use the `make` command followed by the desired test target to execute a specific test case. Each test will log its output into a corresponding `.log` file in the project directory.
	- **Commands for Each Test:**
		- **Test 1:** Run the Paxos protocol with 1 proposer and all members responding instantly.
			```
			make test1
			```
			- Logs output to `Test1.log`.
		- **Test 2:** Run the Paxos protocol with 2 proposers and all members responding instantly.
			```
			make test2
			```
			- Logs output to `Test2.log`.
		- **Test 3:** Run the Paxos protocol with 1 proposer, varied delays, and responsiveness.
			```
			make test3
			```
			- Logs output to `Test3.log`.
		- **Test 4:** Run the Paxos protocol with 2 proposers, varied delays, and responsiveness.
			```
			make test4
			```
			- Logs output to `Test4.log`.
		- **Test 5:** Run the Paxos protocol with 3 proposers, varied delays, and responsiveness.
			```
			make test5
			```
			- Logs output to `Test5.log`.
1. **View Logs:**
	- After executing a test, view its corresponding log file to review the output.
1. **Clean Up (Optional):**
	- Removes all built `.class` files in `bin` directory.
		```
		make clean
		```
---
# Design Overview
## Architecture
The architecture of this implementation follows a modular and distributed design to simulate the Paxos consensus algorithm in a fault-tolerant voting scenario. Key components include:
1. **Members**:
	- Represent individual council members (M1 to M9) in the voting process.
	- Each member acts as both a proposer and an acceptor, depending on the scenario.
	- Members handle message passing, delays, and failure scenarios to mimic real-world conditions like slow responses or intermittent connectivity.
2. **Listener**:
	- A lightweight server component for each member, running on a unique port.
	- Listens for incoming messages and routes them to the corresponding member for processing.
	- Utilises multi-threading to handle concurrent message processing.
3. **Paxos Logic**:
	- Encapsulates the core Paxos protocol logic, including proposal creation, promise handling, and consensus tracking.
	- Manages state variables like current proposals, promises received, and accepted proposals.
	- Implements the distributed consensus logic for handling conflicts and achieving agreement.
4. **Message System**:
	- Messages are JSON-encoded and include essential metadata (e.g., proposal number, sender ID, candidate).
	- Supports five message types: `Prepare`, `Promise`, `Propose`, `Accept`, and `Fail`.
	- Provides robust logging for tracing message flow and debugging.
5. **Election Class**:
	- The main orchestrator that initiates and runs test scenarios.
	- Spawns members, manages their life cycle, and simulates different voting conditions like delays and multiple proposers.
## Workflow
The workflow for this Paxos-based voting system can be broken into the following stages:
1. **Initialisation**:
    - The `Election` class starts by creating and configuring members.
    - Each member spins up a `Listener` on its designated port to receive messages.
2. **Proposal**:
    - A proposer (e.g., M1) initiates the process by broadcasting a `Prepare` message with a unique proposal number to all other members.
    - If multiple proposers exist, they broadcast concurrently, simulating conflicts.
3. **Promise Handling**:
    - Members receiving a `Prepare` message evaluate it based on the proposal number:
        - If the proposal is higher than any previously seen, the member sends a `Promise`.
        - If the proposal is lower, a `Fail` message is sent back.
    - Proposers collect `Promise` responses and determine the candidate with the highest priority.
4. **Proposal Acceptance**:
    - Once a proposer receives a majority of `Promises`, it sends a `Propose` message to all who promised, declaring the selected candidate.
    - Members receiving the `Propose` message either:
        - Accept the proposal if it matches their highest promise.
        - Reject it with a `Fail` message if it conflicts with a newer proposal.
5. **Consensus**:
    - Members receiving `Accept` messages track the count for each candidate.
    - When a majority is reached for a candidate, the system declares consensus, finalising the vote.
6. **Fault Tolerance**:
    - The workflow handles delays, dropped messages, and conflicting proposals gracefully, simulating real-world network and node failures.

This design ensures a robust and clear implementation of the Paxos algorithm, demonstrating its fault-tolerant and consensus-building capabilities in a distributed voting system.

---
# Class Descriptions
## 1. Paxos
The `Paxos` class encapsulates the core logic of the Paxos consensus algorithm. It manages state transitions, message processing, and communication to ensure consensus is achieved among members.
- **Attributes**
	- `private final ExecutorService executor`: Thread pool for managing asynchronous tasks.
	- `private final String memberId`: The unique identifier of the member.
	- `private final Map<String, String> promises`: A map of promises received during a proposal phase.
	- `private final Map<String, AtomicInteger> accepts`: A map of acceptances grouped by candidate.
	- `private final static Map<String, Integer> listenerPorts`: Static mapping of member IDs to ports.
	- `private AtomicInteger currentMaxProposal`: Tracks the highest proposal number seen.
	- `private int currentProposal`: The proposal number for the member’s current proposal.
	- `private String currentCandidate`: The candidate associated with the current proposal.
	- `private int acceptedProposal`: Tracks the proposal number accepted by the member.
	- `private String acceptedCandidate`: Tracks the candidate accepted by the member.
- **Methods**
	- `public Paxos(String memberId)`: Constructor to initialise a Paxos instance.
	- `public void shutdown()`: Shuts down the thread pool.
	- `public void sendTo(String recipient, Message msg)`: Sends a message to a specific recipient.
	- `public void initialiseProposal(String candidate)`: Starts a new proposal and broadcasts a `Prepare` message.
	- `private int getProposalNumber()`: Generates a unique proposal number based on the current system time and member ID.
	- `private void broadcastMessageToAll(Message msg)`: Sends a message to all members except itself.
	- `private void broadcastMessageToPromisors(Message msg)`: Sends a message to members who sent promises.
	- `public synchronized void receivePrepare(Message msg)`: Processes a `Prepare` message, deciding whether to promise or fail.
	- `public synchronized void receivePromise(Message msg)`: Processes a `Promise` message, determining whether a majority has been reached.
	- `public void receivePropose(Message msg)`: Processes a `Propose` message, deciding whether to accept or reject.
	- `public synchronized void receiveAccept(Message msg)`: Tracks acceptances and checks for consensus.
## 2. Election
The `Election` class serves as the entry point and orchestrator for the simulation. It manages member lifecycle, test scenarios, and execution flow.
- **Attributes**
	- `private static final Logger logger`: Logger for logging activities.
    - `private static List<Member> members`: List of all members participating in the election.
- **Methods**
	- `public static void main(String[] args) throws InterruptedException`: Entry point to run the program.
	- `private static void startMembers(int testNumber)`: Configures and starts members based on the test scenario.
	- `private static void stop()`: Stops all members and clears resources.
	- `public static void test1() throws InterruptedException`: Simulates a scenario with one proposer and instant responses.
	- `public static void test2() throws InterruptedException`: Simulates a scenario with two concurrent proposers and instant responses.
	- `public static void test3() throws InterruptedException`: Simulates a scenario with one proposer, varying delays, and intermittent responses.
	- `public static void test4() throws InterruptedException`: Simulates a scenario with two proposers, varying delays, and intermittent responses.
	- `public static void test5() throws InterruptedException`: Simulates a scenario with three proposers, varying delays, and intermittent responses.
## 3. Member
The `Member` class represents a council member participating in the Paxos consensus process. Each member has its own `Paxos` instance and `Listener` to handle messages.
- **Attributes**
	- `private static final Logger logger`: Logger for logging activities.
	- `private static final ExecutorService executor`: Thread pool for handling concurrent tasks.
	- `private final Paxos paxos`: The Paxos instance for this member.
	- `private final String memberId`: The unique identifier for the member.
	- `private final int delay`: The delay in response time for the member.
	- `private boolean working`: Indicates whether the member is actively responsive.
	- `private boolean camping`: Indicates whether the member is unresponsive.
	- `private boolean responding`: Indicates the current responsiveness of the member.
	- `private static final Map<String, Integer> listenerPorts`: Static mapping of member IDs to ports.
- **Methods**
	- `public Member(String memberId, int delay, boolean working, boolean camping)`: Constructor to initialise a member.
    - `public void start()`: Starts the member’s `Listener` to receive messages.
    - `public void beginProposal(String candidate)`: Initiates a new proposal using the `Paxos` instance.
    - `public void receive(Message msg)`: Handles incoming messages and decides whether to respond.
    - `private void processMessage(Message msg)`: Processes the message based on its type (`Prepare`, `Promise`, `Propose`, etc.).
    - `public void stop()`: Shuts down the member and releases resources.
    - `public String getMemberId()`: Returns the member's unique identifier.
## 4. Listener
The `Listener` class handles incoming network communication for a member. It listens on a specific port and processes incoming messages in a separate thread.
- **Attributes**
	- `private static final Logger logger`: Logger for logging activities.
	- `private final Gson gson`: Gson instance for JSON parsing.
	- `private final int port`: The port number the listener operates on.
	- `private final Member member`: The associated `Member` instance that processes the received messages.
	- `private ServerSocket serverSocket`: Socket that listens for incoming connections.
- **Methods**
	- `public Listener(Member member, int port)`: Constructor to initialise the listener with a member and port.
	- `public void start()`: Begins listening for incoming connections and spawns threads for processing them.
	- `private void handleConnection(Socket clientSocket)`: Reads and parses incoming messages, forwarding them to the member.
	- `private Message parseMessage(String message)`: Converts a JSON string into a `Message` object.
## 5. Message
The `Message` class encapsulates the data for communication between members in the Paxos protocol. It uses JSON serialisation for transmission.
- **Attributes**
	- `private final String senderId`: The ID of the member sending the message.
	- `private final String type`: The type of message (e.g., `Prepare`, `Promise`, etc.).
	- `private final int proposalNumber`: The proposal number associated with the message.
	- `private final String candidate`: The candidate associated with the message, if applicable.
- **Sub-Types**
	- **`Prepare`**:
	    - **Attributes**: `proposalNumber (int)`.
	    - Description: Sent by a proposer to initiate the voting process.
	- **`Promise`**:
	    - **Attributes**: `proposalNumber (int)`, `candidate (String)`.
	    - Description: Sent by acceptors to indicate agreement to a proposal.
	- **`Propose`**:
	    - **Attributes**: `proposalNumber (int)`, `candidate (String)`.
	    - Description: Sent by a proposer to declare a candidate after receiving a majority of promises.
	- **`Accept`**:
	    - **Attributes**: `proposalNumber (int)`, `candidate (String)`.
	    - Description: Sent by acceptors to indicate agreement to a specific proposal.
	- **`Fail`**:
	    - **Attributes**: `proposalNumber (int)`.
	    - Description: Sent by acceptors to indicate rejection of a proposal.

---
# Testing
The **Testing** section outlines the strategy, tools, and methodologies used to validate the Paxos implementation. It also summarises the results for each test scenario defined in the assignment.

## Testing Objectives
- Verify correct implementation of the Paxos consensus algorithm under various scenarios.
- Test fault tolerance by simulating different delays, responsiveness levels, and concurrent proposals.
- Ensure the program meets the criteria specified in the assignment brief.
## Test Cases
Each test case simulates a specific scenario involving different member behaviours, response times, and proposer configurations. Logs generated during these tests serve as evidence of the system’s functionality.

- **Test 1: Single Proposer, Instant Responses**
	- **Proposer**: Member M6 proposes M1 as the council president.
	- **Scenario**: All members (M1–M9) respond instantly to voting queries.
	- **Expected Outcome**: M1 receives a majority vote (5+ responses) and is elected as the council president.
	- **Log File**: `Test1.log`

-  **Test 2: Two Proposers, Instant Responses**
	- **Proposers**:
	    - Member M1 proposes themselves (M1) as the council president.
	    - Member M6 proposes M2 as the council president.
	- **Scenario**: Two members broadcast proposals simultaneously, and all members respond instantly.
	- **Expected Outcome**:
	    - One of the proposals achieves a majority (5+ votes) through the Paxos protocol.
	    - Either M1 or M2 becomes the council president.
	- **Log File**: `Test2.log`

-  **Test 3: Single Proposer, Varied Delays**
	- **Proposer**: Member M6 proposes M1 as the council president.
	- **Scenario**:
	    - Response times are varied:
	        - M1 responds instantly.
	        - M2 occasionally delays or does not respond.
	        - M3 intermittently goes offline.
	        - Members M4–M9 respond with random delays (1–3 seconds).
	- **Expected Outcome**: Despite varied delays, M1 secures the majority (5+ votes) and is elected as the council president.
	- **Log File**: `Test3.log`

 - **Test 4: Two Proposers, Varied Delays**
	- **Proposers**:
	    - Member M1 proposes themselves (M1) as the council president.
	    - Member M6 proposes M2 as the council president.
	- **Scenario**:
	    - Two members broadcast proposals under conditions with varied delays and occasional failures:
	        - M1 responds instantly.
	        - M2 occasionally delays or does not respond.
	        - M3 intermittently goes offline.
	        - Members M4–M9 respond with random delays (1–3 seconds).
	- **Expected Outcome**:
	    - The system resolves conflicts using the Paxos protocol.
	    - Either M1 or M2 achieves consensus (majority votes) and becomes the council president.
	- **Log File**: `Test4.log`

- **Test 5: Three Proposers, Varied Delays**
	- **Proposers**:
	    - Member M1 proposes themselves (M1) as the council president.
	    - Member M6 proposes M3 as the council president.
	    - Member M9 proposes M2 as the council president.
	- **Scenario**:
	    - Three members broadcast proposals under conditions with varied delays and occasional failures:
	        - M1 responds instantly.
	        - M2 occasionally delays or does not respond.
	        - M3 intermittently goes offline.
	        - Members M4–M9 respond with random delays (1–3 seconds).
	- **Expected Outcome**:
	    - The system resolves conflicts among the three proposals using the Paxos protocol.
	    - One of M1, M2, or M3 achieves consensus (majority votes) and becomes the council president.
	- **Log File**: `Test5.log`