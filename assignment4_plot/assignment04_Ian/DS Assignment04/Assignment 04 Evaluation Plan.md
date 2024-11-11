
## How do different implementations deal with failures? 
Evaluate and report on different types of failures that can appear in the system and how they are handled by each implementation.

### Failures
1. **Aggregator** is persistent in storing weather info
2. **Aggregator** removes weather data only if
	1. Content Server loses connection
	2. Weather data is too old
		- not one of most 20 recent updates
		- last connection known >30 sec
3. **Client** correctly sends GET request
	- Headers include lamport time
	- Headers formatted correctly.
4. **Client** displays weather data after response
5. **Content Server** sends **PUT** request correctly
	1. **Formatted correctly**
		- Headers correct format (incl. station ID, lamport Time, Application Type)
		- Body is valid JSON
6. **Aggregator** replaces **old data entry** with **new entry** from **PUT** request
7. **Content Server** reads content from file, then assembles into valid **JSON**
8. Aggregator able to restore last known state on crash
9. Aggregator handles invalid content format
10. Aggregator handles empty content
11. Lamport clock times are updated correctly

## Report on the scalability of each system, both for content servers and clients.

Present a graph of all three implementations handling X requests concurrently or in a short time. A test script will send the requests in a short period of time. 

### What are the trade-offs in each implementation?

1. Choosing to send PUT body as pre or post formatted JSON
	- Shin Yi: Processed at Aggregator
		- Reason:
			- To keep content server simple, only need to read file, and send request.
			- Logic for validating data is handled only by aggregator ONLY.
	- Kiylie & Ian: Processed in Content Server and sent as JSON Array to Aggregator
		- Reason:
			- So that Aggregator can handle the request as soon as possible 
			- (Ian) Two points of validating content, before sending in Content Server, and another at Aggregator when receiving. This trade-off of speed for security of content.
3. Using repeated connections instead of persistent ContentServer-Aggregator connection
		
### **1. Repeated Connection (Open/Close Every 30 Seconds)**

This approach involves the `ContentServer` creating a new socket connection to the `AggregationServer` every 30 seconds, sending data, and then closing the connection once the data has been sent and acknowledged.

#### **Advantages:**
   - **Resource Efficiency in Low-Traffic Scenarios**: By closing the connection after each interaction, this approach frees up system resources (e.g., threads, file descriptors) on both the `ContentServer` and `AggregationServer`, which is helpful if the server or client is handling numerous other tasks or connections intermittently.
   - **Fault Isolation and Recovery**: Closing and reopening the connection can reduce the impact of transient network issues. If there’s a problem (e.g., dropped packets, temporary network congestion), the next connection attempt effectively resets the connection state, potentially recovering from issues without requiring a manual restart.
   - **Reduced Idle Resource Usage**: In a system where interactions are infrequent or data transmission isn’t constant, repeated connections ensure that resources aren’t unnecessarily tied up for long periods while waiting for the next communication.

#### **Drawbacks:**
   - **Connection Overhead**: Establishing a new connection each time introduces additional overhead, as TCP handshakes are required with each connection. This could lead to higher latency, especially if the interactions are frequent.
   - **Increased Network Traffic**: Repeatedly opening and closing connections can create extra network traffic and consume bandwidth, particularly if the 30-second interval is short. This is less efficient than a persistent connection for high-frequency communications.
   - **Higher Risk of Connection Refusals Under Load**: If the system reaches the maximum number of sockets quickly (due to repeated open/close operations), there’s a risk of connection refusals, which can result in data transmission failures.

---
### **2. Persistent Connection (Keeping the Socket Open)**

With a persistent connection, the `ContentServer` establishes a single socket connection to the `AggregationServer`, keeping it open for the duration of the server's operation or until explicitly closed.

#### **Advantages:**
   - **Reduced Connection Overhead**: By maintaining a continuous connection, this approach eliminates the need for repeated handshakes, reducing latency for each data transmission. This is particularly beneficial if the `ContentServer` sends updates frequently.
   - **Lower Network Load and Bandwidth Usage**: Persistent connections can reduce network congestion, as fewer packets are required for connection setup and teardown, optimizing data flow between the `ContentServer` and `AggregationServer`.
   - **Improved Consistency and Reliability**: A stable, persistent connection can provide more consistent communication patterns, which can simplify synchronization and error handling (e.g., maintaining session state or continuous streams of data).
   - **Efficient Resource Usage in High-Traffic Scenarios**: Persistent connections are resource-efficient in cases of high-frequency communication because the resources required to maintain the socket are amortized over multiple transmissions, rather than being created and destroyed repeatedly.

#### **Drawbacks:**
   - **Potential Resource Leaks on Idle Connections**: Persistent connections consume system resources (e.g., sockets, threads) continuously, which may lead to resource constraints if many persistent connections remain idle. This could require implementing a keep-alive policy or timeout mechanism.
   - **Handling Disconnections and Faults**: If the persistent connection is interrupted (e.g., due to network issues or server restarts), the system must handle reconnection and recovery. Implementing reliable reconnection logic adds complexity and requires additional error-handling mechanisms.
   - **Higher Resource Usage Under Low Traffic**: For low-frequency communication, keeping a connection open may not be resource-efficient, as the connection will consume resources continuously even when no data is being transmitted.

---
### **Choosing Between Repeated vs. Persistent Connections**

We determined the following attributes about the assignment scope.

1. **Frequency of Data Exchange**: Low
2. **Network Stability**: High
3. **System Resource Availability**: High
4. **Latency Sensitivity**: Low

Considering these attributes in conjunction with the pros & cons of each connection methodology, we determined that the best way was to have a repeated connection from each Content Server instance.


## Does the implementation use Lamport clocks and allows for partial ordering of events or total ordering?
How is total ordering achieved? Compare on the methods used and report pros and cons.

- Total: 
	- Shin Yi & Ian:
		- Every request, `GET` or `PUT` is placed into a queue for processing.
		- Shin Yi:
			- A separate execution thread in the main function of Aggregator continuously polls queue and processes each request.
			- Each time request is put in queue, that request Lamport-Time is stored together.
		- Ian:
			- Every request thread, calls a `processQueue` method to poll, until empty.
			- The thread of the first request in a queue is usually the thread which executes the updates.
			- Subsequent request threads will call the `processQueue` method but does essentially nothing as the queue should be empty. Unless new requests are placed in queue since the previous method call.
			- The request lamport clock at the time of the response (after processing the queue - may not be all requests) will be sent back with the response body.
- Partial: 
	- Kylie:
		- If PUT, store in queue, has ordering
		- If GET, immediately returns current state of data.
		- Together they are partially ordered.