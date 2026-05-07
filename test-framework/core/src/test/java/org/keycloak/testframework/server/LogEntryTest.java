package org.keycloak.testframework.server;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class LogEntryTest {

    @Test
    void parseExtractsAllFields() {
        String raw = "2024-08-21 08:14:33,591 INFO  [org.keycloak.services] (main) KC-SERVICES0001: Keycloak started";
        LogEntry entry = LogEntry.parse(raw, true);

        assertThat("level", entry.level(), is(Logger.Level.INFO));
        assertThat("category", entry.category(), is("org.keycloak.services"));
        assertThat("message", entry.message(), containsString("KC-SERVICES0001"));
        assertThat("rawLine preserved", entry.rawLine(), is(raw));
        assertThat("stderr flag passed through", entry.stderr(), is(true));
    }

    @Test
    void parseRecognizesAllLevels() {
        assertThat(LogEntry.parse("2024-08-21 08:14:33,591 DEBUG [c] (t) m", false).level(), is(Logger.Level.DEBUG));
        assertThat(LogEntry.parse("2024-08-21 08:14:33,591 WARN  [c] (t) m", false).level(), is(Logger.Level.WARN));
        assertThat(LogEntry.parse("2024-08-21 08:14:33,591 ERROR [c] (t) m", false).level(), is(Logger.Level.ERROR));
        assertThat(LogEntry.parse("2024-08-21 08:14:33,591 TRACE [c] (t) m", false).level(), is(Logger.Level.TRACE));
        assertThat(LogEntry.parse("2024-08-21 08:14:33,591 FATAL [c] (t) m", false).level(), is(Logger.Level.FATAL));
    }

    @Test
    void parseLineWithoutCategory() {
        LogEntry entry = LogEntry.parse("2024-08-21 08:14:33,591 INFO  some message without brackets", false);

        assertThat("level still parsed", entry.level(), is(Logger.Level.INFO));
        assertThat("category null when no brackets", entry.category(), nullValue());
        assertThat("message contains full remainder", entry.message(), containsString("some message without brackets"));
    }

    @Test
    void parseMalformedAndEmptyLines() {
        LogEntry malformed = LogEntry.parse("this is not a log line", false);
        assertThat("level null for malformed", malformed.level(), nullValue());
        assertThat("category null for malformed", malformed.category(), nullValue());
        assertThat("message falls back to rawLine", malformed.message(), is("this is not a log line"));

        LogEntry empty = LogEntry.parse("", false);
        assertThat("level null for empty", empty.level(), nullValue());
        assertThat("rawLine preserved as empty", empty.rawLine(), is(""));
    }

    @Test
    void fromRealJulLogger() {
        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("org.keycloak.test.jul");
        julLogger.setLevel(Level.ALL);
        CapturingHandler handler = new CapturingHandler();
        julLogger.addHandler(handler);
        try {
            julLogger.info("JUL message");
        } finally {
            julLogger.removeHandler(handler);
        }

        LogEntry entry = LogEntry.fromLogRecord(handler.captured);

        assertThat("level mapped from JUL", entry.level(), is(Logger.Level.INFO));
        assertThat("category is logger name", entry.category(), is("org.keycloak.test.jul"));
        assertThat("message preserved", entry.message(), is("JUL message"));
        assertThat("rawLine equals message in embedded mode", entry.rawLine(), is("JUL message"));
        assertThat("not stderr", entry.stderr(), is(false));
    }

    @Test
    void fromLogRecordLevelMapping() {
        assertThat("SEVERE→ERROR", LogEntry.fromLogRecord(new LogRecord(Level.SEVERE, "")).level(), is(Logger.Level.ERROR));
        assertThat("WARNING→WARN", LogEntry.fromLogRecord(new LogRecord(Level.WARNING, "")).level(), is(Logger.Level.WARN));
        assertThat("INFO→INFO", LogEntry.fromLogRecord(new LogRecord(Level.INFO, "")).level(), is(Logger.Level.INFO));
        assertThat("FINE→DEBUG", LogEntry.fromLogRecord(new LogRecord(Level.FINE, "")).level(), is(Logger.Level.DEBUG));
        assertThat("FINEST→TRACE", LogEntry.fromLogRecord(new LogRecord(Level.FINEST, "")).level(), is(Logger.Level.TRACE));
    }

    private static final class CapturingHandler extends Handler {

        LogRecord captured;

        @Override
        public void publish(LogRecord record) {
            captured = record;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
