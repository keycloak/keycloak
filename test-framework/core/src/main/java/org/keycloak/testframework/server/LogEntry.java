package org.keycloak.testframework.server;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

public record LogEntry(Logger.Level level, String category, String message, String rawLine, boolean stderr) {

    private static final Pattern LOG_PATTERN = Pattern.compile("([^ ]*) ([^ ]*) ([A-Z]+)(\\s+)(.*)");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("\\[(.+?)]\\s*(.*)");

    static LogEntry parse(String rawLine, boolean stderr) {
        Matcher logMatcher = LOG_PATTERN.matcher(rawLine);
        if (!logMatcher.matches()) {
            return new LogEntry(null, null, rawLine, rawLine, stderr);
        }
        String levelString = logMatcher.group(3);
        String remainder = logMatcher.group(5);

        Logger.Level level = parseLevel(levelString);
        if (level == null) {
            return new LogEntry(null, null, rawLine, rawLine, stderr);
        }

        String category = null;
        String message = remainder;
        Matcher categoryMatcher = CATEGORY_PATTERN.matcher(remainder);
        if (categoryMatcher.matches()) {
            category = categoryMatcher.group(1);
            message = categoryMatcher.group(2);
        }

        return new LogEntry(level, category, message, rawLine, stderr);
    }

    static LogEntry fromLogRecord(LogRecord record) {
        Logger.Level level = mapJulLevel(record.getLevel());
        String category = record.getLoggerName();
        String message = record.getMessage();
        return new LogEntry(level, category, message, message, false);
    }

    private static Logger.Level parseLevel(String levelString) {
        for (Logger.Level l : Logger.Level.values()) {
            if (l.name().equals(levelString)) {
                return l;
            }
        }
        return null;
    }

    private static Logger.Level mapJulLevel(Level julLevel) {
        int value = julLevel.intValue();
        if (value >= Level.SEVERE.intValue()) {
            return Logger.Level.ERROR;
        } else if (value >= Level.WARNING.intValue()) {
            return Logger.Level.WARN;
        } else if (value >= Level.INFO.intValue()) {
            return Logger.Level.INFO;
        } else if (value >= Level.FINE.intValue()) {
            return Logger.Level.DEBUG;
        } else {
            return Logger.Level.TRACE;
        }
    }

}
