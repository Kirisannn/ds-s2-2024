

import java.rmi.*;

public interface Calculator extends Remote {

    // Take val and push it on to the top of the stack.
    public void pushValue(int val, String clientID) throws RemoteException;

    // Push a String containing an operator("min", "max", "lcm", "gcd") to the stack,
    // which will cause the server to pop all the values on the stack and:
    // for min - push the min value of all the popped values;
    // for max - push the max value of all the popped values
    // for lcm - push the least common multiple of all the popped values;
    // for gcd - push the greatest common divisor of all the popped values
    public void pushOperation(String operator, String clientID) throws RemoteException;

    // Pop the top of the stack and return it to the client.
    public int pop(String clientID) throws RemoteException;

    // Return true if the stack is empty, false otherwise.
    public boolean isEmpty(String clientID) throws RemoteException;

    // Wait millis milliseconds before carrying out the pop operation as above.
    public int delayPop(int millis, String clientID) throws RemoteException;
}
