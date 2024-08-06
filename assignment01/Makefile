Calculator:	Server/Calculator.java Server/CalculatorImplementation.java Server/CalculatorServer.java Client/CalculatorClient.java
	javac -d . Server/Calculator.java Server/CalculatorImplementation.java Server/CalculatorServer.java Client/CalculatorClient.java

rmiregistry:
	rmiregistry &

runServer:	rmiregistry
	java Server.CalculatorServer &

runClient:	
	java Client.CalculatorClient

test:	Calculator runServer
	sleep 2
	make runClient

clean:
	rm -rf Server/*.class Client/*.class *.class