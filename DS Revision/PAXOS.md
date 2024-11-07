# Basics

- Defines **three roles**:
	1. ***Proposers***
	2. ***Acceptors***
	3. ***Learners***
- Nodes ***can*** have multiple roles, even all.
- Nodes ***must*** know how many ***acceptors*** a majority is.
	- *Two majorities will always overlap in **at least one node**.*
- Nodes ***must*** be persistent.
	- ***Cannot** forget what has been accepted.*
- A PAXOS run aims to reach ***a single consensus***.
	- *Once a **consensus** has been reached, it **cannot progress** onto another consensus.*
- 