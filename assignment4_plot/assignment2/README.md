# Distributed Systems - Assignment 2 (Shin Yi Goh a1847337)

## Aggregation Server
### 1. Server Initialization ('main' method):
- The server starts by restoring any previously saved weather data from WeatherData.txt.
- Two threads are launched:
    - One to periodically remove old data (based on last communication time).
    - Another to process requests from the request_queue.
- The server listens on the default or user-specified port for incoming client or content server connections, and each connection is handled in a separate thread.

### 2. Restoring Weather Data ('restoreWeatherData' method):
- This method reads weather data from a file (WeatherData.txt) on server startup and populates weather_updates and last_communication. It ensures that data from content servers that communicated earlier is restored.

### 3. Removing Expired Weather Data ('removeOldData' method):
- This method continuously checks for weather data from content servers that haven't communicated in the last 30 seconds. If data is expired, it's removed from weather_updates and last_communication.

### 4. Handling Client/Content Server Connections ('clientHandler' method):
- Each new client connection is handled in a separate thread.
- It reads the client's request, including the Lamport clock value and any weather data.
- Synchronizes the server's Lamport clock with the client's and adds the request to the request_queue for further processing.

### 5. Processing Requests ('processRequests' method):
- This method retrieves and processes requests from the request_queue in Lamport clock order. It ensures that requests are handled sequentially and not out of order.

### 6. Getting Weather Data ('getWeatherData' method):
- This method allows a client to request weather data for a specific station (by station ID). If no ID is provided, it returns data from the most recently communicated content server.

### 7. Updating Weather Data ('updateWeatherData' method):
- When a content server sends new weather data, this method updates the weather_updates and last_communication maps.
- If the server is storing data for more than 20 stations, the data from the station that hasn't communicated for the longest time is removed to enforce the size limit.

### 8. Writing Weather Data to File ('writeWeatherData' method):
- This method writes all current weather data to WeatherData.txt. It first writes the data to a temporary file and then moves it to the main file to ensure atomic updates and prevent data corruption.

### 9. Concurrency Management
- ReentrantLock: Used to ensure thread safety when modifying shared resources such as weather_updates, last_communication, and the weather data file.
- PriorityQueue: Ensures requests are processed in the correct Lamport clock order to maintain consistency across the distributed system.

### 10. Synchronization via Lamport Clocks:
- The Lamport clock is incremented before sending and processing requests to maintain the correct order of events.
- The server synchronizes its clock with the client's clock, ensuring that operations happen in a logically consistent order.

## Content Server
### 1. Socket Connection Setup
- A TCP connection is established to the server using the Socket class, with the server's name and port number.
- A DataOutputStream is used to send data to the server, and a BufferedReader is used to read the server’s response.
- The socket timeout is set to 5000 milliseconds.

### 2. Sending PUT Request
- The method sendPutRequest reads the local weather data from the specified file, converts it to a JSON format, and sends it to the server.
- Lamport Clock: Before sending the PUT request, the content server increments its own Lamport clock to signify that an event (sending a request) is happening.
- Request Headers: The HTTP PUT request includes standard headers (such as User-Agent, Content-Type, and Content-Length) as well as a Lamport-Clock header, which sends the current clock value to the server.
- The weather data is sent in the body of the request in JSON format.

### 3. Retry Mechanism
- The server attempts to send the request up to 3 times if an error occurs.
- Between retries, it waits for 2 seconds (Thread.sleep(2000)).
- If the server response is a success (200 OK or 201 CREATED), the content server retrieves the server’s Lamport clock from the response, compares it with its own clock, and updates its clock to ensure it's synchronized with the server’s clock.

### 4. Reading Server Response
- The server response is read line-by-line and stored. The first line contains the HTTP status code, and the second contains the updated Lamport clock value.
- If the PUT request is successful, the content server’s clock is updated to be the maximum of its own clock and the server’s clock, then incremented by 1 to represent the receipt of the response.

