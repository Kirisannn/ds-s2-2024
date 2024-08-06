
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

// Implementation for Calculator
public class CalculatorImplementation extends UnicastRemoteObject implements Calculator {

    private final Stack<Integer> stack;

    // Constructor
    protected CalculatorImplementation() throws RemoteException {
        stack = new Stack<>();
    }

    // Take val and push it on to the top of the stack.
    @Override
    public void pushValue(int val) throws RemoteException {
        stack.push(val);
        System.out.println("Pushed:\t" + val);
    }

    // Euclidean algorithm for gcd of two values
    private int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b); // formula for greatest common divisor
    }

    // gcd of multiple values
    private int gcd(Stack<Integer> values) {
        int result = values.pop();
        while (!values.isEmpty()) {
            result = gcd(result, values.pop());
        }
        return result;
    }

    // Least common multiple of two values using euclidean algorithm for gcd()
    private int lcm(int a, int b) {
        return a * (b / gcd(a, b)); // Formula for lowest common multiple
    }

    // lcm of multiple values
    private int lcm(Stack<Integer> values) {
        int result = values.pop();
        while (!values.isEmpty()) {
            result = lcm(result, values.pop());
        }
        return result;
    }

    // Push a String containing an operator("min", "max", "lcm", "gcd") to the stack,
    // which will cause the server to pop all the values on the stack and:
    // for min - push the min value of all the popped values;
    // for max - push the max value of all the popped values
    // for lcm - push the least common multiple of all the popped values;
    // for gcd - push the greatest common divisor of all the popped values
    @Override
    public void pushOperation(String operator) throws RemoteException {
        if (stack.isEmpty()) {  // Base case: if stack is empty, return
            return;
        }

        int result;
        switch (operator) {
            case "min" ->
                result = stack.stream().min(Integer::compare).orElseThrow();
            case "max" ->
                result = stack.stream().max(Integer::compare).orElseThrow();
            case "lcm" ->
                result = lcm(stack);
            case "gcd" ->
                result = gcd(stack);
            default ->
                throw new IllegalArgumentException("\n============\nInvalid Operator\n============\n");
        }

        System.out.println("\n============\n" + operator.toUpperCase() + ":\t" + result + "\n============\n");
        stack.clear();
        stack.push(result);
    }

    // Pop the top of the stack and return it to the client.
    @Override
    public int pop() throws RemoteException {
        if (stack.isEmpty()) {
            throw new IllegalStateException("\n============\nThe Stack is Empty zzz...\n============\n");
        }
        System.out.println("\n============\nPopped:\t" + stack.peek() + "\n============\n");
        return stack.pop();
    }

    // Return true if the stack is empty, false otherwise.
    @Override
    public boolean isEmpty() throws RemoteException {
        return stack.isEmpty();
    }

    // Wait millis milliseconds before carrying out the pop operation as above.
    @Override
    public int delayPop(int millis) throws RemoteException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("\n========================\nDelayed Pop:\t" + stack.peek() + "\n========================\n");
        return this.pop();
    }
}
