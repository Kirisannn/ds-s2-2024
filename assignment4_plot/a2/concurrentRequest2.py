import os
import subprocess
import time

# Path to input files and jar library
INPUT_FOLDER = "./input"
JAR_PATH = "./json-20240303.jar"  # Ensure this path is correct
CLASS_PATH = "./*.class"  # Ensure all paths are correct

def get_files():
    print("Getting list of input files...")
    files = []
    for file in os.listdir(INPUT_FOLDER):
        if file.endswith(".txt"):
            files.append(file)
    files.sort()
    # print(f"Found {len(files)} files for processing: {files}")
    return files

def send_put(file):
    print(f"Sending PUT request for file: {file}")
    command = [
        "java",
        "-cp",
        "json-20240303.jar:",  # Use correct classpath format for all dependencies
        "ContentServer",
        "localhost:5050",
        f"{INPUT_FOLDER}/{file}",
    ]

    # Run the command and capture output
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    while True:
        output = process.stdout.readline()
        if "Success" in output:
            print(f"PUT Success for {file}: {output.strip()}")
            break
        elif "Error" in output:
            print(f"PUT Error for {file}: {output.strip()}")
            break
        elif "200" in output or "201" in output:
            print(f"PUT Status for {file}: {output.strip()}")
            break

    process.terminate()
    print(f"PUT request completed for file: {file}")

import subprocess

def send_get():
    print("Sending GET request...")
    command = [
        "java",
        "-cp",
        "./json-20240303.jar:./GETClient.class",  # Adjust the classpath as necessary
        "GETClient",
        "localhost:5050"  # Replace with actual server address if different
    ]

    # Start the process and capture output
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    output = ""
    capture_output = False
    while True:
        line = process.stdout.readline()
        # print("Line: " + line)
        if "\n" in line:  # Adjust trigger based on server's actual response
            capture_output = True
        if capture_output:
            output += line
        if "\n" in line:  # Adjust based on final response structure
            print(f"GET Response: {output.strip()}")
            break

    process.terminate()
    print("GET request completed.")


def start_aggregator():
    print("Starting Aggregation Server...")
    command = [
        "java",
        "-cp",
        f"{JAR_PATH}:",  # Ensure all dependencies are specified
        "AggregationServer",
        "5050"
    ]
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )
    print("Aggregation Server started.")
    return process

def run_test(multiplier):
    print(f"Running test with {multiplier * 10} requests...")
    requests = ["PUT", "PUT", "PUT", "GET", "GET", "PUT", "GET", "GET", "PUT", "GET"] * multiplier
    files = get_files()

    aggregator = start_aggregator()
    time.sleep(2)  # Allow some time for the AggregationServer to initialize

    start_time = time.time()

    for i in range(len(requests)):
        request = requests[i]
        if request == "PUT" and files:
            file = files.pop(0)
            send_put(file)
        elif request == "GET":
            send_get()

    aggregator.terminate()
    print("Aggregation Server terminated.")

    end_time = time.time()
    elapsed = (end_time - start_time) * 1000
    print(f"Test completed in {elapsed:.2f} ms.")
    return elapsed

def main():
    print("Starting main test sequence...")
    data = []

    for multiplier in range(1, 11):
        time_elapsed = run_test(multiplier)
        print(f"No. of Requests: {multiplier * 10}, Time: {time_elapsed:.2f} ms")
        data.append((multiplier * 10, time_elapsed))

    print("Writing test results to output/scaleTest.csv...")
    with open("./output/scaleTest.csv", "w") as file:
        file.write("RequestsNumber,Time\n")
        for d in data:
            file.write(f"{d[0]},{d[1]}\n")
    print("Results written successfully.")

if __name__ == "__main__":
    main()
