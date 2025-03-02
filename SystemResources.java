public class SystemResources {
    private final double cpuUsage;
    private final double memoryUsage;

    public SystemResources(double cpuUsage, double memoryUsage) {
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public double getMemoryUsage() {
        return memoryUsage;
    }

    @Override
    public String toString() {
        return "CPU Usage: " + (cpuUsage * 100) + "%, Memory Usage: " + (memoryUsage * 100) + "%";
    }
}
