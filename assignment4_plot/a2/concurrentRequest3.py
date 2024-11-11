import os
import subprocess
import time

# Path to input files and jar library
INPUT_FOLDER = "./input"
JAR_PATH = "./*.jar)"  # Ensure this path is correct
CLASS_PATH = "./*.class"  # Ensure all paths are correct

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
        f"{CLASS_PATH}:{JAR_PATH}",  # Use correct classpath format for all dependencies
        "ContentServer",
        "localhost:5050",
        f"{INPUT_FOLDER}/{file}",
    ]

    # Run the command and capture output
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    try:
        output = process.communicate(timeout=10)[0]  # 10-second timeout for each PUT request
        if "Success" in output:
            print(f"PUT Success for {file}: {output.strip()}")
        elif "Error" in output:
            print(f"PUT Error for {file}: {output.strip()}")
        elif "200" in output or "201" in output:
            print(f"PUT Status for {file}: {output.strip()}")
        else:
            print(f"Unexpected PUT response for {file}: {output.strip()}")
    except subprocess.TimeoutExpired:
        print(f"Timeout expired for PUT request on file {file}")
        process.terminate()
    print(f"PUT request completed for file: {file}")

def send_get():
    print("Sending GET request...")
    command = [
        "java",
        "-cp",
        f"{CLASS_PATH}:{JAR_PATH}",  # Ensure the classpath includes necessary libraries
        "GETClient",
        "localhost:5050",
    ]

    # Run the command in Popen
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )

    try:
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
    except subprocess.TimeoutExpired:
        print("Timeout expired for GET request")
        process.terminate()
    print("GET request completed.")

def start_aggregator():
    print("Starting Aggregation Server...")
    command = [
        "java",
        "-cp",
        f"{CLASS_PATH}:{JAR_PATH}",  # Ensure all dependencies are specified
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
    time.sleep(2)  # Allow some time for the AggregationServer to initialize

    start_time = time.time()

    # Process requests
    for request in requests:
        if request == "PUT" and files:
            file = files.pop(0)
            send_put(file)
        elif request == "GET":
            time.sleep(1)  # Slight delay to allow PUT requests to be processed before GET
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
