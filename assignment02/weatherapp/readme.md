# Weather Aggregation Server

## Overview

The Weather Aggregation Server is a Java application that allows clients to store and retrieve weather data. It utilizes a Lamport clock for synchronization and supports multiple concurrent connections. The server handles PUT requests to update weather information and GET requests to retrieve it.

## Features

### Aggregation Server
- **Concurrent Handling**: Manages multiple client connections using threads.
- **Data Storage**: Stores weather data in a JSON file and handles temporary file writing for updates.
- **Lamport Clock**: Utilizes a Lamport clock to ensure synchronized access to shared data.
- **Expiration Management**: Automatically removes weather data entries that haven't been updated in the last 30 seconds.
- **Error Handling**: Provides error responses for invalid requests and missing data.
- **JSON Handling**: Supports reading and writing weather data in JSON format.

### Content Server
- **Client Requests**: Handles incoming requests from clients, allowing them to submit or retrieve weather data.
- **Request Processing**: Validates and processes PUT and GET requests using a queue mechanism to ensure ordered processing.
- **Connection Handling**: Manages individual client connections and timeouts.
- **Header Parsing**: Extracts relevant headers from client requests, including Lamport time for synchronization.

### GET Client
- **Request Sending**: Sends GET requests to the server to retrieve weather data for a specified station.
- **Response Handling**: Processes the server's response and displays the retrieved weather data.
- **Error Management**: Handles errors gracefully, providing informative messages if data retrieval fails.

## Prerequisites

- Java Development Kit (JDK) 22 or higher
- Apache Maven

## Running the Servers

### Running the Aggregation Server

1. **Navigate to the project directory**:
   ```bash
   cd path/to/weatherapp

2. **Build the project using Maven**:
    ```bash
    mvn clean install

3. **To start the Aggregation Server, use the following command**:
    ```bash
    mvn exec:java -Dexec.mainClass="com.weatherapp.AggregationServer" -Dexec.args="<PORTNUMBER>"

### Running the Content Server

1. **Navigate to the project directory**:
   ```bash
   cd path/to/weatherapp

2. **To start the Content Server, use the following command**:
    ```bash
    mvn exec:java -Dexec.mainClass="com.weatherapp.ContentServer" -Dexec.args="<URL> <STATION_ID>"

### Running the GET Client

1. **Navigate to the project directory**:
   ```bash
   cd path/to/weatherapp

2. **To start the GET Client, use the following command**:
    ```bash
    mvn exec:java -Dexec.mainClass="com.weatherapp.GETClient" -Dexec.args="<URL> <STATION_ID>"

### Running Test

1. **Testing Content Server**
    ```bash
    mvn test


### JSON Data Format
## Weather Data Structure

The weather data is stored in JSON format. Each weather entry should have the following structure:
``` json
{
    "id": "station_id",
    "temperature": 23.5,
    ... data ...
    "humidity": 60,
    "windSpeed": 5,
    "Lamport-Time": 10
}
```

### Example of a JSON File
## Here is a simple example of how the weather.json file might look:
``` json
[
    {
        "id": "station1",
        "temperature": 23.5,
        "humidity": 60,
        "windSpeed": 5,
        "Lamport-Time": 10
    },
    {
        "id": "station2",
        "temperature": 18.2,
        "humidity": 55,
        "windSpeed": 3,
        "Lamport-Time": 9
    }
]
```


### Notes
- Issues with parsing and byte management during response & request message passing cause work to stop. I was unable to find the issue despite hours and days of research, debugging and scrolling through piazza.