### 5. Weather Data Parsing
- The 'readFromFile; method reads the weather data file and parses each line into key-value pairs.
- It stores these key-value pairs into a JSON object, converting values to int or double where appropriate, and leaves them as strings otherwise.
- The method also ensures that the weather data contains an id field, which is critical for identifying the weather station. If the data is incorrectly formatted, it prints an error.

## GET Client
### 1. Socket Connection Setup
- A TCP connection to the aggregation server is established using Socket.
- The DataOutputStream is used to send the GET request, and BufferedReader is used to read the server’s response.
- A 5-second timeout is set for the socket, so it won’t wait indefinitely for the server to respond.

### 2. Sending the GET Request
- The 'sendGetRequest' method sends the actual HTTP GET request to the aggregation server:
    - Lamport Clock: Before sending the request, the client's Lamport clock is incremented to indicate a new event.
    - GET Request Headers:
        - If a station_id is provided, it is included in the request as a custom header (Station-ID).
        - The current value of the Lamport clock is sent in the Lamport-Clock header.
    - After sending the request, the client waits for the server’s response.
    - The request is retried up to 3 times if any errors occur.

### 3. Receiving the Server Response
- The response from the server is read line by line and stored in a StringBuilder.
- The first line contains the HTTP status code (200 OK).
- If the server returns an empty JSON object ({}), the client reports that no data is available.
- If the response is successful, the client updates its own Lamport clock based on the server's Lamport clock, ensuring that events are synchronized. The clock is updated to the maximum of the client and server clock values plus 1.
- The weather data (in JSON format) is extracted from the response, and the displayWeatherData method is called to display it in a readable format.

### 4. Displaying Weather Data
- The 'displayWeatherData' method parses the weather data from the server response.
- It iterates over the key-value pairs in the JSON object, printing each piece of weather data to the terminal.

### 5. Retry Mechanism
- If the GET request fails (e.g., due to a network issue or an invalid response), the client retries the request up to 3 times before giving up.
- If all retries fail, the client reports that the maximum retry attempts have been reached.

## To compile the Java code, run the following command in a terminal:
```
javac -cp ".:lib/*" *.java
```

## To start Aggregation Server, run the following command in the same terminal:
```
java -cp .:lib/* AggregationServer [optional_port_number]
```

## To run Content Server, run the following command in a new terminal:
```
java -cp .:lib/* ContentServer <server_address>:<port_number> <file_path>
```

## To run GET Client, run the following command in the same terminal as when running Content Server:
```
java -cp .:lib/* GETClient <server_address>:<port_number> [optional_station_id]
```

## To clean class files:
```
rm *.class
```

# Step-by-Step Testing via Terminal
## 1. Start the AggregationServer
Open a terminal and start the AggregationServer:
```
java -cp .:lib/* AggregationServer 1234
```

Omit the port number to use the default port (4567)

## 2. Process a Basic PUT Request & Update Weather Data (Assuming no WeatherData.txt file has been created)
### Expected Result: The server should update its weather data with the content sent by the ContentServer
In another terminal, start the ContentServer with a weather data file (e.g. test_files/data.txt):
```
java -cp .:lib/* ContentServer localhost:1234 test_files/data.txt
```
### Verify: 
- The AggregationServer logs indicate that it has connected to the client
- The ContentServer logs indicate that the PUT request was successful with 201 CREATED, with its updated lamport clock to be 3
- The WeatherData.txt file is created and now contains the data in JSON string format

## 3. Handle a Basic GET Request with a Specific Station ID
### Expected Result: The server should return the weather data for the given station ID
If weather data from a ContentServer has already been submitted (and 30 seconds has not yet passed), data can now be requested for that specific station. Assuming the station ID is IDS60901:
```
java -cp .:lib/* GETClient localhost:1234 IDS60901
```
### Verify: 
- The AggregationServer logs indicate that it has connected to the client
- The GETClient logs indicate that the GET request was successful, with its updated lamport clock to be 4.
- The returned JSON should show the weather data associated with station IDS60901

