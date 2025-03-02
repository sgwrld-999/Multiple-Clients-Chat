import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.logging.Logger;


public class SystemResourceMonitor {
    private static final Logger logger = Logger.getLogger(SystemResourceMonitor.class.getName());

    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;

    public SystemResourceMonitor() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
    }

    public SystemResources getCurrentResources() {
        double cpuLoad = osBean.getSystemLoadAverage();
        if (cpuLoad < 0) {  
            cpuLoad = 0.5;  // Default value for unsupported platforms
        }

        double normalizedCpuLoad = cpuLoad / osBean.getAvailableProcessors();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsage = (double) usedMemory / maxMemory;

        return new SystemResources(normalizedCpuLoad, memoryUsage);
    }
}
