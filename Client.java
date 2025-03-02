import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private static final int DEFAULT_PORT = 1234;
    private static final String DEFAULT_HOST = "localhost";
    
    private final Socket socket;
    private final BufferedReader bufferedReader;
    private final BufferedWriter bufferedWriter;
    private final String username;
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * Constructor for the Client class
     *
     * @param socket Socket connection to the server
     * @param username Client's username
     * @throws IOException If an I/O error occurs
     */
    public Client(Socket socket, String username) throws IOException {
        this.socket = socket;
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.username = username;
    }

    /**
     * Sends messages from the client to the server
     */
    public void sendMessage() {
        try (Scanner scanner = new Scanner(System.in)) {
            // Send the username as the first message
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            
            logger.info("Successfully connected to the chat server");
            System.out.println("==== Welcome to the chat ====");
            System.out.println("Type your message and press Enter to send");
            System.out.println("Type '/exit' to leave the chat");
            
            // Continuously read from the console and send to the server
            while (running.get() && socket.isConnected()) {
                String messageToSend = scanner.nextLine();
                
                // Handle exit command
                if ("/exit".equalsIgnoreCase(messageToSend)) {
                    running.set(false);
                    System.out.println("Exiting chat...");
                    break;
                }
                
                bufferedWriter.write(messageToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } catch (SocketException e) {
            logger.log(Level.WARNING, "Connection to server lost", e);
            System.out.println("Connection to server lost. Please restart the client.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while sending message", e);
            System.out.println("Error communicating with server: " + e.getMessage());
        } finally {
            closeEverything();
        }
    }

    /**
     * Listens for incoming messages from the server in a separate thread
     */
    public void listenForMessages() {
        Thread messageListener = new Thread(() -> {
            try {
                while (running.get() && socket.isConnected()) {
                    String msgFromServer = bufferedReader.readLine();
                    if (msgFromServer == null) {
                        System.out.println("Server has closed the connection.");
                        running.set(false);
                        break;
                    }
                    System.out.println(msgFromServer);
                }
            } catch (SocketException e) {
                if (running.get()) {
                    logger.log(Level.WARNING, "Connection to server lost", e);
                    System.out.println("Connection to server lost.");
                }
            } catch (IOException e) {
                if (running.get()) {
                    logger.log(Level.SEVERE, "Error while receiving message", e);
                    System.out.println("Error reading messages from server: " + e.getMessage());
                }
            } finally {
                if (running.get()) {
                    closeEverything();
                }
            }
        });
        messageListener.setDaemon(true);
        messageListener.start();
    }

    
    public void closeEverything() {
        Utility.closeResources(bufferedReader, bufferedWriter, socket);
    }
    

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        
        // Optional command-line arguments for host and port
        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Using default port " + DEFAULT_PORT);
            }
        }
        
        System.out.println("==== Chat Client ====");
        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();
        
        // Validate username
        while (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            System.out.print("Enter your username: ");
            username = scanner.nextLine().trim();
        }
        
        try {
            System.out.println("Connecting to " + host + ":" + port + "...");
            Socket socket = new Socket(host, port);
            Client client = new Client(socket, username);
            client.listenForMessages();
            client.sendMessage();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to connect to server", e);
            System.out.println("Failed to connect to the server: " + e.getMessage());
            System.out.println("Please check if the server is running and try again.");
        }
    }
}