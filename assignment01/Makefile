server: compile rmiRegistry runServer 
	@touch tests.log
	sleep 2
	make runClientJunit > tests.log 2>&1

# Compile all Java files
compile:
	javac -d builds/ *.java

# RMIRegistry
rmiRegistry:
	rmiregistry &
	sleep 2

# Run the CalculatorServer
runServer:
	java -cp builds/ CalculatorServer > /dev/null 2>&1 &

# Run the CalculatorClient in case of any manual code
runClient:
	java -cp builds/ CalculatorClient

# Run the CalculatorClient JUnit tests
runClientJunit:
	java -jar test-lib/junit-platform-console-standalone-1.11.0-RC1.jar --class-path builds --select-class CalculatorClient

clean:
	chmod +x killRMI.sh
	./killRMI.sh
	rm -rf builds/*.class
	rm -rf *.log
	rm -rf builds
	clear