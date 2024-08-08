# Compile all Java files
compile:
	javac -d builds Calculator.java CalculatorImplementation.java CalculatorServer.java CalculatorClient.java

# RMIRegistry
rmiRegistry:
	rmiregistry &

# Run the CalculatorServer
runServer:
	java CalculatorServer

# Run the CalculatorClient
runClient:
	java CalculatorClient

clean:
	rm -rf builds/*.class