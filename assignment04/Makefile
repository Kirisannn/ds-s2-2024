compile:
	javac -d bin -cp "lib/gson-2.11.0.jar" src/*.java

run-client:
	echo
	java -cp "bin:lib/gson-2.11.0.jar" GETClient http://localhost:8080 IDS60901

run-aggregation:
	echo
	java -cp "bin:lib/gson-2.11.0.jar" AggregationServer 8080

connection-test:
	telnet localhost 8080