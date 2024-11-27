# Elements
## Election
- **Responsibilities:**
	- Run election test cases. Including:
		1. 1 proposer, no delays & all always responds.
		2. 2 proposers, no delays & all always responds
		3. 1 proposer, with delays & responsiveness varied.
- **Attributes:**
	- `private static final Logger logger` - for printing
- **Methods:**
	- `public static void test1() throws InterruptedException` 
		- 1 proposer, all `delay = 0`, all `responds = true`
	- `public static void test2() throws InterruptedException`
		- 2 proposers, all `delay = 0`, all `responds = true`
	- `public static void test3() throws InterruptedException`
		- 1 proposer, delays, responsiveness varied.
		- **M1**:
			- `int delay = 0` always
			- `boolean responds = true` always
		- **M2**: 
			- If `boolean working == true`, `int delay = 0`, `boolean responds = true`
			- Else if `boolean working == false`, `int delay = 5000`, when receives PREPARE, PROMISE, ACCEPT messages, `boolean responds = new Random().nextBoolean()`.
		- **M3**:
			- If `boolean camping == false`, `delay = (new Random().nextInt(4) * 1) * 1000`, `boolean responds = true`
			- Else if `boolean camping == true`, `responds = false`, so no need to start listener at all.
		- **M4-M9**:
			- `boolean responds = true` always
			- `int delay = (new Random().nextInt(3) + 1) * 1000`
	- `public static void main() throws InterruptedException`
		- Calls `test1()`, `test2()` & `test3()`.

## Paxos
- **Responsibilities:**
	- Contains all PAXOS protocol related methods.
- **Attributes:**
	- `private static final Logger logger` - for printing
	- `private final AtomicIntger proposalCount` - total number of proposals
	- `private int currentHighestProposal` - current highest proposal ID
	- `private int proposalAccepted` - ID of the accepted proposal
		- Initialised to `0`. Any number other than that indicates proposal accepted.
	- `private AtomicReference<String> acceptedCandidate` - value of accepted candidate
	- `private final Map<String, AtomicInteger> acceptedCounts` - map to track accepted counts for each candidate value proposed.
	- `private static final Map<String, Integer> listenerPorts`
		- Map of listening ports for M1-M9.
		- Initialised as `Map.ofEntries()`
		- i.e. M1 port = 5001, M2 port = 5002.
- **Methods:**
	- `private void broadcast(Message message)`
		- send out message to all Members
	- `private void messageMember(String memberId)`
		- Attempts to open a Socket connection to the member with the appropriate port
	- `public void initiateProposal(String candidate)`
		- Begins the Paxos proposal protocol. 
## Member
- **Responsibilities:**
		- Represents a council member.
		- Sends proposal if 
- **Attributes:**
	- `private final Gson gson` - for parsing messages
	- `private static final Logger logger` - for printing
	- `private final String memberId` - the member this belongs to, i.e. "M1"
	- `private final int delay` - delay in milliseconds before responding to messages
	- `private boolean responds` - flag to indicate if should respond to messages
	- `private final ExecutorService executor` - executor for all tasks
	- `private static final Paxos paxos` - object for using all protocol methods
- **Methods:**
	- `public void startProposal(String candidate)`
		- Starts the proposal process
		- calls `void inititateProposal(String candidate)` in Paxos

## Listener
- **Responsibilities:**
- **Attributes:**
	- `private final Gson gson` - for parsing messages
	- `private static final Logger logger` - for printing
- **Methods:**

## Sender
- **Responsibilities:**
- **Attributes:**
	- `private final Gson gson` - for parsing messages
	- `private static final Logger logger` - for printing
- **Methods:**