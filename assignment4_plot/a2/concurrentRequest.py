import os
import subprocess
import time

# Path to input files
INPUT_FOLDER = "./input"

def get_files():
    print("Getting list of input files...")
    files = []
    for file in os.listdir(INPUT_FOLDER):
        if file.endswith(".txt"):
            files.append(file)
    files.sort()
    print(f"Found {len(files)} files for processing: {files}")
    return files

def send_put(file):
    print(f"Sending PUT request for file: {file}")
    command = [
        "java",
        "-cp",
        "./gson-2.11.0.jar",
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

def send_get():
    print("Sending GET request...")
    command = [
        "java",
        "-cp",
        "./gson-2.11.0.jar",
        "GETClient",
        "localhost:5050",
    ]

    # Run the command in Popen
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    output = ""
    json = False
    while True:
        line = process.stdout.readline()
        if "Response:" in line:
            json = True

        if json:
            output += line

        if "Lamport-Time" in line:
            print(f"GET Response: {output.strip()}")
            break

    process.terminate()
    print("GET request completed.")

def start_aggregator():
    print("Starting Aggregation Server...")
    command = [
        "java",
        "-cp",
        "./gson-2.11.0.jar",
        "AggregationServer",
    ]
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )
    print("Aggregation Server started.")
    return process

def run_test(multiplier):
    print(f"Running test with {multiplier * 10} requests...")
    requests = ["PUT", "GET", "PUT", "PUT", "PUT", "GET", "GET", "PUT", "GET", "GET"] * multiplier
    files = get_files()

    aggregator = start_aggregator()

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

    print("Writing test results to src/output/scaleTest.csv...")
    with open("./output/scaleTest.csv", "w") as file:
        file.write("RequestsNumber,Time\n")
        for d in data:
            file.write(f"{d[0]},{d[1]}\n")
    print("Results written successfully.")

if __name__ == "__main__":
    main()
