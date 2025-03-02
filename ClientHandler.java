import java.io.*;
import javax.net.ssl.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private final SSLSocket clientSocket;
    private final Server server;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    public ClientHandler(SSLSocket socket, Server server) {
        this.clientSocket = socket;
        this.server = server;
        try {
            this.bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (IOException e) {
            closeEverything();
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = bufferedReader.readLine()) != null) {
                logger.info("Message from client: " + message);
                
                // Rate limiting check
                if (!server.getRateLimiter().isAllowed(clientSocket.getInetAddress().toString())) {
                    bufferedWriter.write("SERVER: Too many messages, slow down.");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    continue;
                }
                broadcastMessage(message);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Client disconnected abruptly", e);
        } finally {
            closeEverything();
            server.decreaseActiveConnections();  // Update active connections on disconnect
        }
    }

    private void broadcastMessage(String message) {
        System.out.println("Broadcasting: " + message);
        // Add logic to send message to all connected clients
    }

    public void sendMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.out.println("Error sending message: " + e.getMessage());
        }
    }

    private void closeEverything() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error closing resources", e);
        }
    }
}
