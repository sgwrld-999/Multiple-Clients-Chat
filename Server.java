import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket serverSocket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    // Starts the server to listen for connections
    public void startServer() {
        try {
            // Continuously listen for incoming client connections
            while (!serverSocket.isClosed()) {
                // Accept incoming client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("A new client has connected!");
                
                // Create a new ClientHandler to manage this client connection
                ClientHandler clientHandler = new ClientHandler(clientSocket);

                // Start a new thread for the client handler
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            closeServerSocket();
        }
    }

    // Closes the server socket
    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // Start the server on port 1234
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}
