import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {
    // A static list to keep track of all connected clients
    private static ArrayList<ClientHandler> clients = new ArrayList<>();
    private Socket clientSocket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientName;

    public ClientHandler(Socket clientSocket) {
        try {
            this.clientSocket = clientSocket;
            // Convert byte stream to character stream for output
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            // Read messages sent by the client
            this.bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // Get the client's name from the first line sent by the client
            this.clientName = bufferedReader.readLine();
            clients.add(this); // Add the new client handler to the list
            broadcastMessage("Server: " + clientName + " has joined the chat");
        } catch (IOException e) {
            // Close resources if an error occurs during initialization
            closeEverything(clientSocket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    // This method will run in a separate thread because listening for messages from clients
    // is a blocking operation. Running it in the main thread would block the server from
    // accepting new clients.
    public void run() {
        String messageFromClient;
        try {
            // Keep listening for messages from the client while the connection is active
            while (clientSocket.isConnected()) {
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient != null) {
                    broadcastMessage(clientName + ": " + messageFromClient);
                } else {
                    break; // If no message, client might have disconnected
                }
            }
        } catch (IOException e) {
            closeEverything(clientSocket, bufferedReader, bufferedWriter);
        }
    }

    // Broadcasts a message to all connected clients
    public void broadcastMessage(String messageToSend) {
        for (ClientHandler clientHandler : clients) {
            try {
                // Don't send the message to the client who sent it
                if (!clientHandler.clientName.equals(clientName)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(clientSocket, bufferedReader, bufferedWriter);
            }
        }
    }

    // Removes a client from the list and closes all resources
    public void removeClientHandler() {
        clients.remove(this);
        broadcastMessage("Server: " + clientName + " has left the chat");
    }

    // Closes the socket and streams
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler(); // Ensure the client is removed from the list
        try {
            if (socket != null) {
                socket.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
