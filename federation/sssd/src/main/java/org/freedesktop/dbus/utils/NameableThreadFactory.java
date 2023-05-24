package org.freedesktop.dbus.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread factory which allows setting name, priority and daemon flag
 * for all newly created threads.
 */
public class NameableThreadFactory implements ThreadFactory {

    private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
    private final ThreadGroup          group;
    private final AtomicInteger        threadNumber = new AtomicInteger(1);
    private final String               namePrefix;

    private final int                  threadPriority;
    private final boolean              daemonizeThreads;

    /**
     * Create a new ThreadFactory instance.
     * The thread name is created like this:
     * _name + THREAD_NUMBER
     * e.g: connectionPool-1
     * If _name is null or blank, UnnamedThreadPool-POOL_NUMBER-thread-THREAD_NUMBER will be used.
     *
     * @param _name prefix for all thread names
     * @param _daemonizeThreads turn all created threads to daemon threads
     */
    public NameableThreadFactory(String _name, boolean _daemonizeThreads) {
        this(_name, _daemonizeThreads, Thread.NORM_PRIORITY);
    }

    /**
     * Create a new ThreadFactory instance.
     * The thread name is created like this:
     * _name + THREAD_NUMBER
     * e.g: connectionPool-1
     * If _name is null or blank, UnnamedThreadPool-POOL_NUMBER-thread-THREAD_NUMBER will be used.
     *
     * @param _name prefix for all thread names
     * @param _daemonizeThreads turn all created threads to daemon threads
     * @param _threadPriority priority to use for new threads
     *
     * @since 4.2.0 - 2022-07-13
     */
    public NameableThreadFactory(String _name, boolean _daemonizeThreads, int _threadPriority) {
        group = Thread.currentThread().getThreadGroup(); //NOPMD
        namePrefix = Util.isBlank(_name) ? "UnnamedThreadPool-" + POOL_NUMBER.getAndIncrement() + "-thread-" : _name;
        daemonizeThreads = _daemonizeThreads;
        threadPriority = _threadPriority;
    }

    @Override
    public Thread newThread(Runnable _runnable) {
        Thread t = new Thread(group, _runnable, namePrefix + threadNumber.getAndIncrement(), 0);
        t.setDaemon(daemonizeThreads);
        t.setPriority(threadPriority);

        return t;
    }

}
