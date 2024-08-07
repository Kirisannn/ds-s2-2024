// Server side program for Calculator RMI application
package com.example.calculator;

import java.net.MalformedURLException;
import java.rmi.*;

public class CalculatorServer {

    public static void main(String[] args) {
        try {
            // Create a new CalculatorImplementation object instance
            CalculatorImplementation calc = new CalculatorImplementation();

            // Create & export the remote registry instance on port 1100
            java.rmi.registry.LocateRegistry.createRegistry(1100);

            // Bind this object instance to the name "CalculatorServer"
            Naming.rebind("rmi://localhost:1100/CalculatorServer", calc);

            System.out.println("CalculatorServer is running...");

        } catch (MalformedURLException | RemoteException e) {
            System.err.println("Server Exception: " + e.toString());
        }
    }
}
