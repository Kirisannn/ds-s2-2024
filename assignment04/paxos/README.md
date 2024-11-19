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