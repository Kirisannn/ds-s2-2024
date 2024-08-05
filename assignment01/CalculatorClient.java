
// Importing the necessary packages
import java.net.MalformedURLException;
import java.rmi.*;

public class CalculatorClient {

    public static void main(String[] args) {
        try {
            // Try looking up the Calculator object in the RMI registry
            Calculator c = (Calculator) Naming.lookup("rmi://localhost:1100/CalculatorServer");

            // Run tests
            c.pushValue(5);
            c.pushValue(10);
            c.pushValue(15);

            c.pushOperation("min");
            System.out.println("Min: " + c.pop());

            c.pushValue(5);
            c.pushValue(10);
            c.pushValue(15);

            c.pushOperation("max");
            System.out.println("Max: " + c.pop());

            c.pushValue(5);
            c.pushValue(10);
            c.pushValue(15);

            c.pushOperation("lcm");
            System.out.println("LCM: " + c.pop());

            c.pushValue(5);
            c.pushValue(10);
            c.pushValue(15);

            c.pushOperation("gcd");
            System.out.println("GCD: " + c.pop());

            // Test isEmpty()
            System.out.println("Stack is empty: " + c.isEmpty());

            // Test delayPop(200) - 200ms delay
            c.pushValue(5);
            c.pushValue(10);
            c.pushValue(15);
            c.delayPop(2000);
            System.out.println("Min: " + c.pop() + " (delayed 2000ms)");

            System.out.println("Tests Completed");
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("Client Exception: " + e.toString());
        }
    }
}
