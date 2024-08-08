import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class CalculatorClient {

    // Main function for manual testing - Left empty for JUnit testing.
    // Must comment out JUnit tests to conduct manual testing.
    public static void main(String[] args) throws RemoteException {

    }

    // Beginning of JUnit Tests

    // Testing pushVal() and pop() methods.
    @Test
    public void testPushValPop() throws RemoteException {
        try {
            Calculator calc = (Calculator) Naming.lookup("rmi://localhost:1100/CalculatorServer");
            calc.pushValue(10, "Client1");
            Boolean result1 = (calc.pop("Client1") == 10);
            if (result1) {
                System.out.println("Push & Pop Test Passed");
            } else {
                System.out.println("Push & Pop Test Failed");
            }
            assertEquals(true, result1);
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("CalculatorClient exception: " + e.getMessage());
        }
    }

    // Testing pushOperation() - min, max, lcm, gcd
    @Test
    public void testPushOperation() throws RemoteException {
        // Test min
        try {
            Calculator calc = (Calculator) Naming.lookup("rmi://localhost:1100/CalculatorServer");
            calc.pushValue(10, "Client2");
            calc.pushValue(20, "Client2");
            calc.pushValue(30, "Client2");
            calc.pushOperation("min", "Client2");
            Boolean result2 = (calc.pop("Client2") == 10);
            if (result2) {
                System.out.println("Min Test Passed");
            } else {
                System.out.println("Min Test Failed");
            }
            assertEquals(true, result2);
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("CalculatorClient exception: " + e.getMessage());
        }

        // Test max
        try {
            Calculator calc = (Calculator) Naming.lookup("rmi://localhost:1100/CalculatorServer");
            calc.pushValue(10, "Client3");
            calc.pushValue(20, "Client3");
            calc.pushValue(30, "Client3");
            calc.pushOperation("max", "Client3");
            Boolean result3 = (calc.pop("Client3") == 30);
            if (result3) {
                System.out.println("Max Test Passed");
            } else {
                System.out.println("Max Test Failed");
            }
            assertEquals(true, result3);
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("CalculatorClient exception: " + e.getMessage());
        }

        // Test lcm
        try {
            Calculator calc = (Calculator) Naming.lookup("rmi://localhost:1100/CalculatorServer");
            calc.pushValue(10, "Client4");
            calc.pushValue(20, "Client4");
            calc.pushValue(30, "Client4");
            calc.pushOperation("lcm", "Client4");
            Boolean result4 = (calc.pop("Client4") == 60);
            if (result4) {
                System.out.println("LCM Test Passed");
            } else {
                System.out.println("LCM Test Failed");
            }
            assertEquals(true, result4);
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("CalculatorClient exception: " + e.getMessage());
        }

        // Test gcd
        try {
            Calculator calc = (Calculator) Naming.lookup("rmi://localhost:1100/CalculatorServer");
            calc.pushValue(10, "Client5");
            calc.pushValue(20, "Client5");
            calc.pushValue(30, "Client5");
            calc.pushOperation("gcd", "Client5");
            Boolean result5 = (calc.pop("Client5") == 10);
            if (result5) {
                System.out.println("GCD Test Passed");
            } else {
                System.out.println("GCD Test Failed");
            }
            assertEquals(true, result5);
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("CalculatorClient exception: " + e.getMessage());
        }

        // Test invalid operation
        try {
            Calculator calc = (Calculator) Naming.lookup("rmi://localhost:1100/CalculatorServer");
            calc.pushValue(10, "Client6");
            calc.pushValue(20, "Client6");
            calc.pushValue(30, "Client6");
            calc.pushOperation("invalid", "Client6");
            assertEquals(true, calc.pop("Client6")); // Shouldn't matter as would throw exception
        } catch (MalformedURLException | NotBoundException | RemoteException | IllegalArgumentException e) {
            System.err.println("CalculatorClient exception: " + e.getMessage());
        }
    }

    // Test delayPop() method
    @Test
    public void testDelayPop() throws RemoteException {
        try {
            Calculator calc = (Calculator) Naming.lookup("rmi://localhost:1100/CalculatorServer");
            calc.pushValue(10, "Client7");
            Boolean result6 = (calc.delayPop(5000, "Client7") == 10);
            if (result6) {
                System.out.println("Delay Pop Test Passed");
            } else {
                System.out.println("Delay Pop Test Failed");
            }
            assertEquals(true, result6);
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("CalculatorClient exception: " + e.getMessage());
        }
    }

    // Test isEmtpy() method
    @Test
    public void testIsEmpty() throws RemoteException {
        try {
            Calculator calc = (Calculator) Naming.lookup("rmi://localhost:1100/CalculatorServer");
            calc.pushValue(10, "Client7");
            Boolean result6 = calc.isEmpty("Client7");
            if (!result6) {
                System.out.println("isEmpty Test Passed");
            } else {
                System.out.println("isEmpty Test Failed");
            }
            assertEquals(false, result6);
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("CalculatorClient exception: " + e.getMessage());
        }
    }

    // Testing delayPop with multiple threads to simulate clients.
    @Test
    public void testMultiThread() throws RemoteException {
        try {
            System.out.println("Starting Multi-Thread Test...");
            Thread c1 = createClientThread(10, 0); // Either 1st or 2nd to pop
            Thread c2 = createClientThread(20, 5000); // 5th to pop
            Thread c3 = createClientThread(30, 3000); // 4th to pop
            Thread c4 = createClientThread(40, 0); // 2rd to pop
            Thread c5 = createClientThread(50, 1000); // 3rd to pop
            // Start all threads
            c1.start();
            c2.start();
            c3.start();
            c4.start();
            c5.start();
            // Wait for all threads to finish
            try {
                c1.join();
                c2.join();
                c3.join();
                c4.join();
                c5.join();
            } catch (InterruptedException e) {
                System.err.println("Interrupted Exception: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("CalculatorClient exception: " + e.getMessage());
        }

        System.out.println("Multi-Thread Test Completed.");
    }

    private static Thread createClientThread(int value, int delay) {
        return new Thread(() -> {
            try {
                String clientId = "Client" + Thread.currentThread().getName();
                Calculator calculator = (Calculator) Naming.lookup("rmi://localhost:1100/CalculatorServer");
                calculator.pushValue(value, clientId);
                if (delay > 0) {
                    System.out.println(clientId + " Popped w/ delay: " + calculator.delayPop(delay, clientId));
                } else {
                    System.out.println(clientId + " Popped w/o delay: " + calculator.pop(clientId));
                }
            } catch (MalformedURLException | NotBoundException | RemoteException e) {
                System.err.println("Client Exception: " + e.getMessage());
            }
        });
    }
}