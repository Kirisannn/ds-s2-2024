# Java RMI Tutorial

## 1. Defining the Remote Interface

A remote object in Java RMI (Remote Method Invocation) is an instance of a class that implements a remote interface. This remote interface has specific requirements:

- Extends the `java.rmi.Remote` interface.
- Declares remote methods that can be called from another JVM.
- Throws RemoteException: Each remote method must include `java.rmi.RemoteException` (or a superclass of `RemoteException`) in its `throws` clause to handle potential network-related or server issues.

### Example: Remote Interface Definition

The following code defines a remote interface called `Hello`, which has one method `sayHello` that returns a string.

```
package example.hello;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Hello extends Remote {
    String sayHello() throws RemoteException;
}
```

#### Explanation
- **The Hello interface**:

    - Extends `Remote`, marking it as a remote interface.
    - Declares `sayHello`, a remote method that clients can call from different JVMs.
    - Throws `RemoteException` to handle errors specific to remote invocations (e.g., network issues, server unavailability).

## 2. Implementing the Server

In Java RMI, the **server class** is responsible for creating and registering a remote object. This class contains the `main` method that:

1. **Creates and exports** an instance of the remote object.
2. **Registers the remote object** in the Java RMI registry.

The server can either be the same class as the implementation of the remote interface or a separate class entirely.

### Example: Server Implementation

In this example, the `Server` class implements the `Hello` remote interface, providing the `sayHello` method and managing the setup and registry of the remote object.

```
package example.hello;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Server implements Hello {
    
    public Server() {}

    @Override
    public String sayHello() {
        return "Hello, world!";
    }

    public static void main(String[] args) {
        try {
            // Create and export the remote object
            Server obj = new Server();
            Hello stub = (Hello) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("Hello", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
```

#### Explanation

- **Server Class**:

    - Implements `Hello` to provide an implementation for the `sayHello` remote method.
    - In `main`:
        - Creates a `Server` instance and exports it using `UnicastRemoteObject.exportObject`.
         - Retrieves the RMI registry and binds the remote object’s stub to the name `"Hello"`.
        - Prints "Server ready" if the server is set up successfully, or logs an error if an exception occurs.

#### Notes

- **Exception Handling**: `sayHello` does not declare any exceptions, as the implementation itself doesn’t throw `RemoteException` or other checked exceptions.
- **Additional Methods**: Methods not specified in the remote interface (like helper methods) can be defined but are accessible only within the server's JVM, not remotely.
---
### Creating and Exporting a Remote Object

The **main method** in the server class must create the remote object that provides the service. Additionally, it must **export** the remote object to the Java RMI runtime to enable it to handle incoming remote calls. This setup is accomplished with the following steps:

```
Server obj = new Server();
Hello stub = (Hello) UnicastRemoteObject.exportObject(obj, 0);
```

- **Creating the Remote Object**:
    - `Server obj = new Server();` creates an instance of the server that implements the `Hello` interface, thereby providing the actual service.

- **Exporting the Remote Object**:
    - The `UnicastRemoteObject.exportObject` method exports the remote object (`obj`) so that it can receive remote method calls.
    - It takes two parameters: the remote object itself and the port number (`0` for an anonymous port).
    - The `exportObject` method returns a **stub** for the remote object, which clients use to call remote methods. The stub:
        - Implements the same remote interfaces as the original remote object.
        - Contains details about the host name and port where the remote object can be contacted.

#### Important Notes

- **Dynamic Stub Generation**:
    - Since J2SE 5.0, the RMI framework can dynamically generate stubs for remote objects, making pre-generation with `rmic` unnecessary in most cases.
    - **Backward Compatibility**: If your application needs to support clients using pre-5.0 JVMs, you must pre-generate stub classes with `rmic` and deploy them for client download.
---
### Registering the Remote Object with the Java RMI Registry

For clients to invoke methods on a remote object, they first need to obtain a stub for the remote object. The Java RMI framework provides a **registry** API to facilitate this:

1. **Bind a name** to the remote object's stub in the registry.
2. **Look up** remote objects by name in the registry to retrieve the stub and invoke methods on the object.

A **Java RMI registry** is a simplified naming service that provides a reference (stub) for remote objects, typically used for locating the first remote object a client needs. Subsequent references to additional remote objects can then be managed by that initial object.

