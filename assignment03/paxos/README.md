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
3. **View Logs:**
4. **Clean Up (Optional):**

---
# Design Overview
## Architecture


## Workflow

---
# Class Descriptions
## 1. Paxos
### Purpose


### Attributes


### Methods


## 2. Election
### Purpose


### Attributes


### Methods


## 3. Member
### Purpose


### Attributes


### Methods


## 4. Listener
### Purpose


### Attributes


### Methods


## 5. Message
### Purpose


### Attributes


### Methods

---
# Testing
## Case 1: Single Proposer, Instant Responses
- **Objective**
- **Setup**
- **Expected Outcome**

## Case 2: Double Proposers, Instant Responses
- **Objective**
- **Setup**
- **Expected Outcome**

## Case 3: Single Proposer, Varied delays & Responsiveness
- **Objective**
- **Setup**
- **Expected Outcome**

## Case 4: Double Proposers, Varied delays & Responsiveness
- **Objective**
- **Setup**
- **Expected Outcome**

## Case 5: Triple Proposers, Varied delays & Responsiveness
- **Objective**
- **Setup**
- **Expected Outcome**