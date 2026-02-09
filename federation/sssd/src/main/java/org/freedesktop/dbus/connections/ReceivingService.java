package org.freedesktop.dbus.connections;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfigBuilder;
import org.freedesktop.dbus.exceptions.IllegalThreadPoolStateException;
import org.freedesktop.dbus.utils.NameableThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service providing threads for every type of message expected to be received by DBus.
 *
 * @author hypfvieh
 * @version 4.1.0 - 2022-02-02
 */
public class ReceivingService {
    static final int MAX_RETRIES = 50;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean closed = false;

    private final Map<ExecutorNames, ExecutorService> executors = new ConcurrentHashMap<>();

    private final IThreadPoolRetryHandler retryHandler;

    /**
     * Creates a new instance.
     *
     * @param _rsCfg configuration
     */
    ReceivingService(ReceivingServiceConfig _rsCfg) {
        ReceivingServiceConfig rsCfg = Optional.ofNullable(_rsCfg).orElse(ReceivingServiceConfigBuilder.getDefaultConfig());
        executors.put(ExecutorNames.SIGNAL,
                Executors.newFixedThreadPool(rsCfg.getSignalThreadPoolSize(), new NameableThreadFactory("DBus-Signal-Receiver-", true, rsCfg.getSignalThreadPriority())));
        executors.put(ExecutorNames.ERROR,
                Executors.newFixedThreadPool(rsCfg.getErrorThreadPoolSize(), new NameableThreadFactory("DBus-Error-Receiver-", true, rsCfg.getErrorThreadPriority())));

        // we need multiple threads here so recursive method calls are possible
        executors.put(ExecutorNames.METHODCALL,
                Executors.newFixedThreadPool(rsCfg.getMethodCallThreadPoolSize(), new NameableThreadFactory("DBus-MethodCall-Receiver-", true, rsCfg.getMethodCallThreadPriority())));
        executors.put(ExecutorNames.METHODRETURN,
                Executors.newFixedThreadPool(rsCfg.getMethodReturnThreadPoolSize(), new NameableThreadFactory("DBus-MethodReturn-Receiver-", true, rsCfg.getMethodReturnThreadPriority())));

        retryHandler = rsCfg.getRetryHandler();
    }

    /**
     * Execute a runnable which handles a signal.
     *
     * @param _r runnable
     *
     * @return retries, if any input was null -1 is returned
     */
    int execSignalHandler(Runnable _r) {
        return execOrFail(ExecutorNames.SIGNAL, _r);
    }

    /**
     * Execute a runnable which handles an error.
     *
     * @param _r runnable
     *
     * @return retries, if any input was null -1 is returned
     */
    int execErrorHandler(Runnable _r) {
        return execOrFail(ExecutorNames.ERROR, _r);
    }

    /**
     * Execute a runnable which handles a method call.
     *
     * @param _r runnable
     *
     * @return retries, if any input was null -1 is returned
     */
    int execMethodCallHandler(Runnable _r) {
       return execOrFail(ExecutorNames.METHODCALL, _r);
    }

    /**
     * Execute a runnable which handles the return of a method.
     *
     * @param _r runnable
     *
     * @return retries, if any input was null -1 is returned
     */
    int execMethodReturnHandler(Runnable _r) {
        return execOrFail(ExecutorNames.METHODRETURN, _r);
    }

    /**
     * Executes a runnable in a given executor.
     * May retry execution if {@link ExecutorService} has thrown an exception and retry handler
     * allows re-processing.
     * When re-processing fails for {@value #MAX_RETRIES} or more retries, an error will be logged
     * and the runnable will not be executed.
     *
     * @param _executor executor to use
     * @param _r runnable
     *
     * @return retries, if any input was null -1 is returned
     */
    int execOrFail(ExecutorNames _executor, Runnable _r) {
        if (_r == null || _executor == null) { // ignore invalid runnables or executors
            return -1;
        }

        int failCount = 0;
        while (failCount < MAX_RETRIES) {
            try {
                ExecutorService exec = getExecutor(_executor);
                if (exec == null) { // this should never happen, map is initialized in constructor
                    throw new IllegalThreadPoolStateException("No executor found for " + _executor);
                } else if (closed || exec.isShutdown() || exec.isTerminated()) {
                    throw new IllegalThreadPoolStateException("Receiving service already closed");
                }
                exec.execute(_r);
                break; // execution done, no retry needed
            } catch (IllegalThreadPoolStateException _ex) { // just throw our exception
                throw _ex;
            } catch (Exception _ex) {
                if (retryHandler == null) {
                    logger.error("Could not handle runnable for executor {}, runnable will be dropped", _executor, _ex);
                    break; // no handler, assume ignoring runnable is ok
                }

                failCount++;
                if (!retryHandler.handle(_executor, _ex)) {
                    logger.trace("Ignoring unhandled runnable for executor {} due to {}, dropped by retry handler after {} retries", _executor, _ex.getClass().getName(), failCount);
                    break;
                }
            }
        }

        if (failCount >= MAX_RETRIES) {
            logger.error("Could not handle runnable for executor {} after {} retries, runnable will be dropped", _executor, failCount);
        }

        return failCount;
    }

    /**
     * Returns the executor or null.
     *
     * @param _executor executor to use
     * @return executor or null
     */
    ExecutorService getExecutor(ExecutorNames _executor) {
        return executors.get(_executor);
    }

    /**
     * Shutdown all executor services waiting up to the given timeout/unit.
     *
     * @param _timeout timeout
     * @param _unit time unit
     */
    public synchronized void shutdown(int _timeout, TimeUnit _unit) {
        for (Entry<ExecutorNames, ExecutorService> es : executors.entrySet()) {
            logger.debug("Shutting down executor: {}", es.getKey());
            es.getValue().shutdown();
        }

        for (Entry<ExecutorNames, ExecutorService> es : executors.entrySet()) {
            try {
                es.getValue().awaitTermination(_timeout, _unit);
            } catch (InterruptedException _ex) {
                logger.debug("Interrupted while waiting for termination of executor");
            }
        }

        closed = true;
    }

    /**
     * Forcefully stop the executors.
     */
    public synchronized void shutdownNow() {
        for (Entry<ExecutorNames, ExecutorService> es : executors.entrySet()) {
            if (!es.getValue().isTerminated()) {
                logger.debug("Forcefully stopping {}", es.getKey());
                es.getValue().shutdownNow();
            }
        }

        closed = true;
    }

    /**
     * Enum representing different executor services.
     *
     * @author hypfvieh
     * @version 4.0.1 - 2022-02-02
     */
    public enum ExecutorNames {
        SIGNAL("SignalExecutor"),
        ERROR("ErrorExecutor"),
        METHODCALL("MethodCallExecutor"),
        METHODRETURN("MethodReturnExecutor");

        private final String description;

        ExecutorNames(String _name) {
            description = _name;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Interface which specifies a handler which will be called when the thread pool throws any exception.
     *
     * @author hypfvieh
     * @since 4.2.0 - 2022-07-14
     */
    @FunctionalInterface
    public interface IThreadPoolRetryHandler {
        /**
         * Called to handle an exception.
         * <p>
         * This method should return true to retry execution or false to
         * just ignore the error and drop the unhandled message.
         * </p>
         *
         * @param _executor the executor which has thrown the exception
         * @param _ex the exception which was thrown
         *
         * @return true to retry execution of the failed runnable, false to ignore runnable
         */
        boolean handle(ExecutorNames _executor, Exception _ex);
    }

}
