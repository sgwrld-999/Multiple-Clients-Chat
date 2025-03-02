import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static final int PORT = 1234;
    private static final int MAX_CONNECTIONS = 100; // Limit simultaneous connections
    private static final String KEYSTORE_FILE = "keystore.jks";
    private static final String KEYSTORE_PASSWORD = "changeit";

    private final ConnectionManager connectionManager;
    private final ExecutorService executorService;
    private volatile boolean running = true;
    private static final AtomicInteger activeConnections = new AtomicInteger(0);
    private final RateLimiter rateLimiter = new RateLimiter();

    public Server() {
        this.connectionManager = new ConnectionManager();
        this.executorService = new AdaptiveThreadPool(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 11,
            100
        ).getExecutor();
    }

    public void startServer() {
        try {
            // Load the keystore
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream keyStoreStream = new FileInputStream(KEYSTORE_FILE)) {
                keyStore.load(keyStoreStream, KEYSTORE_PASSWORD.toCharArray());
            }

            // Initialize KeyManagerFactory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

            // Initialize SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            // Create SSLServerSocket
            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(PORT);

            logger.info("Secure Chat Server started on port " + PORT);

            while (running) {
                if (activeConnections.get() >= MAX_CONNECTIONS) {
                    logger.warning("Max connections reached. Rejecting new clients.");
                    Thread.sleep(500); // Prevent excessive CPU usage
                    continue;
                }

                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                activeConnections.incrementAndGet();
                logger.info("New client connected. Active connections: " + activeConnections.get());

                new Thread(new ClientHandler(clientSocket, this)).start();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in secure server", e);
        }
    }

    // Called when a client disconnects
    public void decreaseActiveConnections() {
        activeConnections.decrementAndGet();
        logger.info("Client disconnected. Active connections: " + activeConnections.get());
    }

    // Provide access to rateLimiter for ClientHandler
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public static void main(String[] args) {
        new Server().startServer();
    }
}
