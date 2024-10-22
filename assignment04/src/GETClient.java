
import java.io.*;
import java.net.*;

public class GETClient {
    public static void main(String[] args) {
        // Read arguments
        int port = 0;
        String host = null;
        if (args.length > 0) {
            // If host_port doesn't start with "http://"
            if (!args[0].startsWith("http://")) {
                // Get the host and port from the host_port
                String[] host_port = getHostPort(args[0]);
                host = host_port[0];
                port = Integer.parseInt(host_port[1]);
            } else {
                // Get the host and port from the host_port
                String[] host_port = getHostPort(args[0].substring(7));
                // Host from index 6 to the end
                host = host_port[0];
                port = Integer.parseInt(host_port[1]);
            }

            if (port < 0 || port > 65535) {
                System.err.println("Invalid port number (Out of range 0-65535)");
                System.exit(1);
            }

        }

        // If stationID provided
        if (args.length > 1) {
            String stationID = args[1];

            System.out.print("""
                    ---------------------------------------------------------------------------
                    Updating Station: """ + stationID + ", ");
        }
        System.out.println("Connecting to { " + host + ":" + port + " }");

        System.out.println("---------------------------------------------------------------------------");

        // Create client socket
        try (Socket client_socket = new Socket(host, port)) {
            System.out.println("Successfully connected to server: { " + host + ":" + port + " }\n");

            OutputStream output = client_socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            InputStream input = client_socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            // Recieve message from server
            String serverResponse = reader.readLine();
            System.out.println("Server Response: " + serverResponse);
        } catch (UnknownHostException e) {
            System.err.println("Could not connect to server: " + host + ":" + port + ". Unknown host\n" + e);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Could not connect to server: " + host + ":" + port + ".\n" + e);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Could not connect to server. Unknown error\n" + e);
            System.exit(1);
        }

    }

    static String[] getHostPort(String host_port) {
        String[] host_port_arr = host_port.split(":");
        return host_port_arr;
    }

    // static String formatMessage() {

    // }
}
