package org.freedesktop.dbus.connections.config;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.freedesktop.dbus.connections.ReceivingService;
import org.freedesktop.dbus.connections.ReceivingService.ExecutorNames;
import org.freedesktop.dbus.connections.ReceivingService.IThreadPoolRetryHandler;
import org.freedesktop.dbus.connections.impl.BaseConnectionBuilder;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.LoggerFactory;

/**
 * Configuration builder to configure {@link ReceivingService}.
 * Only intended to be used in combination with {@link BaseConnectionBuilder}
 *
 * @author hypfvieh
 *
 * @param <R> BaseConnectionBuilder type
 * @since 4.2.0 - 2022-07-14
 */
public final class ReceivingServiceConfigBuilder<R extends BaseConnectionBuilder<?, ?>> {
    public static final int DEFAULT_HANDLER_RETRIES = 10;

    private static final ReceivingServiceConfig DEFAULT_CFG = new ReceivingServiceConfig();

    private static final IThreadPoolRetryHandler DEFAULT_RETRYHANDLER = new IThreadPoolRetryHandler() {
        private AtomicInteger retries = new AtomicInteger(0);
        @Override
        public boolean handle(ExecutorNames _executor, Exception _ex) {
            if (retries.incrementAndGet() < DEFAULT_HANDLER_RETRIES) {
                return true;
            }
            LoggerFactory.getLogger(ReceivingService.class).error("Dropping runnable for {}, retry failed for more than {} iterations, cause:", _executor, DEFAULT_HANDLER_RETRIES, _ex);
            return false;
        }
    };

    private final Supplier<R> connectionBuilder;
    private final ReceivingServiceConfig config = new ReceivingServiceConfig();

    public ReceivingServiceConfigBuilder(Supplier<R> _bldr) {
        connectionBuilder = _bldr;
        config.setRetryHandler(DEFAULT_RETRYHANDLER);
    }

    /**
     * Set the size of the thread-pool used to handle signals from the bus.
     * Caution: Using thread-pool size &gt; 1 may cause signals to be handled out-of-order
     * <p>
     * Default: 1
     *
     * @param _threads int &gt;= 1
     * @return this
     */
    public ReceivingServiceConfigBuilder<R> withSignalThreadCount(int _threads) {
        config.setSignalThreadPoolSize(Math.max(1, _threads));
        return this;
    }

    /**
     * Set the size of the thread-pool used to handle error messages received on the bus.
     * <p>
     * Default: 1
     *
     * @param _threads int &gt;= 1
     * @return this
     */
    public ReceivingServiceConfigBuilder<R> withErrorHandlerThreadCount(int _threads) {
        config.setErrorThreadPoolSize(Math.max(1, _threads));
        return this;
    }

    /**
     * Set the size of the thread-pool used to handle methods calls previously sent to the bus.
     * The thread pool size has to be &gt; 1 to handle recursive calls.
     * <p>
     * Default: 4
     *
     * @param _threads int &gt;= 1
     * @return this
     */
    public ReceivingServiceConfigBuilder<R> withMethodCallThreadCount(int _threads) {
        config.setMethodCallThreadPoolSize(Math.max(1, _threads));
        return this;
    }

    /**
     * Set the size of the thread-pool used to handle method return values received on the bus.
     * <p>
     * Default: 1
     *
     * @param _threads int &gt;= 1
     * @return this
     */
    public ReceivingServiceConfigBuilder<R> withMethodReturnThreadCount(int _threads) {
        config.setMethodReturnThreadPoolSize(Math.max(1, _threads));
        return this;
    }

    /**
     * Sets the thread priority of the created signal thread(s).
     * <p>
     * Default: {@link Thread#NORM_PRIORITY} ({@value Thread#NORM_PRIORITY});
     *
     * @param _priority int &gt;={@value Thread#MIN_PRIORITY} and &lt;= {@value Thread#MAX_PRIORITY}
     * @return this
     *
     * @throws IllegalArgumentException when value is out ouf range (value &lt; {@value Thread#MIN_PRIORITY} &amp;&amp; &gt; {@value Thread#MAX_PRIORITY})
     */
    public ReceivingServiceConfigBuilder<R> withSignalThreadPriority(int _priority) {
        config.setSignalThreadPriority(Util.checkIntInRange(_priority, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY));
        return this;
    }

