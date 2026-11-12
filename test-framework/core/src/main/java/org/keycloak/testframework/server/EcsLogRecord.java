package org.keycloak.testframework.server;

import java.io.IOException;
import java.util.Optional;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * A log record of the managed server in the ECS JSON format. A record that includes a stack trace is a single line.
 */
record EcsLogRecord(Logger.Level level, String loggerName, String message, String stackTrace) {

    /**
     * Parses a line of the server output, and returns empty if it is not a complete ECS log record, so that the
     * caller can forward the line unchanged.
     */
    static Optional<EcsLogRecord> parse(String line) {
        JsonNode node;
        try {
            node = JsonSerialization.mapper.readTree(line);
        } catch (IOException e) {
            return Optional.empty();
        }

        if (node == null || !node.isObject() || !node.path("message").isTextual() || !node.path("log.logger").isTextual()) {
            return Optional.empty();
        }

        Logger.Level level = null;
        if (node.path("log.level").isTextual()) {
            for (Logger.Level l : Logger.Level.values()) {
                if (l.name().equals(node.get("log.level").asText())) {
                    level = l;
                    break;
                }
            }
        }
        if (level == null) {
            return Optional.empty();
        }

        JsonNode stackTrace = node.get("error.stack_trace");
        if (stackTrace != null && !stackTrace.isNull() && !stackTrace.isTextual()) {
            return Optional.empty();
        }

        return Optional.of(new EcsLogRecord(level, node.get("log.logger").asText(), node.get("message").asText(), stackTrace != null && stackTrace.isTextual() ? stackTrace.asText() : null));
    }

    String fullMessage() {
        return stackTrace != null ? message + "\n" + stackTrace : message;
    }

}
