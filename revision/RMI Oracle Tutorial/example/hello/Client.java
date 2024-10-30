package example.hello;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private Client() {
    }

    public static void main(String[] args) {
        String host = (args.length < 1) ? null : args[0];
        try {
            // Obtain the registry on the specified host (or localhost if none is specified)
            Registry registry = LocateRegistry.getRegistry(host);

            // Look up the "Hello" remote object's stub in the registry
            Hello stub = (Hello) registry.lookup("Hello");
            
            // Invoke the remote method
            String response = stub.sayHello();
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}