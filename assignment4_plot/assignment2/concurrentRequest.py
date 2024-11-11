# import matplotlib.pyplot as plt
import os
import subprocess
import time
from time import sleep


def getFiles():
    files = []
    for file in os.listdir("input"):
        if file.endswith(".txt"):
            files.append(file)
    files.sort()
    return files


def sendPUT(request, file):
    # new thread to send request
    command = [
        "java",
        "-cp",
        ".:lib/*",
        "ContentServer",
        "localhost:4567",
        "input/" + file,
    ]
    # Run the command
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    while True:
        output = process.stdout.readline().strip()

        # print(output.strip())
        if "PUT request successful" in output:
            # print("PUT Response: ", output)
            break
        elif "Failed to send" in output or "" == output:
            # print("PUT Response: ", output)
            break

    process.terminate()


def sendGET(request):
    command = [
        "java",
        "-cp",
        ".:lib/*",
        "GETClient",
        "localhost:4567",
    ]
    # Run the command in Popen
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    # Read all lines from the output
    output = ""
    json = False
    while True:
        line = process.stdout.readline()
        if "Response:" in line:
            json = True

        if json:
            output += line

        if "lat" in line or "Failed to send" in line:
            break

    # print(output)


def startAggregator():
    command = [
        "java",
        "-cp",
        ".:lib/*",
        "AggregationServer",
    ]
    # Run the command
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    return process


def runTest(multiplier):
    requests = [
        "PUT",
        "GET",
        "PUT",
        "PUT",
        "PUT",
        "GET",
        "GET",
        "PUT",
        "GET",
        "GET",
    ] * multiplier

    files = getFiles()

    # aggregator = startAggregator()

    # Start timer here
    start_time = time.time()

    for i in range(len(requests)):
        # print(f"Request {i}")
        request = requests[i]
        if request == "PUT":
            file = files.pop(0)  # Pop the first element in the list
            # print("Request: PUT - ", file)

            sendPUT(request, file)
        elif request == "GET":
            # print("Request: GET")
            sendGET(request)

    # aggregator.terminate()  # Terminate the Aggregator process

    # End timer here
    end_time = time.time()
    elapsed = (end_time - start_time) * 1000
    return elapsed


def main():
    data = []

    multiplier = 10

    time = runTest(multiplier)
    # print(f"No. of Requests: {multiplier * 10}, Time: {time} ms")
    data.append((multiplier * 10, time))

    # Delete WeatherData.txt
    while os.path.exists("WeatherData.txt"):
        os.remove("WeatherData.txt")

    # Write the data to a csv file with headers "RequestsNumber" and "Time"
    for d in data:
        print(f"{d[0]},{d[1]}")


if __name__ == "__main__":
    main()
