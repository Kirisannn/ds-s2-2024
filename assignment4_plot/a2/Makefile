
# java -cp .:lib/* ContentServer "localhost:5050" weather_data.txt
# java -cp .:lib/* ContentServer "localhost:5050" weather_data1.txt
# java -cp .:lib/* ContentServer "localhost:5050" weather_data2.txt
# java -cp .:lib/* ContentServer "localhost:5050" weather_data3.txt
# java -cp .:lib/* ContentServer "localhost:5050" weather_data4.txt
# java -cp .:lib/* ContentServer "localhost:5050" weather_data5.txt
# java -cp .:lib/* ContentServer "localhost:5050" weather_data6.txt
# java -cp .:lib/* ContentServer "localhost:5050" weather_data7.txt
#Java compiler
JAVAC = javac

#Directory Structure
SRC_DIR = src
CP = -cp "json-20240303.jar"
RUNJAR = -cp "json-20240303.jar:"


#Compile Aggregation Server
compile:
	$(JAVAC) $(CP) AggregationServer.java ContentServer.java GETClient.java PutRequest.java LamportClock.java

#Start Aggregation Server
start:
	java $(RUNJAR) AggregationServer 5050

#Run the ContentServer
run_contentserver:
	java $(RUNJAR) ContentServer "localhost:5050" weather_data.txt
	java $(RUNJAR) ContentServer "localhost:5050" weather_data1.txt
	java $(RUNJAR) ContentServer "localhost:5050" weather_data2.txt
	java $(RUNJAR) ContentServer "localhost:5050" weather_data3.txt
	java $(RUNJAR) ContentServer "localhost:5050" weather_data4.txt
	java $(RUNJAR) ContentServer "localhost:5050" weather_data5.txt
	java $(RUNJAR) ContentServer "localhost:5050" weather_data6.txt
	java $(RUNJAR) ContentServer "localhost:5050" weather_data7.txt
	java -cp "json-20240303.jar:" ContentServer "localhost:5050" ./input/xx.txt

#Run the client with test file
run_client:
	java $(RUNJAR) -cp "json-20240303.jar:" GETClient "localhost:5050" 

clean:
	rm -f *.class
