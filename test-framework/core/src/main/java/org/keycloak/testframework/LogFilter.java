package org.keycloak.testframework;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogFilter implements Filter {

    private final Queue<LogRecord> queue = new LinkedList<>();

    @Override
    public boolean isLoggable(LogRecord record) {
        queue.add(record);
        return false;
    }

    public void clear(boolean forwardLogs) {
        if (forwardLogs) {
            for (LogRecord r = queue.poll(); r != null; r = queue.poll()) {
                Logger.getLogger(r.getLoggerName()).log(r.getLevel(), r.getMessage(), r.getParameters());
            }
        } else {
            queue.clear();
        }
    }

}
