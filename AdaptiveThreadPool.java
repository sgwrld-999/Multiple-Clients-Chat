import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An adaptive thread pool that scales based on current load and system resources.
 */
public class AdaptiveThreadPool {
    private static final Logger logger = Logger.getLogger(AdaptiveThreadPool.class.getName());
    
    // Thread pool monitoring interval in seconds
    private static final int MONITORING_INTERVAL = 15;
    
    // Thresholds for scaling decisions
    private static final double HIGH_LOAD_THRESHOLD = 0.7; // 70% of queue capacity
    private static final double LOW_LOAD_THRESHOLD = 0.2;  // 20% of queue capacity
    
    // Thread pool parameters
    private final int corePoolSize;
    private final int maximumPoolSize;
    private final int queueCapacity;
    
    // The thread pool executor
    private final ThreadPoolExecutor executor;
    
    // System resource monitor
    private final SystemResourceMonitor resourceMonitor;
    
    /**
     * Create a new adaptive thread pool.
     * 
     * @param initialCorePoolSize Initial number of core threads
     * @param initialMaxPoolSize Initial maximum number of threads
     * @param initialQueueSize Initial work queue capacity
     */
    public AdaptiveThreadPool(int initialCorePoolSize, int initialMaxPoolSize, int initialQueueSize) {
        this.corePoolSize = initialCorePoolSize;
        this.maximumPoolSize = calculateMaxThreads(initialMaxPoolSize);
        this.queueCapacity = initialQueueSize;
        
        // Create a bounded queue to allow backpressure sensing
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(queueCapacity);
        
        // Create the thread pool with a custom rejection policy
        this.executor = new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            60L, TimeUnit.SECONDS,
            workQueue,
            new ThreadPoolExecutor.CallerRunsPolicy() // If queue is full, caller thread executes task
        );
        
        // Allow core threads to timeout if idle
        executor.allowCoreThreadTimeOut(true);
        
        // Initialize resource monitor
        this.resourceMonitor = new SystemResourceMonitor();
        
        // Start monitoring thread
        startMonitoring();
        
        logger.info("Adaptive thread pool created: coreSize=" + corePoolSize + 
                   ", maxSize=" + maximumPoolSize + ", queueCapacity=" + queueCapacity);
    }
    
    /**
     * Calculate the maximum number of threads based on system resources.
     */
    private int calculateMaxThreads(int suggestedMax) {
        // Get available processors (cores)
        int processors = Runtime.getRuntime().availableProcessors();
        
        // A common formula is processor count * (1 + wait time / service time)
        // For I/O bound work like chat, wait/service ratio might be high (e.g., 10)
        int calculatedMax = processors * 11; // 1 + 10 for I/O waiting
        
        // Choose the smaller of our calculation and the suggested max
        int result = Math.min(calculatedMax, suggestedMax);
        
        // Ensure we have at least a minimum number of threads
        return Math.max(16, result);
    }
    
    /**
     * Submit a task to the thread pool.
     */
    public void submit(Runnable task) {
        executor.submit(task);
    }
    
    /**
     * Submit a task with a timeout.
     * 
     * @return A Future representing the result of the task
     */
    public <T> Future<T> submit(Callable<T> task, long timeout, TimeUnit unit) {
        Future<T> future = executor.submit(task);
        
        // Schedule a timeout for the task
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            if (!future.isDone()) {
                future.cancel(true);
            }
            scheduler.shutdown();
        }, timeout, unit);
        
        return future;
    }
    
    /**
     * Start a monitoring thread to adjust thread pool parameters based on load.
     */
    private void startMonitoring() {
        Thread monitorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Sleep at the beginning so we don't adjust immediately after startup
                    Thread.sleep(TimeUnit.SECONDS.toMillis(MONITORING_INTERVAL));
                    
                    // Get current metrics
                    int activeThreads = executor.getActiveCount();
                    int poolSize = executor.getPoolSize();
                    int queueSize = executor.getQueue().size();
                    double queueUtilization = (double) queueSize / queueCapacity;
                    
                    // Get system resource metrics
                    SystemResources resources = resourceMonitor.getCurrentResources();

                    
                    // Log current state
                    logger.info("Thread pool stats: active=" + activeThreads + 
                              ", size=" + poolSize + 
                              ", queue=" + queueSize + "/" + queueCapacity +
                              ", resources=" + resources);
                    
                    // Adjust thread pool based on load and resources
                    adjustThreadPool(queueUtilization, resources);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error in thread pool monitoring", e);
                }
            }
        });
        
        monitorThread.setDaemon(true);
        monitorThread.setName("ThreadPoolMonitor");
        monitorThread.start();
    }
    
    /**
     * Adjust thread pool parameters based on current load and system resources.
     */
    private void adjustThreadPool(double queueUtilization, SystemResources resources) {
        // If queue is getting full and system resources are available, increase threads
        if (queueUtilization > HIGH_LOAD_THRESHOLD && 
            resources.getCpuUsage() < 0.8 && 
            resources.getMemoryUsage() < 0.8) {
            
            int currentMax = executor.getMaximumPoolSize();
            int newMax = Math.min(currentMax + 5, maximumPoolSize);
            
            if (newMax > currentMax) {
                executor.setMaximumPoolSize(newMax);
                logger.info("Increased maximum threads to " + newMax + " due to high load");
            }
        } 
        // If queue utilization is low and we have more than core threads, decrease max threads
        else if (queueUtilization < LOW_LOAD_THRESHOLD && 
                executor.getMaximumPoolSize() > corePoolSize) {
            
            int currentMax = executor.getMaximumPoolSize();
            int newMax = Math.max(currentMax - 2, corePoolSize);
            
            if (newMax < currentMax) {
                executor.setMaximumPoolSize(newMax);
                logger.info("Decreased maximum threads to " + newMax + " due to low load");
            }
        }
    }
    
    /**
     * Shutdown the thread pool, waiting for tasks to complete.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.severe("Thread pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get current thread pool metrics.
     */
    public ThreadPoolMetrics getMetrics() {
        return new ThreadPoolMetrics(
            executor.getActiveCount(),
            executor.getPoolSize(),
            executor.getMaximumPoolSize(),
            executor.getQueue().size(),
            queueCapacity,
            executor.getCompletedTaskCount()
        );
    }
    
    /**
     * Value class to hold thread pool metrics.
     */
    public static class ThreadPoolMetrics {
        private final int activeThreads;
        private final int currentPoolSize;
        private final int maxPoolSize;
        private final int queueSize;
        private final int queueCapacity;
        private final long completedTasks;
        
        public ThreadPoolMetrics(int activeThreads, int currentPoolSize, int maxPoolSize, 
                               int queueSize, int queueCapacity, long completedTasks) {
            this.activeThreads = activeThreads;
            this.currentPoolSize = currentPoolSize;
            this.maxPoolSize = maxPoolSize;
            this.queueSize = queueSize;
            this.queueCapacity = queueCapacity;
            this.completedTasks = completedTasks;
        }
        
        public double getThreadUtilization() {
            return currentPoolSize > 0 ? (double) activeThreads / currentPoolSize : 0;
        }
        
        public double getQueueUtilization() {
            return (double) queueSize / queueCapacity;
        }
        
        @Override
        public String toString() {
            return String.format("ThreadPoolMetrics[active=%d, poolSize=%d/%d, queue=%d/%d, completed=%d]",
                               activeThreads, currentPoolSize, maxPoolSize, 
                               queueSize, queueCapacity, completedTasks);
        }
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}