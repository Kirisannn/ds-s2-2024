# Targets
SOURCES = src/main/java/com/example/calculator/Calculator.java src/main/java/com/example/calculator/CalculatorImplementation.java src/main/java/com/example/calculator/CalculatorServer.java src/test/java/com/example/calculator/CalculatorClient.java

compile:
	javac -d build $(SOURCES)

rmiregistry:
	rmiregistry &

runServer:
	java build.CalculatorServer &

runClients:
	java build.CalculatorClient

test:
	make compile
	make runServer
	sleep 2
	java -jar /home/kirisan/Documents/ds-s2-2024/assignment01-1/my-app/lib/junit-platform-console-standalone-1.11.0-RC1.jar --class-path build --scan-classpath

manClean:
	rm -f build/*.class

compileMVN:
	mvn clean compile

run-server:
	mvn exec:java -Dexec.mainClass="com.example.calculator.CalculatorServer"

run-client:
	mvn exec:java -Dexec.mainClass="com.example.calculator.CalculatorClient"

clean:
	mvn clean
