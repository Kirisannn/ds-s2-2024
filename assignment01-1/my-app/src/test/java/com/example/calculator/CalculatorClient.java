// Importing the necessary packages
package com.example.calculator;

import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CalculatorClient {
    private Calculator calc;

    @BeforeEach
    public void setUp() throws RemoteException {
        calc = new CalculatorImplementation();
    }

    @Test
    public void testPushValPop() throws RemoteException {
        calc.pushValue(5);
        assertEquals(5, calc.pop());
    }

    @Test
    public void testPushOpMin() throws RemoteException {
        calc.pushValue(10);
        calc.pushValue(20);
        calc.pushValue(30);
        calc.pushOperation("min");
        assertEquals(5, calc.pop());
    }

    @Test
    public void testPushOpMax() throws RemoteException {
        calc.pushValue(10);
        calc.pushValue(20);
        calc.pushValue(30);
        calc.pushOperation("max");
        assertEquals(30, calc.pop());
    }

    @Test
    public void testPushOpLCM() throws RemoteException {
        calc.pushValue(10);
        calc.pushValue(20);
        calc.pushValue(30);
        calc.pushOperation("lcm");
        assertEquals(60, calc.pop());
    }

    @Test
    public void testPushOpGCD() throws RemoteException {
        calc.pushValue(10);
        calc.pushValue(20);
        calc.pushValue(30);
        calc.pushOperation("gcd");
        assertEquals(10, calc.pop());
    }

    // Test isEmpty, will output to test results "PASSED" if the stack is empty
    @Test
    public void testIsEmpty() throws RemoteException {
        assertTrue(calc.isEmpty());

        calc.pushValue(10);
        assertTrue(calc.isEmpty());

        calc.pop();
        assertTrue(calc.isEmpty());
    }

    @Test
    public void testDelayPop() throws RemoteException {
        calc.pushValue(10);
        calc.pushValue(20);
        calc.pushValue(30);
        assertEquals(30, calc.delayPop(5000));
    }

    // public static void main(String[] args) {
    // try {
    // // Try looking up the Calculator object in the RMI registry
    // Calculator c = (Calculator)
    // Naming.lookup("rmi://localhost:1100/CalculatorServer");

    // // Run tests
    // // Testing Min
    // c.pushValue(5);
    // c.pushValue(10);
    // c.pushValue(15);
    // c.pushOperation("min");
    // System.out.println("Min:\t" + c.pop());

    // // Testing Max
    // c.pushValue(5);
    // c.pushValue(10);
    // c.pushValue(15);
    // c.pushOperation("max");
    // System.out.println("Max:\t" + c.pop());

    // // Testing LCM
    // c.pushValue(5);
    // c.pushValue(10);
    // c.pushValue(15);
    // c.pushOperation("lcm");
    // System.out.println("LCM:\t" + c.pop());

    // // Testing GCD
    // c.pushValue(5);
    // c.pushValue(10);
    // c.pushValue(15);
    // c.pushOperation("gcd");
    // System.out.println("GCD:\t" + c.pop());

    // // Testing isEmpty()
    // System.out.println("Stack is empty:\t" + c.isEmpty());

    // // Testing delayPop(2000) - 2000ms(2s) delay
    // c.pushValue(5);
    // c.pushValue(10);
    // c.pushValue(15);
    // System.out.println("Delayed Pop:\t" + c.delayPop(2000) + "\tdelayed 2000ms");

    // System.out.println("New top stack value:\t" + c.pop());

    // System.out.println("Tests Completed");
    // } catch (MalformedURLException | NotBoundException | RemoteException e) {
    // System.err.println("Client Exception:\t" + e.toString());
    // }
    // }
}
