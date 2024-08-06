all:
	make Calculator
	make test
	make clean

Calculator:	Calculator.java CalculatorImplementation.java CalculatorServer.java CalculatorClient.java
	javac Calculator.java CalculatorImplementation.java CalculatorServer.java CalculatorClient.java

rmiregistry:
	rmiregistry &

runServer:	rmiregistry
	java CalculatorServer &

runClient:	
	java CalculatorClient

test:	Calculator runServer
	sleep 2
	make runClient

clean:
	rm -rf *.class