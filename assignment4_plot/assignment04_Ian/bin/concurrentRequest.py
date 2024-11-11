import matplotlib.pyplot as plt
import os
import subprocess
import time

def getFiles():
    files = []
    for file in os.listdir("src/input"):
        if file.endswith(".txt"):
            files.append(file)
    files.sort()
    return files


def sendPUT(request, file):
    # new thread to send request
    command = [
        "java",
        "-cp",
        "bin:lib/gson-2.11.0.jar",
        "ContentServer",
        "localhost:4567",
        "src/input/" + file,
    ]
    # Run the command
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    while True:
        output = process.stdout.readline()

        # print(output.strip())
        if "Success" in output:
            # print("PUT Response: ", output)
            break
        elif "Error" in output:
            # print("PUT Response: ", output)
            break

    process.terminate()


def sendGET(request):
    command = [
        "java",
        "-cp",
        "bin:lib/gson-2.11.0.jar",
        "GETClient",
        "localhost:4567",
    ]
    # Run the command in Popen
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    # Read all lines from the output
    output = ""
    line = ""
    json = False
    while True:
        line = process.stdout.readline()
        if "Response:" in line:
            json = True

        if json:
            output += line

        if "Lamport-Time" in line:
            break

    # print(output)


def startAggregator():
    command = [
        "java",
        "-cp",
        "bin:lib/gson-2.11.0.jar",
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

    aggregator = startAggregator()

    # Start timer here
    start_time = time.time()

    for i in range(len(requests)):
        request = requests[i]
        if request == "PUT":
            file = files.pop(0)  # Pop the first element in the list
            # print("Request: PUT - ", file)

            sendPUT(request, file)
        elif request == "GET":
            # print("Request: GET")
            sendGET(request)

        # print()

    aggregator.terminate()  # Terminate the Aggregator process

    # End timer here
    end_time = time.time()
    elapsed = (end_time - start_time) * 1000
    return elapsed


def main():
    data = []

    for multiplier in range(1, 11):
        time = runTest(multiplier)
        print(f"No. of Requests: {multiplier * 10}, Time: {time} ms")
        data.append((multiplier * 10, time))

    # Write the data to a csv file with headers "RequestsNumber" and "Time"
    with open("src/output/scaleTest.csv", "w") as file:
        file.write("RequestsNumber,Time\n")
        for d in data:
            file.write(f"{d[0]},{d[1]}\n")

if __name__ == "__main__":
    main()
