package org.keycloak.testframework.log;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogQueue {

    private static final LogQueue INSTANCE = new LogQueue();

    private final List<LogEntry> records = Collections.synchronizedList(new LinkedList<>());

    private volatile State state = State.INIT;

    private volatile boolean filterLogs = false;

    public static LogQueue getInstance() {
        return INSTANCE;
    }

    public void add(LogRecord record) {
        if (!state.equals(LogQueue.State.NONE)) {
            records.add(new LogEntry(state, record));
        }
    }

    public List<LogEntry> snapshot() {
        return new LinkedList<>(records);
    }

    public void enableLogFiltering(boolean filterLogs) {
        this.filterLogs = filterLogs;
    }

    public void testInit() {
        this.state = State.INIT;
    }

    public void testStarting() {
        this.state = State.RUNNING;
    }

    public void testSuccess() {
        this.state = State.NONE;
        records.clear();
    }

    public void testFailure() {
        this.state = State.NONE;

        if (filterLogs) {
            Iterator<LogEntry> logItr = records.iterator();
            while (logItr.hasNext()) {
                LogRecord record = logItr.next().getRecord();
                logItr.remove();
                Logger.getLogger(record.getLoggerName()).log(record.getLevel(), record.getMessage(), record.getParameters());
            }
        }

        records.clear();
    }

    public boolean shouldPublish() {
        return state.equals(State.NONE) || !filterLogs;
    }

    public enum State {
        INIT,
        RUNNING,
        NONE
    }

}
