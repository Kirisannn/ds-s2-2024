
// Testing Calculator with JUnit
// Importing the necessary packages
import java.net.MalformedURLException;
import java.rmi.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CalculatorClientTest {
    private static Calculator calc;

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
        assertEquals(10, calc.pop());
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
    }

    @Test
    public void testDelayPop() throws RemoteException {
        calc.pushValue(10);
        calc.pushValue(20);
        calc.pushValue(30);
        assertEquals(30, calc.delayPop(5000));
    }

}
