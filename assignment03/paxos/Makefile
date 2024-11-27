CLASSPATH=lib/gson-2.11.0.jar:lib/slf4j-api-2.0.16.jar:lib/slf4j-simple-2.0.16.jar
BUILD_PATH=bin

compile:	src/*.java
	javac -cp $(CLASSPATH) -d $(BUILD_PATH) src/*.java

test1:
	java -cp $(CLASSPATH):$(BUILD_PATH) Election 1 > Test1.log 2>&1 

test2:
	java -cp $(CLASSPATH):$(BUILD_PATH) Election 2 > Test2.log 2>&1

test3:
	java -cp $(CLASSPATH):$(BUILD_PATH) Election 3 > Test3.log 2>&1

test4:
	java -cp $(CLASSPATH):$(BUILD_PATH) Election 4 > Test4.log 2>&1

test5:
	java -cp $(CLASSPATH):$(BUILD_PATH) Election 5 > Test5.log 2>&1

clean:
	rm -rf $(BUILD_PATH)/*.class