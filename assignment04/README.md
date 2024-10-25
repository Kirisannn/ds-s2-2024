# Distributed Systems - Assignment 2

## Compilation

```
javac -d bin -cp "lib/gson-2.11.0.jar" src/*.java
```

## Start Aggregation Server

```
java -cp "bin:lib/gson-2.11.0.jar" AggregationServer
```

## Test Cases covered

1. Data loading
    
    If server starts after a crash, it will try to read from `weather_backup.json`. Should it fail, then try to check if `weather.json` exists, and try to read from it again. If both failed, then proceed to create a new empty `weather.json`.

    In this way, there are 2 copies of the data each each successful update. Otherwise, on server reboot from a crash, there will be 2 layers of data security to ensure availability.