    /**
     * Sets the thread priority of the created signal thread(s).
     * <p>
     * Default: {@link Thread#NORM_PRIORITY} ({@value Thread#NORM_PRIORITY});
     *
     * @param _priority int &gt;={@value Thread#MIN_PRIORITY} and &lt;= {@value Thread#MAX_PRIORITY}
     * @return this
     *
     * @throws IllegalArgumentException when value is out ouf range (value &lt; {@value Thread#MIN_PRIORITY} &amp;&amp; &gt; {@value Thread#MAX_PRIORITY})
     */
    public ReceivingServiceConfigBuilder<R> withErrorThreadPriority(int _priority) {
        config.setErrorThreadPriority(Util.checkIntInRange(_priority, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY));
        return this;
    }

    /**
     * Sets the thread priority of the created signal thread(s).
     * <p>
     * Default: {@link Thread#NORM_PRIORITY} ({@value Thread#NORM_PRIORITY});
     *
     * @param _priority int &gt;={@value Thread#MIN_PRIORITY} and &lt;= {@value Thread#MAX_PRIORITY}
     * @return this
     *
     * @throws IllegalArgumentException when value is out ouf range (value &lt; {@value Thread#MIN_PRIORITY} &amp;&amp; &gt; {@value Thread#MAX_PRIORITY})
     */
    public ReceivingServiceConfigBuilder<R> withMethedCallThreadPriority(int _priority) {
        config.setMethodCallThreadPriority(Util.checkIntInRange(_priority, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY));
        return this;
    }

    /**
     * Sets the thread priority of the created signal thread(s).
     * <p>
     * Default: {@link Thread#NORM_PRIORITY} ({@value Thread#NORM_PRIORITY});
     *
     * @param _priority int &gt;={@value Thread#MIN_PRIORITY} and &lt;= {@value Thread#MAX_PRIORITY}
     * @return this
     *
     * @throws IllegalArgumentException when value is out ouf range (value &lt; {@value Thread#MIN_PRIORITY} &amp;&amp; &gt; {@value Thread#MAX_PRIORITY})
     */
    public ReceivingServiceConfigBuilder<R> withMethodReturnThreadPriority(int _priority) {
        config.setMethodReturnThreadPriority(Util.checkIntInRange(_priority, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY));
        return this;
    }

    /**
     * Sets the retry handler which should be called when executing a runnable in {@link ReceivingService} thread pools fail.
     * <p>
     * Defaults to an implementation retrying executing the runnable up to ten times.
     * If <code>null</code> is given, retrying will be disabled (but error will be logged).
     *
     * @param _handler handler to use
     * @return this
     */
    public ReceivingServiceConfigBuilder<R> withRetryHandler(IThreadPoolRetryHandler _handler) {
        config.setRetryHandler(_handler);
        return this;
    }

    /**
     * Returns the configured {@link ReceivingServiceConfig} instance.
     * @return config never null
     */
    public ReceivingServiceConfig build() {
        return config;
    }

    /**
     * Returns the used ConnectionBuilder for the connection for further configuration.
     * @return connection builder
     */
    public R connectionConfig() {
        return connectionBuilder.get();
    }

    /**
     * Returns the default configuration used for {@link ReceivingService}.
     * @return default config
     */
    public static ReceivingServiceConfig getDefaultConfig() {
        return DEFAULT_CFG;
    }

    /**
     * Returns the default retry handler used for {@link ReceivingService}.
     * @return default handler
     */
    public static IThreadPoolRetryHandler getDefaultRetryHandler() {
        return DEFAULT_RETRYHANDLER;
    }

}
