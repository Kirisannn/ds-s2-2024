import os
import subprocess
from threading import Thread


def getFiles():
    files = []
    for file in os.listdir("src/input"):
        if file.endswith(".txt"):
            files.append(file)
    files.sort()
    return files


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


def captureAggregatorOutput(process):
    # Capture Aggregator output in a file for analysis
    with open("src/runtimeFiles/aggregatorOutput.log", "w") as log:
        while True:
            output = process.stdout.readline()
            print(output.strip(), file=log)

            if "successfully" in output:
                break
            elif "Error" in output:
                break

    process.terminate()


def sendValidPUT(file):
    print("Testing sending PUT request with VALID file: " + file)

    # If file not empty
    if file != "":
        file = "src/input/" + file

    Threads = []
    # Create a thread to run the Aggregator
    aggregator = startAggregator()
    aggregatorT = Thread(target=captureAggregatorOutput, args=(aggregator,))
    Threads.append(aggregatorT)
    aggregatorT.start()

    # Create a thread to run the PUT request
    putT = Thread(target=validHelper, args=(file,))
    Threads.append(putT)
    putT.start()

    # Wait for all threads to finish
    for thread in Threads:
        thread.join()

    aggregator.terminate()  # Terminate the Aggregator process

    # Check if the PUT request was successful by checking the Aggregator output & PUT output
    # If last line of Aggregator output is "Updated weather successfully." then PUT request was successful
    updateSuccess = False
    with open("src/runtimeFiles/aggregatorOutput.log", "r") as log:
        lines = log.readlines()
        if "Updated weather successfully." in lines[-1]:
            updateSuccess = True
    with open("src/runtimeFiles/testPUToutput.log", "r") as log:
        lines = log.readlines()
        if "Connection: close" in lines[-1]:
            updateSuccess = True
        else:
            updateSuccess = False

    if updateSuccess:
        print("TEST: PASSED\n")
    else:
        print("TEST: FAILED\n")


def validHelper(file):
    command = [
        "java",
        "-cp",
        "bin:lib/gson-2.11.0.jar",
        "ContentServer",
        "localhost:4567",
        file,
    ]
    # Run the command
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    with open("src/runtimeFiles/testPUToutput.log", "w") as log:
        while True:
            output = process.stdout.readline()
            print(output.strip(), file=log)

            if "Connection:" in output:
                break

    process.terminate()


def sendEmptyPUT():
    print("Testing sending PUT request with EMPTY file")

    Threads = []
    # Create a thread to run the Aggregator
    aggregator = startAggregator()
    aggregatorT = Thread(target=captureAggregatorOutput, args=(aggregator,))
    Threads.append(aggregatorT)
    aggregatorT.start()

    # Create a thread to run the PUT request
    putT = Thread(target=emptyHelper)
    Threads.append(putT)
    putT.start()

    # Wait for all threads to finish
    for thread in Threads:
        thread.join()

    aggregator.terminate()  # Terminate the Aggregator process

    # Check if the PUT request was successful by checking the Aggregator output & PUT output
    # If last line of Aggregator output is "Updated weather successfully." then PUT request was successful
    updateSuccess = False
    with open("src/runtimeFiles/aggregatorOutput.log", "r") as log:
        lines = log.readlines()
        if "Updated weather successfully." in lines[-1]:
            updateSuccess = True
    with open("src/runtimeFiles/testPUToutput.log", "r") as log:
        lines = log.readlines()
        if "Connection: close" in lines[-1]:
            updateSuccess = True
        else:
            updateSuccess = False

    if updateSuccess:
        print("TEST: PASSED\n")
    else:
        print("TEST: FAILED\n")


def emptyHelper():
    command = [
        "java",
        "-cp",
        "bin:lib/gson-2.11.0.jar",
        "ContentServer",
        "localhost:4567",
    ]
    # Run the command
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    with open("src/runtimeFiles/testPUToutput.log", "w") as log:
        while True:
            output = process.stdout.readline()
            print(output.strip(), file=log)

            if "Connection:" in output:
                break

    process.terminate()


def sendInvalidJSON():
    file = "src/input/invalidInput.txt"

    print("Testing sending PUT request with INVALID file: " + file)

    Threads = []
    # Create a thread to run the Aggregator
    aggregator = startAggregator()
    aggregatorT = Thread(target=captureAggregatorOutput, args=(aggregator,))
    Threads.append(aggregatorT)
    aggregatorT.start()

    # Create a thread to run the PUT request
    putT = Thread(target=invalidHelper, args=(file,))
    Threads.append(putT)
    putT.start()

    # Wait for all threads to finish
    for thread in Threads:
        thread.join()

    aggregator.terminate()  # Terminate the Aggregator process

    # Check if the PUT request was successful by checking the Aggregator output & PUT output
    # If last line of Aggregator output is "Updated weather successfully." then PUT request was successful
    updateFailed = False
    with open("src/runtimeFiles/aggregatorOutput.log", "r") as log:
        lines = log.readlines()
        for line in lines:
            if "Error" in line:
                updateFailed = True
                break
    with open("src/runtimeFiles/testPUToutput.log", "r") as log:
        lines = log.readlines()
        if "HTTP/1.1 500 Internal Server Error" in lines[-1]:
            updateFailed = False
        else:
            updateFailed = True

    if updateFailed:
        print("TEST: PASSED\n")
    else:
        print("TEST: FAILED\n")


def invalidHelper(file):
    command = [
        "java",
        "-cp",
        "bin:lib/gson-2.11.0.jar",
        "ContentServer",
        "localhost:4567",
        file,
    ]
    # Run the command
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    with open("src/runtimeFiles/testPUToutput.log", "w") as log:
        while True:
            output = process.stdout.readline()
            print(output.strip(), file=log)

            if "Connection:" in output:
                break

    process.terminate()


def main():
    files = getFiles()
    sendValidPUT(files[0])
    sendEmptyPUT()
    sendInvalidJSON()
    return


if __name__ == "__main__":
    main()
