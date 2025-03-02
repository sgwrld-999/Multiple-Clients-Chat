import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.logging.Level;



public class ConnectionManager {
    private static final Logger logger = Logger.getLogger(ConnectionManager.class.getName());
    
    private static final int INITIAL_CONNECTION_LIMIT = 500;
    private static final double HIGH_LOAD_THRESHOLD = 0.85; // 85% of max
    private static final double LOW_LOAD_THRESHOLD = 0.40;  // 40% of max
    
    private final AtomicInteger currentConnections = new AtomicInteger(0);
    private final ConcurrentHashMap<String, ClientMetadata> clientRegistry = new ConcurrentHashMap<>();
    private volatile int connectionLimit;
    
    private final SystemResourceMonitor resourceMonitor;
    
    public ConnectionManager() {
        this.connectionLimit = calculateOptimalConnectionLimit();
        this.resourceMonitor = new SystemResourceMonitor();
        startResourceMonitoring();
    }
    
    private int calculateOptimalConnectionLimit() {
        int processors = Runtime.getRuntime().availableProcessors();
        long maxMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        int memoryBasedLimit = (int) (maxMemoryMB / 2);
        int processorBasedLimit = processors * 50;
        
        int calculatedLimit = Math.min(memoryBasedLimit, processorBasedLimit);
        calculatedLimit = Math.max(100, Math.min(10000, calculatedLimit));
        
        logger.info("Calculated optimal connection limit: " + calculatedLimit + 
                   " (Memory: " + maxMemoryMB + "MB, Processors: " + processors + ")");
        
        return calculatedLimit;
    }
    
    private void startResourceMonitoring() {
        Thread monitorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    SystemResources resources = resourceMonitor.getCurrentResources();
                    adjustConnectionLimits(resources);
                    Thread.sleep(30000); // 30 seconds between checks
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error in resource monitoring", e);
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("ResourceMonitor");
        monitorThread.start();
    }
    
    private synchronized void adjustConnectionLimits(SystemResources resources) {
        if (resources.getCpuUsage() > 0.85 || resources.getMemoryUsage() > 0.85) {
            int newLimit = (int) (connectionLimit * 0.8); // Reduce by 20%
            logger.info("High resource usage detected. Reducing connection limit to " + newLimit);
            connectionLimit = Math.max(50, newLimit); // Don't go below 50
        } else if (resources.getCpuUsage() < 0.50 && resources.getMemoryUsage() < 0.60 && 
                currentConnections.get() > connectionLimit * HIGH_LOAD_THRESHOLD) {
            int newLimit = (int) (connectionLimit * 1.2); // Increase by 20%
            int maxAllowed = calculateOptimalConnectionLimit();
            connectionLimit = Math.min(newLimit, maxAllowed);
            logger.info("Low resource usage detected. Increasing connection limit to " + connectionLimit);
        }
    }
    
    public synchronized boolean registerClient(String clientId, ClientMetadata metadata) {
        if (currentConnections.get() >= connectionLimit) {
            logger.warning("Connection limit reached. Rejecting new client: " + clientId);
            return false;
        }
        
        if (clientRegistry.containsKey(clientId)) {
            logger.warning("Client already registered: " + clientId);
            return false;
        }
        
        clientRegistry.put(clientId, metadata);
        currentConnections.incrementAndGet();
        logger.info("Client registered: " + clientId + " | Active connections: " + 
                   currentConnections.get() + "/" + connectionLimit);
        
        return true;
    }
    
    public synchronized void unregisterClient(String clientId) {
        ClientMetadata removed = clientRegistry.remove(clientId);
        if (removed != null) {
            currentConnections.decrementAndGet();
            logger.info("Client unregistered: " + clientId + " | Active connections: " + 
                       currentConnections.get() + "/" + connectionLimit);
        } else {
            logger.warning("Client not found: " + clientId);
        }
    }
    
    public int getConnectionLimit() {
        return connectionLimit;
    }
    
    public int getCurrentConnections() {
        return currentConnections.get();
    }
    
    public double getLoadFactor() {
        return (double) currentConnections.get() / connectionLimit;
    }
    
    public void forEachClient(Consumer<ClientMetadata> action) {
        clientRegistry.values().forEach(action);
    }
    
    public static class ClientMetadata {
        private final String username;
        private final long connectionTime;
        private volatile long lastActivity;
        
        public ClientMetadata(String username) {
            this.username = username;
            this.connectionTime = System.currentTimeMillis();
            this.lastActivity = connectionTime;
        }
        
        public String getUsername() {
            return username;
        }
        
        public long getConnectionTime() {
            return connectionTime;
        }
        
        public long getLastActivity() {
            return lastActivity;
        }
        
        public void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
    }
}