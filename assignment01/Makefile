DIR = assignment01

# Compile all Java files
compile:
	javac $(DIR)/Calculator.java $(DIR)/CalculatorImplementation.java $(DIR)/CalculatorServer.java $(DIR)/CalculatorClient.java

# RMIRegistry
rmiRegistry:
	rmiregistry &

# Run the CalculatorServer
runServer:
	java CalculatorServer

# Run the CalculatorClient
runClient:
	java CalculatorClient