# Distributed Systems - Assignment 2

## Directory Structure Check

Before proceeding with use, please ensure the directory is as follows:

```
(WORKSPACE)
│
├── bin/
│   ├── <All compiled class files as well as test script copies will be here>
│   ├── input/
│   │   └── <All input files are copied here>
│   │
│   └── runtimeFiles/
│       └── <All files used during runtime will be located here>
│
├── lib/
│   └── gson-2.11.0.jar
│
├── src/
│   ├── <Source code for all Java, python and test scripts are here>
│   ├── AggregationServer.java
│   ├── concurrentRequests.py
│   ├── ContentServer.java
│   ├── ContentServerTests.py
│   ├── crashAggregator
│   ├── GETClient.java
│   ├── GETClientTests.py
│   ├── LamportClock.java
│   ├── testConcurrentRequest
│   ├── testGET
│   ├── testPUT
│   ├── input/
│   │   ├── <Place all your input .txt files here>
│   │   ├── IDS60901.txt      (Required for test scripts)
│   │   ├── IDS60901_2.txt    (Required for test scripts)
│   │   └── invalidInput.txt  (Required for test scripts)
│   │
│   └── runtimeFiles/
│       ├── <Runtime files will be here>
│       ├── aggregatorOutput.log
│       └── testPUToutput.log
│
├── Makefile
└── README.md
```

## Compilation

For ease of compilation. Please use the following `make` command:


```
make compile
```

## Running Aggregation Server

The following command format is used to start the server

```
java -cp "bin:lib/gson-2.11.0.jar" AggregationServer <port>
```

- `port` - The port you wish to start the server on, but may not be successful if port is already in use.

<br>

Should you wish to start the server on the default `port=4567`, you may also use the following `make` command for convenience:

```
make run-aggregation
```

## Running Content Server

To start the content server, please use the following command format:

```
java -cp "bin:lib/gson-2.11.0.jar" ContentServer localhost:<port> <filepath>
```

- `port` - As before, this will indicate the port of Aggregation Server to connect.
- `filepath` - The path of the input file you wish to send.

## Running GET Client

To start the GET client, please use the following command format:

```
java -cp "bin:lib/gson-2.11.0.jar" GETClient localhost:<port> <station_id>
```

- `port` - Same as for Content Server.
- `station_id` - The ID of the station you wish to retrieve. (OPTIONAL)


## Test Cases covered

Three test scripts have been provided. The cases covered are as follows for each element.

### Content Server
1. No request body.

2. Invalid input file sent.

3. Valid input file sent.

### GET Client
1. Send GET request with unspecified station.

2. Send GET request with specific station (content available).

3. Send GET request with specific station (content unavailable)

### Aggregation Server

1. Ability to correctly respond with requests being received in the order `(PUT-GET-PUT)`

## How Aggregation works

2. Data loading

    If server starts after a crash, it will try to read from `weather_backup.json`. Should it fail, then try to check if `weather.json` exists, and try to read from it again. If both failed, then proceed to create a new empty `weather.json`.

    In this way, there are 2 copies of the data each each successful update. Otherwise, on server reboot from a crash, there will be 2 layers of data security to ensure availability.