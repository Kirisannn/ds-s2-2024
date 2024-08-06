
// Importing the necessary packages
import java.net.MalformedURLException;
import java.rmi.*;

public class CalculatorClient {

    public static void main(String[] args) {
        try {
            // Try looking up the Calculator object in the RMI registry
            Calculator c = (Calculator) Naming.lookup("rmi://localhost:1100/CalculatorServer");

            // Run tests
            // Testing Min
            c.pushValue(5);
            c.pushValue(10);
            c.pushValue(15);
            c.pushOperation("min");
            System.out.println("Min:\t" + c.pop());

            // Testing Max
            c.pushValue(5);
            c.pushValue(10);
            c.pushValue(15);
            c.pushOperation("max");
            System.out.println("Max:\t" + c.pop());

            // Testing LCM
            c.pushValue(5);
            c.pushValue(10);
            c.pushValue(15);
            c.pushOperation("lcm");
            System.out.println("LCM:\t" + c.pop());

            // Testing GCD
            c.pushValue(5);
            c.pushValue(10);
            c.pushValue(15);
            c.pushOperation("gcd");
            System.out.println("GCD:\t" + c.pop());

            // Testing isEmpty()
            System.out.println("Stack is empty:\t" + c.isEmpty());

            // Testing delayPop(2000) - 2000ms(2s) delay
            c.pushValue(5);
            c.pushValue(10);
            c.pushValue(15);
            System.out.println("Delayed Pop:\t" + c.delayPop(2000) + "\tdelayed 2000ms");

            System.out.println("New top stack value:\t" + c.pop());

            System.out.println("Tests Completed");
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("Client Exception:\t" + e.toString());
        }
    }
}
