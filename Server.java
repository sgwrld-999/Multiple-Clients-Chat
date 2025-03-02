import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private static final int DEFAULT_PORT = 1234;
    private static final int BUFFER_SIZE = 1024;
    private final ConnectionManager connectionManager;
    private final ExecutorService executorService;
    private volatile boolean running = true;

    public Server() {
        this.connectionManager = new ConnectionManager();
        this.executorService = new AdaptiveThreadPool(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 11,
            100
        ).getExecutor();
    }

    public void startServer(int port) {
        try (Selector selector = Selector.open();
             ServerSocketChannel serverChannel = ServerSocketChannel.open()) {

            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            logger.info("Server started on port " + port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown initiated. Closing server...");
                running = false;
                try {
                    selector.close();
                    serverChannel.close();
                    executorService.shutdown();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error closing server", e);
                }
                logger.info("Server shutdown complete.");
            }));

            while (running) {
                selector.select(); // Wait for an event
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        acceptClient(selector, serverChannel);
                    } else if (key.isReadable()) {
                        handleClientMessage(selector, key);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Server encountered an error", e);
        }
    }

    private void acceptClient(Selector selector, ServerSocketChannel serverChannel) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel == null) return;

        String clientId = clientChannel.socket().getRemoteSocketAddress().toString();
        if (!connectionManager.registerClient(clientId, new ConnectionManager.ClientMetadata(clientId))) {
            logger.warning("Connection limit reached. Rejecting new client: " + clientId);
            clientChannel.close();
            return;
        }

        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        logger.info("New client connected: " + clientId);
    }

    private void handleClientMessage(Selector selector, SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        int bytesRead = clientChannel.read(buffer);
        if (bytesRead == -1) {
            clientChannel.close();
            String clientId = clientChannel.socket().getRemoteSocketAddress().toString();
            connectionManager.unregisterClient(clientId);
            logger.info("Client disconnected: " + clientId);
        } else {
            buffer.flip();
            String message = new String(buffer.array(), 0, bytesRead);
            logger.info("Received message from client: " + message);

            executorService.submit(() -> {
                try {
                    broadcastMessage(selector, clientChannel, message);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error broadcasting message", e);
                }
            });
        }
    }

    private void broadcastMessage(Selector selector, SocketChannel sender, String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());

        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel && key.channel() != sender) {
                ((SocketChannel) key.channel()).write(buffer);
                buffer.rewind();  // Reset buffer for the next client
            }
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.warning("Invalid port number. Using default port " + DEFAULT_PORT);
            }
        }

        new Server().startServer(port);
    }
}