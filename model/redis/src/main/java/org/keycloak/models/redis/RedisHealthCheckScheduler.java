/*
 * Copyright 2026 Capital One Financial Corporation and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.redis;

import org.jboss.logging.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Automated health check scheduler for Redis connections.
 * Runs periodic health checks in a background thread and attempts automatic reconnection
 * when Redis becomes unhealthy.
 * 
 * This scheduler follows production best practices:
 * - Single daemon thread for minimal resource overhead
 * - Configurable check interval
 * - Graceful shutdown support
 * - Exception isolation (failures don't stop the scheduler)
 * - Automatic reconnection on health check failures
 */
public class RedisHealthCheckScheduler {
    private static final Logger logger = Logger.getLogger(RedisHealthCheckScheduler.class);
    
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduledTask;
    
    public RedisHealthCheckScheduler() {
        // Single daemon thread - won't prevent JVM shutdown
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "redis-health-check");
            t.setDaemon(true); // Daemon thread won't block JVM shutdown
            return t;
        });
    }
    
    /**
     * Start automated health checking.
     *
     * @param provider The Redis provider to monitor
     * @param intervalMs Health check interval in milliseconds
     */
    public void start(DefaultRedisConnectionProvider provider, int intervalMs) {
        if (running.compareAndSet(false, true)) {
            logger.infof("Starting Redis health check scheduler (interval=%dms)", intervalMs);
            
            scheduledTask = scheduler.scheduleWithFixedDelay(
                () -> performHealthCheck(provider),
                intervalMs, // Initial delay
                intervalMs, // Period
                TimeUnit.MILLISECONDS
            );
        } else {
            logger.warn("Health check scheduler is already running");
        }
    }
    
    /**
     * Stop the health check scheduler.
     * Allows currently running health checks to complete.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping Redis health check scheduler");
            
            if (scheduledTask != null) {
                scheduledTask.cancel(false); // Don't interrupt running task
            }
            
            scheduler.shutdown();
            
            try {
                // Wait up to 5 seconds for graceful shutdown
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Health check scheduler did not terminate gracefully, forcing shutdown");
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
            
            logger.info("Health check scheduler stopped");
        }
    }
    
    /**
     * Check if the scheduler is currently running.
     */
    boolean isRunning() {
        return running.get();
    }
    
    /**
     * Perform a health check and attempt reconnection if needed.
     * Isolated in try-catch to prevent scheduler thread from dying.
     */
    private void performHealthCheck(DefaultRedisConnectionProvider provider) {
        try {
            boolean healthy = provider.ping();
            
            if (!healthy) {
                logger.warn("Redis health check failed, attempting reconnection...");
                
                boolean reconnected = provider.reconnect();
                
                if (reconnected) {
                    logger.info("Redis reconnection successful");
                } else {
                    logger.error("Redis reconnection failed, will retry on next health check");
                }
            } else {
                // Only log at debug level when healthy to reduce log noise
                if (logger.isDebugEnabled()) {
                    logger.debugf("Redis health check passed");
                }
            }
        } catch (Exception e) {
            // Catch all exceptions to prevent scheduler thread from dying
            logger.errorf(e, "Unexpected error during Redis health check");
        }
    }
}
