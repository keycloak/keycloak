package org.keycloak.performance.util;

import org.jboss.logging.Logger;

/**
 *
 * @author tkyjovsk
 */
public interface Loggable {
    
    default public Logger logger() {
        return Logger.getLogger(this.getClass());
    }
    
    default public void info(String message) {
        logger().info(message);
    }
    
    default public void debug(String message) {
        logger().debug(message);
    }
    
    default public void warn(String message) {
        logger().warn(message);
    }
    
}
