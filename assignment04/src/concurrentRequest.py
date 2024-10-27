import os
import subprocess

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
            print("PUT Response: ", output)
            break
        elif "Error" in output:
            print("PUT Response: ", output)
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

    print("Get Response:\n", output)


def main():
    requests = ["PUT", "GET", "PUT"]
    files = getFiles()

    # print("Files: ", files)
    # print("Requests: ", requests)

    for i in range(len(requests)):
        request = requests[i]
        if request == "PUT":
            file = files.pop(0)  # Pop the first element in the list
            print("Request: PUT - ", file)

            sendPUT(request, file)
        elif request == "GET":
            print("Request: GET")
            sendGET(request)

        print()

    print()


if __name__ == "__main__":
    main()
