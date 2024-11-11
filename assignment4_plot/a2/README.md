
# Aggregation Server System

This project implements an aggregation server with a fault-tolerant system designed to handle weather data from multiple content servers, support GET requests from multiple clients, and manage concurrent requests with Lamport clock synchronization. The server can also handle failure modes such as client/server crashes, data expiration, and network issues.

## Project Overview

This system consists of:
1. **AggregationServer**: Manages concurrent PUT and GET requests, stores data, and removes expired data.
2. **ContentServer**: Sends PUT requests with weather data to the AggregationServer.
3. **GETClient**: Sends GET requests to retrieve weather data from the AggregationServer.

Key Features:
- **Lamport Clocks** for synchronization and ordering of requests.
- **Fault Tolerance** with retries and data expiration management.
- **Concurrency Support** allowing multiple PUT and GET requests from various clients and servers.

## Prerequisites

To run this project, you need:
- **Java 8+**
- **JUnit 5** (for testing)
- **json-20210307.jar** (JSON library)

Add `json-20210307.jar` to your classpath:
export CLASSPATH=json-20210307.jar:.
```

## Files in This Project

### Code Files
- **AggregationServer.java**: The main server handling PUT and GET requests, managing concurrency, and data expiration.
- **ContentServer.java**: A content server that sends weather data updates via PUT requests.
- **GETClient.java**: A client that requests weather data from the server via GET requests.
- **ClientRequest.java**: Handles individual client requests and manages Lamport clock synchronization.

### Testing Files
- **AggregationSystemTests.java**: A suite of automated JUnit tests to validate the functionality, concurrency, and fault tolerance of the system.

### Data Files
- **weather_data.txt**: Weather data for Adelaide.
- **weather_data2.txt**: Additional weather data (sample with missing ID).
- **weather_data3.txt**: Weather data for Brisbane and Melbourne.

## How to Run

1. **Compile All Files**:
   First terminal (Makefile: make compile)
   javac *.java
   ```

2. **Start AggregationServer**:
   First terminal (Makefile: make start)
   Run the AggregationServer on a designated port (e.g., `5050`):
   java AggregationServer 5050
   ```

3. **Start ContentServer**:
   Second terminal (Makefile: make run_contentserver)
   Run one or more ContentServers to send PUT requests:
   java ContentServer localhost:5050 weather_data.txt
   java ContentServer localhost:5050 weather_data2.txt
   java ContentServer localhost:5050 weather_data3.txt
   ```

4. **Start GETClient**:
   Third terminal (Makefile: make run_client)
   Run one or more GETClients to request data from the AggregationServer:
   java GETClient localhost:5050
   ```

### Test Cases in `AggregationSystemTests.java`
The suite includes:
1. **Single PUT and GET Requests**: Verifies individual PUT and GET operations.
2. **Concurrent PUT and GET Requests**: Ensures consistency and order in concurrent requests.
3. **Lamport Clock Ordering**: Confirms ordering with Lamport clock synchronization.
4. **Data Expiration**: Checks data expiration after 30 seconds.
5. **Retry Mechanisms**: Tests client retry attempts on network or server failure.

## Expected Outputs

### Output for `ContentServer`
Upon sending data, a `ContentServer` instance will log messages similar to:
```plaintext
PUT request sent to AggregationServer for data: Adelaide (West Terrace / ngayirdapira)
Server response: status 200 OK - Updates Successfully
```

### Output for `GETClient`
A `GETClient` instance will receive JSON-formatted weather data:
```plaintext
{
  "id": "IDS60901",
  "name": "Adelaide (West Terrace / ngayirdapira)",
  "state": "SA",
  "time_zone": "CST",
  "air_temp": 13.3,
  ...
}
```

### AggregationServer Logs
The AggregationServer logs PUT and GET requests, Lamport clock updates, and expired data removal:
```plaintext
Server started successfully on port 5050
Received PUT request from ContentServer. Updated data for ID: IDS60901
Lamport Clock updated to 5
Removing expired data for ID: IDS10000
```

## Code Explanation

### AggregationServer.java

The AggregationServer is the core of this system:
- **PUT Request Handling**: Receives data from `ContentServer` instances, checks Lamport clock values, and updates weather data.
- **GET Request Handling**: Serves weather data to `GETClient` instances.
- **Lamport Clock**: Ensures requests are serialized and ordered correctly.
- **Data Expiration**: Removes entries not updated within the last 30 seconds, improving memory efficiency.

Key Methods:
- `startServer()`: Sets up server socket, accepts incoming connections, and starts threads to handle requests.
- `handlingPutRequest()`: Processes data from `ContentServer`, validates JSON structure, and updates data.
- `handlingGetRequest()`: Retrieves and sends requested data to `GETClient`.
- `RemoveOutdatedData()`: Periodically checks for and removes expired data.

### ContentServer.java

The `ContentServer` class simulates a weather data provider:
- **Lamport Clock Handling**: Updates the Lamport clock with each request.
- **PUT Request Construction**: Formats data into a JSON object and sends it to the `AggregationServer`.

Key Method:
- `sendPutRequest()`: Reads data from input files, formats it, and sends it to the AggregationServer as a PUT request.

### GETClient.java

The `GETClient` class requests aggregated data from the AggregationServer:
- **GET Request Handling**: Constructs GET requests, sends them to the AggregationServer, and receives JSON-formatted responses.
- **Error Handling**: Retries connections if the server is unavailable.

Key Method:
- `fetchData()`: Sends a GET request and parses the JSON response for specific weather data.

### ClientRequest.java

Handles individual client requests on the server side:
- **Lamport Clock Management**: Updates and synchronizes the Lamport clock for request ordering.
- **Request Processing**: Manages PUT and GET request formats, header processing, and response handling.

---