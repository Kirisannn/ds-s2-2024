CLASSPATH=lib/gson-2.11.0.jar:lib/slf4j-api-2.0.16.jar:lib/slf4j-simple-2.0.16.jar
BUILD_PATH=bin

compile:	src/*.java
	javac -cp $(CLASSPATH) -d $(BUILD_PATH) src/*.java

# 	java -cp $(CLASSPATH):$(BUILD_PATH) Election 1 > Election.log 2>&1 && exit 0 || exit 1
test1:
	java -cp $(CLASSPATH):$(BUILD_PATH) Election 1