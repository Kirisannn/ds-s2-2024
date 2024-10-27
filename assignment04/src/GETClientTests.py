import sys
import os
import subprocess


def sendPUT(file):
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
            break
        elif "Error" in output:
            break

    process.terminate()


def testGetAll():
    print("===========================================================================")
    print("TEST GET ALL WEATHER DATA")

    command = [
        "java",
        "-cp",
        "bin:lib/gson-2.11.0.jar",
        "GETClient",
        "localhost:4567",
    ]
    process = subprocess.run(command, capture_output=True, text=True)
    output = process.stdout
    error = process.stderr

    # Output both the output and error
    if output is not None:
        print(output)
    if error is not None:
        print(error)


def testGetSpecific():
    print("===========================================================================")
    print("TEST GET SPECIFIC WEATHER DATA (IDS60901)")

    sendPUT("IDS60901.txt")

    command = [
        "java",
        "-cp",
        "bin:lib/gson-2.11.0.jar",
        "GETClient",
        "localhost:4567",
        "IDS60901",
    ]
    process = subprocess.run(command, capture_output=True, text=True)
    output = process.stdout
    error = process.stderr

    # Output both the output and error
    if output is not None:
        print(output)
    if error is not None:
        print(error)


def testIdNotExist():
    print("===========================================================================")
    print("TEST GET SPECIFIC WEATHER DATA (ID DOES NOT EXIST)")

    command = [
        "java",
        "-cp",
        "bin:lib/gson-2.11.0.jar",
        "GETClient",
        "localhost:4567",
        "ID12345",
    ]
    process = subprocess.run(command, capture_output=True, text=True)
    output = process.stdout
    error = process.stderr

    # Output both the output and error
    if output is not None:
        print(output)
    if error is not None:
        print(error)


def main():
    testGetAll()
    testGetSpecific()
    testIdNotExist()


if __name__ == "__main__":
    main()
