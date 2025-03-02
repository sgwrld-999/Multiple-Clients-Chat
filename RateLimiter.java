import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {
    private static final int MAX_MESSAGES_PER_SECOND = 5;
    private static final long RESET_INTERVAL = 1000; // 1 second

    private final ConcurrentHashMap<String, AtomicInteger> messageCounts = new ConcurrentHashMap<>();

    public boolean isAllowed(String clientId) {
        messageCounts.putIfAbsent(clientId, new AtomicInteger(0));

        int count = messageCounts.get(clientId).incrementAndGet();
        if (count > MAX_MESSAGES_PER_SECOND) {
            return false; // Rate limit exceeded
        }

        // Reset counter every second
        new Thread(() -> {
            try {
                Thread.sleep(RESET_INTERVAL);
                messageCounts.get(clientId).set(0);
            } catch (InterruptedException ignored) {}
        }).start();

        return true;
    }
}
