package org.keycloak.testframework.server;

import java.util.Optional;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EcsLogRecordTest {

    @Test
    public void recordWithStackTrace() {
        EcsLogRecord record = EcsLogRecord.parse("{\"log.level\":\"ERROR\",\"log.logger\":\"a.B\",\"message\":\"failed\",\"error.stack_trace\":\"java.lang.RuntimeException: x\\n\\tat a.B.c(B.java:1)\"}").orElseThrow();
        Assertions.assertEquals(Logger.Level.ERROR, record.level());
        Assertions.assertEquals("a.B", record.loggerName());
        Assertions.assertEquals("failed\njava.lang.RuntimeException: x\n\tat a.B.c(B.java:1)", record.fullMessage());
    }

    @Test
    public void recordWithoutStackTrace() {
        EcsLogRecord record = EcsLogRecord.parse("{\"log.level\":\"WARN\",\"log.logger\":\"a.B\",\"message\":\"hello\"}").orElseThrow();
        Assertions.assertEquals("hello", record.fullMessage());
    }

    @Test
    public void nonRecordsAreNotParsed() {
        for (String line : new String[] {
                "{\"message\":\"ReviewRawJson-marker\",\"diagnostic\":\"important detail\"}",
                "{\"status\":\"ok\"}",
                "{\"log.level\":\"UNKNOWN\",\"log.logger\":\"a.B\",\"message\":\"x\"}",
                "{\"log.level\":\"INFO\",\"message\":\"x\"}",
                "{\"log.level\":\"INFO\",\"log.logger\":\"a.B\",\"message\":{\"a\":1}}",
                "{\"log.level\":\"INFO\",\"log.logger\":\"a.B\",\"message\":\"x\",\"error.stack_trace\":[\"a\"]}",
                "{not json",
                "[]"}) {
            Assertions.assertEquals(Optional.empty(), EcsLogRecord.parse(line), line);
        }
    }

}
