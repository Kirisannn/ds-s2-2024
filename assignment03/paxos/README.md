# Distributed Systems Assignment04 - PAXOS Council Election
The assignment asks for the implementation of PAXOS decision-making algorithm to facilitate a Council Election for president.

The PAXOS algorithm will be implemented as described in the following sections.

# Phases
In general, the algorithm consists of two phases, "PREPARE-PROMISE" and "PROPOSE-ACCEPT".
## Phase 1: PREPARE-PROMISE
A proposer receives a consensus request for a **VALUE** from a client. It creates a unique proposal number **`ID`**, and sends a **PREPARE(ID)** message to at least a majority of acceptors.

Each acceptor that receives the **PREPARE** message looks at the ID in the message and decides:
1. Is this ID bigger than any round I have previously received?
2. If **yes**:
	- Store **ID number**: `max_id = ID`
	- Respond with **"promise"** message.
3. If **no**:
	- Do not respond **or** respond with **"fail"** message.
## Phase 2: PROPOSE-ACCEPT

# Edgy Behaviours
1. When a proposer ***i*** has sent a *prepare message* to acceptor ***j***, if ***j*** has *accepted (phase 2)* a higher proposal number from ***k***, ***j*** will *promise* ***i*** with the higher proposal from ***k*** back to ***i***. Then, ***i*** will *propose* to ***j*** with the new *higher proposal*. Proceed as per normal.
2. When a proposer ***i*** has sent a *prepare message* to acceptor ***j***, if ***j*** has *promised (phase 1)* a higher proposal number from ***k***, ***j*** will send a *fail message* to ***i***. If ***i*** does not receive *promise* from majority members, it **retries** *prepare* to all members with *new higher proposal ID*.
3. **M1** always responds on first attempt (never fails), responds to EVERY message from every other member.
4. **M2**: If M2 not at work (cafe), always takes 5 seconds to respond to any message, it always responds to first message, thereafter any further proposals, randomly (true/false) decide to promise/accept the new proposals. If M2 AT work, follow M1 behaviours.
5. **M3**: Responds afters 3 seconds to any message, ONLY AFTER if randomly decided to respond to message.
6. **M4-M9**: Randomly selects 0-5 seconds delay the first time the councillor was created. Responds for this decided delay for all messages in the current election. Proposal value is randomly selected the first time the councillor created. selects between **M1-M9**
7. **M1-M3** will always propose themselves for their first prepare.
8. Each councillor should have a designated port for **Acceptor** & **Proposer**. I.e. **M1** gets `propose_port=5000` & `accept_port=5001`.

# Elements
*Note: `ctrl+f`  "uncomment" and remove change code back to random delays and cafe/camping.*

## Message - Done
- Contains 5 different child message classes.
	1. **Prepare**
	2. **Promise**
	3. **Propose**
	4. **Accept**
	5. **Fail**
	6. **Result**
	7. **Stop**

## Member - In Progress
- Contains a `proposer` & a `acceptor`
- **Steps:**
	1. Set delay
	2. Decide if working or camping
	3. Decide if to propose
	4. Create `proposer` & `acceptor`
	5. Create and start `proposer` & `acceptor` threads
	6. Stop early if majority consensus reached.
	7. Send elected president to `Election`
	
- **Methods:**
	- **`void`** `setDelay()`
	- **`void`** `toPropose()`
	- **`void`** `setWorkingCamping`
	- **`void`** `createProposer()`
		- Only done if `proposing = true`
	- **`void`** `createAcceptor()`
	- **`void`** `startThreads()`
	- **`void`** `stop()`
	
- **Attributes:**
	- **`String`** `member_id`
	- **`Proposer`** `proposer`
	- **`Acceptor`** `acceptor`
	- **`Random`** `rand = new Random()`
	- **`boolean`** `proposing`
	- **`boolean`** `working = false`
	- **`boolean`** `camping = false`
	- **`int`**`delay`
	- **`Thread`** `proposeThread`
	- **`Thread`** `acceptThread`
	
- **Pseudocode:**
	1. Check which member it is.
		- If M1, `delay = 0`
		- If M2, randomly select if at home or at cafe (working) 
			- `working = rand.nextBoolean()`
			- If at home, `delay = 5000`
			- If at cafe (working), `delay = 0`
		- If M3, randomly select if at home or camping
			- `camping = rand.nextBoolean()`
			- If at home, `delay = 3000`
			- If at camping, `delay = 0`
		- If M4-M9, `delay = rand.nextInt(6) * 1000`, can be instant
	2. Decide if member will be proposing
		- If proposing, create `proposer`
	3. Create `acceptor`
	4. Create `proposerThread` & `acceptorThread`, then start them

## Proposer - In Progress
- **Attributes:**
	- **`static final AtomicInteger`** `proposalCount = new AtomicInteger(0)`
	- **`int`** `currentProposal`
	- **`AtomicIntger`** `acceptedCount`
	- **`String`** `currentCandidate`
	- **`List<int>`** `acceptorPorts`
		- List of all the acceptor ports for each member.
	
- **Methods:**
	- **`void`** `run()`
		- The main method for the class.
		- While consensus not reached
			- Reset accepted count to 0
			- Gets a new proposal number.
			- Gets a new candidate if not yet assigned
			- Sends a prepare message
			- Receives response (FAIL/PROMISE)
			- Sends a propose message
			- Receives respond (FAIL/ACCEPT)
			-  Break out of while loop when consensus reached, `acceptedCount = 5`
		- Call `consensusReached()`
	- **`int`** `getProposalNumber()`
	- **`int`** `getCandidate()`
		- Returns the `currentCandidate`.
	- **`void`** `prepare()`
		- Sends a `prepare` message to a member
	- **`void`** `handlePrepare()`
	- **`void`** `propose()`
	- **`void`** `handleAccept()`
	- **`void`** `consensusReached()` 
		- Once majority consensus reached
			- Sends kill messages to all other members' `acceptor`.
			- Send to `Election` the decided president. 
			- Sends kill message to its own `acceptor`.
## Acceptor - In Progress
- **Attributes:**
	- **`AtomicInteger`** `maxProposalId`
	- **`String`** `acceptedCandidate`
	- **`boolean`** `proposalAccepted`
	
- **Methods:**
	- **`void`** `run()`
		- Main class method
		- While true
			- Accept incoming connections
			- If `proposalId > maxProposalId`, `maxProposalId = proposalId` and respond PROMISE(`maxProposalId`)
			- Else if `proposalId <= maxProposalId`,
				- If `proposalAccepted == true`, respond PROMISE(`maxProposalId`, `acceptedCandidate`)
				- else if `proposalAccepted == false`, respond FAIL
	- 

## Election - In Progress
- **Attributes:**
	- **`String`** `president = null`
	- **`List<Member>`** `members` 
	- **`List<Thread>`** `memberThreads`
	
- **Methods:**
	- **`void`** `createMembers()`
		- Create all the `Member` objects
		- Saves them to `members` 
	- **`void`** `createMemberThreads()`
		- Create member threads, save them to `memberThreads`
	- **`void`** `startMembers()`
		- Starts all the member threads.
	- **`void`** `stopMember(String memberId)`
		- Stops a member's thread, which stops the member's `proposer` and `acceptor`
	- **`void`** `electPresident()`
		- Thread that listens to incoming connections
		- Receives the message containing the final voted president
		- Sets `president` to the new voted president