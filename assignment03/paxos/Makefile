CLASSPATH=lib/gson-2.11.0.jar:lib/slf4j-api-2.0.16.jar:lib/slf4j-simple-2.0.16.jar
BUILD_PATH=bin

compile:	src/*.java
	javac -cp $(CLASSPATH) -d $(BUILD_PATH) src/*.java

election:
	java -cp $(CLASSPATH):$(BUILD_PATH) Election > Test1.log 2>&1