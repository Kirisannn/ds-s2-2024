compile:
	mvn clean install

run-aggregation-server:
	mvn exec:java -Dexec.mainClass="com.example.weather.AggregationServer" -Dexec.args="8080"

run-content-server:
	mvn exec:java -Dexec.mainClass="com.example.weather.ContentServer" -Dexec.args="http://localhost:8080"

run-get-client:
	mvn exec:java -Dexec.mainClass="com.example.weather.GetClient" -Dexec.args="http://localhost:8080"