#### Code Example: Binding the Remote Object in the Registry

In the server, we bind the remote object’s stub in the registry with the name `"Hello"`:

```
Registry registry = LocateRegistry.getRegistry();
registry.bind("Hello", stub);
```

- **LocateRegistry.getRegistry()**:

    - Retrieves a stub for the registry on the **local host** and the **default port** (1099).
    - This stub implements the `java.rmi.registry.Registry` interface and allows the server to bind names to remote objects in the registry.

- **registry.bind("Hello", stub)**:

    - Binds the name `"Hello"` to the remote object’s stub in the registry.
    - Once bound, clients can use the name `"Hello"` to look up this remote object in the registry and retrieve a stub to interact with it.

#### Important Notes

- **Registry Availability**:
    - The `LocateRegistry.getRegistry()` method only returns a stub and does not check if the registry is actually running.
    - If a registry is not running on TCP port 1099 on the local host, the `bind` call will fail with a `RemoteException`.

## 3. Implementing the Client

The **client program** in Java RMI connects to the server, retrieves the remote object’s stub from the RMI registry, and invokes remote methods using this stub.

### Code Example: Client Implementation

The following client code connects to the registry, retrieves the `Hello` remote object’s stub, and calls the `sayHello` method:

```
package example.hello;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private Client() {}

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
```

#### Explanation

1. **Registry Connection**:
    - `LocateRegistry.getRegistry(host)`: Retrieves the registry’s stub on the specified host. If no host is provided, `null` defaults to the local host.

2. **Stub Lookup**:
    - `registry.lookup("Hello")`: Looks up the `"Hello"` remote object’s stub in the registry by name.

3. **Remote Method Invocation**:

- `stub.sayHello()`: Invokes `sayHello` on the remote object. This triggers the following sequence:
    - The **client-side RMI** runtime opens a connection to the server using the stub’s host and port, serializes the call data, and sends it to the server.
    - The **server-side RMI runtime** receives the call, dispatches it to the remote object, and serializes the result (e.g., `"Hello, world!"`) back to the client.
    - The **client-side runtime** receives the result, deserializes it, and returns it to the caller.

4. **Output**:

    - The result of the remote method invocation (`"Hello, world!"`) is printed to `System.out`.

## 4. Compiling the Source Files

To compile the source files for the Java RMI example, use the following command:

```
javac -d <destDir> Hello.java Server.java Client.java
```

- `destDir`: Replace this with the destination directory where you want to place the compiled `.class` files.

### Important Note on Stub Classes

If the server needs to support clients using **pre-5.0 JVMs**, a **stub class** must be pregenerated for the remote object implementation. Use the `rmic` compiler to generate this stub:

```
rmic Server
```

The generated stub class should then be made available for clients to download.

## 5. Running the Java RMI Registry, Server, and Client

To run the Java RMI example, follow these steps:
1. **Starting the Java RMI Registry**

    To start the registry, use the `rmiregistry` command on the server’s host. This command runs the registry on the default TCP port (1099) and produces no output if successful.

    ### Examples:

    - **On Unix/Linux (background process)**:
	```
	rmiregistry &
	```
    - **On Windows**:
	```
	start rmiregistry
	```

    ### Running on Different Port
    To run the registry on a different port (e.g., 2001), specify the port number:
    ```
    start rmiregistry 2001
    ```
    
    #### Note:
    - If the registry runs on a non-default port, modify `LocateRegistry.getRegistry` in both the `Server` and `Client` classes to specify this port. For example:
	```
	Registry registry = LocateRegistry.getRegistry(2001);
	```

2. **Starting the Server**

    To start the server, run the `Server` class using the `java` command.

    ### Examples:

    - **On Unix/Linux (background process)**:
	```
	java -classpath <classDir> example.hello.Server &
	```
    - **On Windows**:
	```
	start java -classpath classDir example.hello.Server
	```

    Replace `classDir` with the path to the root directory of your compiled class files. When started successfully, the server will display:

    ```
    Server ready
    ```

    The server will keep running until manually terminated.

3. **Run the Client**

    Once the server is ready, run the client with the following command:
    ```
    java -classpath <classDir> example.hello.Client
    ```

    Replace `classDir` with the root directory of your class files. If successful, the client will print:
    ```
    response: Hello, world!
    ```