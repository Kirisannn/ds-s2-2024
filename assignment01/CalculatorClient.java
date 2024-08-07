import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class CalculatorClient {

    public static void main(String[] args) {
        try {
            // Multi-thread test with 3 clients
            testMultiThread();

        } catch (Exception e) {
            System.err.println("CalculatorClient exception: " + e.getMessage());
        }
    }

    private static void testMultiThread() {
        System.out.println("Starting Multi-Thread Test...");

        Thread c1 = createClientThread(10, 0);
        Thread c2 = createClientThread(20, 5000);
        Thread c3 = createClientThread(30, 3000);
        Thread c4 = createClientThread(40, 0);
        Thread c5 = createClientThread(50, 1000);

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
