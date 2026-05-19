package org.keycloak.testframework.log;

import java.text.MessageFormat;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;

public class LogEntry {

    private static final Pattern KEYCLOAK_LOG_PATTERN = Pattern.compile("\\[([^]]*)] \\([^)]*\\) (.*)");

    private final LogQueue.State state;
    private final LogRecord record;
    private String convertedLoggerName;
    private String convertedMessage;
    private String formattedMessage;
    private Level convertedLevel;

    LogEntry(LogQueue.State state, LogRecord record) {
        this.state = state;
        this.record = record;
    }

    public LogQueue.State getState() {
        return state;
    }

    public String getMessage() {
        if (formattedMessage == null) {
            convertKeycloakLog();

            if (record instanceof ExtLogRecord extLogRecord) {
                ExtLogRecord.FormatStyle formatStyle = extLogRecord.getFormatStyle();
                formattedMessage = switch (formatStyle) {
                    case MESSAGE_FORMAT -> MessageFormat.format(convertedMessage, record.getParameters());
                    case PRINTF -> String.format(convertedMessage, record.getParameters());
                    default -> convertedMessage;
                };
            } else {
                formattedMessage = convertedMessage;
            }
        }
        return formattedMessage;
    }

    public String getLoggerName() {
        convertKeycloakLog();

        return convertedLoggerName;
    }

    public Level getLevel() {
        if (convertedLevel == null) {
            convertedLevel = convertLevel(record.getLevel());
        }
        return convertedLevel;
    }

    LogRecord getRecord() {
        return record;
    }

    boolean isManagedKeycloakLogEntry() {
        return record.getLoggerName().equals(LogCategories.MANAGED_KEYCLOAK);
    }

    private void convertKeycloakLog() {
        if (convertedLoggerName == null && convertedMessage == null) {
            if (isManagedKeycloakLogEntry()) {
                Matcher matcher = KEYCLOAK_LOG_PATTERN.matcher(record.getMessage());
                if (matcher.matches()) {
                    convertedLoggerName = matcher.group(1);
                    convertedMessage = matcher.group(2);
                    return;
                }
            }
            convertedLoggerName = record.getLoggerName();
            convertedMessage = record.getMessage();
        }
    }

    private Level convertLevel(java.util.logging.Level level) {
        if (level.equals(java.util.logging.Level.INFO)) {
            return Level.INFO;
        } else if (level.equals(java.util.logging.Level.FINE)) {
            return Level.DEBUG;
        } else if (level.equals(java.util.logging.Level.FINER) || level.equals(java.util.logging.Level.FINEST)) {
            return Level.TRACE;
        } else if (level.equals(java.util.logging.Level.WARNING)) {
            return Level.WARN;
        } else if (level.equals(java.util.logging.Level.SEVERE)) {
            return Level.ERROR;
        } else if (level.equals(java.util.logging.Level.CONFIG)) {
            return Level.DEBUG;
        } else {
            throw new IllegalArgumentException("Unknown log level " + level.getName());
        }
    }

}