## 4. Simultaneous PUT Requests with Lamport Clock Synchronization
### Expected Result: The server should correctly update the Lamport clock and weather data when handling multiple simultaneous PUT requests.
Open 2 terminal windows. In each window, start a different ContentServer instance with different weather data files.
In one terminal:
```
java -cp .:lib/* ContentServer localhost:1234 test_files/data.txt
```

In another terminal:
```
java -cp .:lib/* ContentServer localhost:1234 test_files/data2.txt
```
### Verify:
- The AggregationServer logs indicate that it has connected to both clients
- The ContentServer logs indicate that both PUT requests were successful with 200 OK, and Lamport clock is correctly incremented for each request.
- The WeatherData.txt file contains both of the data in JSON string format

## 5. Handle a Basic GET Request Without a Station ID
### Expected Result: The server should return the latest weather data (the most recent PUT request)
If both the weather data from step 4 have been submitted (and 30 seconds has not yet passed), run the GETClient without specifying a station ID:
```
java -cp .:lib/* GETClient localhost:1234
```
### Verify: 
- The AggregationServer logs indicate that it has connected to the client
- The GETClient logs indicate that the GET request was successful, with its incremented lamport clock.
- The returned JSON should show the weather data associated with station IDS60902 (data2.txt), assuming it's the last in order previously submitted

## 6. Handle Invalid Station ID (Client Failure-Tolerant)
### Expected Result: The server should still return the latest weather data (the most recent PUT request)
Run the GETClient with an invalid station ID, e.g. "001"
```
java -cp .:lib/* GETClient localhost:1234 001
```
### Verify:
- The AggregationServer logs indicate that it has connected to the client
- The GETClient logs indicate that the GET request was successful, with its incremented lamport clock.
- The returned JSON should show the weather data associated with station IDS60902 (data2.txt), assuming it's the last in order previously submitted

## 7. Test Expiring Data
### Expected Result: The Aggregation Server should remove weather data from content servers that haven't sent a PUT request for over 30 seconds
Start the ContentServer with a weather data file (e.g. test_files/data.txt):
```
java -cp .:lib/* ContentServer localhost:1234 test_files/data.txt
```

Then, wait for 30 seconds and run the GETClient:
```
java -cp .:lib/* GETClient localhost:1234
```
### Verify:
- The AggregationServer logs indicate that it has removed expired data for station: IDS60901 after 30 seconds
- The WeatherData.txt file should no longer contain the data in JSON string format
- The GETClient logs indicate that it failed to send the GET request due to no data being available.

## 8. Test Crash Recovery
### Expected Result: The Aggregation Server can recover weather data after a simulated crash by using WeatherData.txt
Start the ContentServer with a weather data file (e.g. test_files/data.txt):
```
java -cp .:lib/* ContentServer localhost:1234 test_files/data.txt
```

To simulate a crash, manually stop the AggregationServer by pressing Ctrl+C in the terminal running the server. Then, restart the AggregationServer:
```
java -cp .:lib/* AggregationServer 1234
```

Run the GETClient to verify that the weather data for station IDS60901 is still available after the server restart
```
java -cp .:lib/* GETClient localhost:1234
```
### Verify:
- The AggregationServer logs indicate "Reading line: [weather_data]"
- The WeatherData.txt file should still contain the data in JSON string format
- The GETClient logs indicate that the GET request was successful, with its incremented lamport clock, and the returned JSON data

## 9. Test Full Workflow
### Expected Result: System should handle concurrent PUT and GET requests while maintaining Lamport clock order
Start 2 ContentServers:
```
java -cp .:lib/* ContentServer localhost:1234 test_files/data.txt
```
```
java -cp .:lib/* ContentServer localhost:1234 test_files/data2.txt 
```

Then, start 2 GETClients in separate terminals:
```
java -cp .:lib/* GETClient localhost:1234 IDS60901
```
```
java -cp .:lib/* GETClient localhost:1234 IDS60902
```
### Verify:
- The AggregationServer logs should indicate that each request has been added to the queue with the server's current Lamport clock
- Server's Lamport clock is incremented when a request has been processed 
- The ContentServer and GETClient logs should indicate that these requests have been correctly handled in order of the aggregation server's Lamport clock.