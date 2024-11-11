import requests
import time

# Set the multiplier to control the number of requests
multiplier = 5  # Adjust this value as needed

# Define the sequence of requests
requests_list = [
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

# Define the URLs for PUT and GET requests
put_url = "http://localhost:5050/path/to/put_endpoint"
get_url = "http://localhost:5050/path/to/get_endpoint"

# Optional: Payload for PUT requests
put_data = {"key": "value"}  # Adjust based on your server's expected format

# Track total time for each request type
total_put_time = 0
total_get_time = 0
put_count = 0
get_count = 0

# Send requests and calculate times
for request_type in requests_list:
    start_time = time.time()
    
    if request_type == "PUT":
        response = requests.put(put_url, json=put_data)
        elapsed_time = time.time() - start_time
        total_put_time += elapsed_time
        put_count += 1
    elif request_type == "GET":
        response = requests.get(get_url)
        elapsed_time = time.time() - start_time
        total_get_time += elapsed_time
        get_count += 1
    
    # Print the individual request time (optional for debugging)
    print(f"{request_type} Request Time: {elapsed_time:.4f} seconds")
    # Optional: Check response status and content
    print(f"{request_type} Response Code: {response.status_code}")
    print(f"{request_type} Response: {response.text}")

# Calculate and display average times
if put_count > 0:
    avg_put_time = total_put_time / put_count
    print(f"\nAverage PUT Request Time: {avg_put_time:.4f} seconds")

if get_count > 0:
    avg_get_time = total_get_time / get_count
    print(f"Average GET Request Time: {avg_get_time:.4f} seconds")

# Display total execution time
total_execution_time = total_put_time + total_get_time
print(f"\nTotal Execution Time: {total_execution_time:.4f} seconds")
