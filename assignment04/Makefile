compile:
	javac -d bin -cp "lib/gson-2.11.0.jar" src/*.java

run-get:
	java -cp "bin:lib/gson-2.11.0.jar" GETClient http://localhost:4567 IDS60901

run-aggregation:
	java -cp "bin:lib/gson-2.11.0.jar" AggregationServer

run-content:
	java -cp "bin:lib/gson-2.11.0.jar" ContentServer localhost:4567 

connection-test:
	telnet localhost 4567

simulate-crash:
	./src/crashAggregator