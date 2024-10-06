CLASSPATHS = -cp .:lib/gson-2.11.0.jar:lib/spark-core-2.9.4.jar:lib/slf4j-api-2.0.16.jar:lib/slf4j-simple-2.0.16.jar
BIN = -d ./bin

compile:
	javac $(CLASSPATHS) LamportClock.java
	javac $(CLASSPATHS) LamportRequest.java
	javac $(CLASSPATHS) AggregationServer.java

run-aggregation:
	java $(CLASSPATHS) AggregationServer