import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    
    private final Socket clientSocket;
    private final BufferedReader bufferedReader;
    private final BufferedWriter bufferedWriter;
    private final String clientName;
    private final Runnable disconnectCallback;
    private static final ChatObservable chatObservable = new ChatObservable();

    public ClientHandler(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        this.clientName = "Client-" + clientSocket.getPort(); // Example client name
        this.disconnectCallback = () -> removeClientHandler(); // Example callback
        chatObservable.addObserver(this);
    }

    @Override
    public void run() {
        try {
            String messageFromClient;
            while ((messageFromClient = bufferedReader.readLine()) != null) {
                if (messageFromClient.startsWith("/")) {
                    handleCommand(messageFromClient);
                } else {
                    broadcastMessage(clientName + ": " + messageFromClient);
                }
            }
        } catch (SocketException e) {
            logger.log(Level.INFO, "Client disconnected: " + clientName, e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error handling client " + clientName, e);
        } finally {
            closeEverything();
        }
    }

    private void handleCommand(String command) throws IOException {
        if ("/exit".equalsIgnoreCase(command)) {
            throw new IOException("Client requested exit");
        } else if ("/users".equalsIgnoreCase(command)) {
            StringBuilder userList = new StringBuilder("Connected users: ");
            clients.forEach(client -> userList.append(client.clientName).append(", "));
            if (userList.length() > 16) {
                userList.setLength(userList.length() - 2);
            }
            sendMessage("SERVER: " + userList.toString());
        } else if ("/help".equalsIgnoreCase(command)) {
            sendMessage("SERVER: Available commands:\n" +
                      "/exit - Leave the chat\n" +
                      "/users - List all connected users\n" +
                      "/help - Show this help message");
        } else {
            sendMessage("SERVER: Unknown command. Type /help for available commands.");
        }
    }

    public void broadcastMessage(String message) {
        chatObservable.broadcast(message);
    }
    
    public void sendMessage(String message) throws IOException {
        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    public void removeClientHandler() {
        clients.remove(this);
        broadcastMessage("SERVER: " + clientName + " has left the chat");
        logger.info("Client left: " + clientName);
        if (disconnectCallback != null) {
            disconnectCallback.run();
        }
    }

    public void closeEverything() {
        Utility.closeResources(bufferedReader, bufferedWriter, clientSocket);
    }
    
}