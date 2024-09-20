all: src/*.java
	javac -d build -cp lib/junit-platform-console-standalone-1.11.0.jar:lib/json-20240303.jar src/*.java

# Run aggregation server with default port 8080
# java -cp build:lib/* AggregationServer 8080

# Run GETClient with default port 8080
# java -cp build:lib/* GETClient 8080

# Run ContentServer with default port 8080
# java -cp build:lib/* ContentServer 8080

clean:
	rm -f build/*.class

runMultiGet:
	java -cp build GETClient localhost:8080 &
	java -cp build GETClient localhost:8080 &
	java -cp build GETClient localhost:8080 &
	java -cp build GETClient localhost:8080 &
	java -cp build GETClient localhost:8080
