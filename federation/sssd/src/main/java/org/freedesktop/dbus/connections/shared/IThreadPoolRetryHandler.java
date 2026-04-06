package org.freedesktop.dbus.connections.shared;

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
