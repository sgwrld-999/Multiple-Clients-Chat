import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    public Client(Socket socket, String username) {
        try {
            this.socket = socket;
            // Initialize writer and reader for communication
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    // Sends messages from the client to the server
    public void sendMessage() {
        try {
            // Send the username as the first message
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            Scanner scanner = new Scanner(System.in);
            // Continuously read from the console and send to the server
            while (socket.isConnected()) {
                String messageToSend = scanner.nextLine();
                bufferedWriter.write(messageToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    // Listens for incoming messages from the server
    public void listenForMessages() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;

                while (socket.isConnected()) {
                    try {
                        msgFromGroupChat = bufferedReader.readLine();
                        if (msgFromGroupChat != null) {
                            System.out.println(msgFromGroupChat);
                        }
                    } catch (IOException e) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }).start();
    }

    // Closes all resources and handles the cleanup process
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // Connects to the server running on localhost at port 1234
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        Socket socket = new Socket("localhost", 1234);
        Client client = new Client(socket, username);
        client.listenForMessages();
        client.sendMessage();
    }
}
