

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utility {
    private static final Logger logger = Logger.getLogger(Utility.class.getName());
    
    public static void closeResources(Closeable... resources) {
        for (Closeable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error closing resource", e);
                }
            }
        }
    }
}
