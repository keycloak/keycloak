package org.keycloak.quarkus.runtime.configuration.mappers;

import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.logging.Level;

import org.jboss.logmanager.LogContext;
import org.keycloak.quarkus.runtime.Messages;

import io.smallrye.config.ConfigSourceInterceptorContext;
import org.keycloak.quarkus.runtime.storage.database.Database;

final class LoggingPropertyMappers {

    private static final String DEFAULT_LOG_LEVEL = "info";
    private static final List<String> JSON_FIELD_LIST = List.of("timestamp", "hostname", "sequence", "logger-class-name",
            "logger-name", "level", "message", "thread-name", "thread-id","process-name", "process-id",
            "stack-trace", "error-type", "error-message");
    private static final List<String> JSON_FIELD_DEFAULT_DISABLED = List.of(
            "hostname", "mdc","ndc","process-name", "process-id", "logger-class-name", "thread-name",
            "thread-id","process-name", "process-id");

    private LoggingPropertyMappers(){}

    public static PropertyMapper[] getMappers() {
        return new PropertyMapper[] {
                builder().from("log-level")
                        .to("quarkus.log.level")
                        .transformer(new BiFunction<String, ConfigSourceInterceptorContext, String>() {
                            @Override
                            public String apply(String value, ConfigSourceInterceptorContext configSourceInterceptorContext) {
                                String rootLevel = DEFAULT_LOG_LEVEL;

                                for (String level : value.split(",")) {
                                    String[] parts = level.split(":");
                                    String category = null;
                                    String categoryLevel;

                                    if (parts.length == 1) {
                                        categoryLevel = parts[0];
                                    } else if (parts.length == 2) {
                                        category = parts[0];
                                        categoryLevel = parts[1];
                                    } else {
                                        addInitializationException(Messages.invalidLogCategoryFormat(level));
                                        return rootLevel;
                                    }

                                    Level levelType;

                                    try {
                                        levelType = toLevel(categoryLevel);
                                    } catch (IllegalArgumentException iae) {
                                        addInitializationException(Messages.invalidLogLevel(categoryLevel));
                                        return rootLevel;
                                    }

                                    if (category == null) {
                                        rootLevel = levelType.getName();
                                    } else {
                                        setCategoryLevel(category, levelType.getName());
                                    }
                                }

                                return rootLevel;
                            }
                        })
                        .defaultValue(DEFAULT_LOG_LEVEL)
                        .description("The log level of the root category or a comma-separated list of individual categories and their levels. For the root category, you don't need to specify a category.")
                        .paramLabel("category:level")
                        .build(),
                builder().from("log-format")
                        .to("quarkus.log.console.format")
                        .defaultValue("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n")
                        .description("The format of log entries. If the format has spaces in it, you need to escape the value such as \"<format>\".")
                        .paramLabel("format")
                        .build(),
                builder().from("log-json-enabled")
                        .to("quarkus.log.console.json")
                        .defaultValue(Boolean.FALSE.toString())
                        .description("Enable or disable JSON logging")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .expectedValues(Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()))
                        .build(),

                //from --log-json-fields-disable=...,... to multiple keys on quarkus side -> quarkus.log.console.json.fields.error-type.enabled
                //TODO
                builder().from("log-json-fields-disabled")
                        .defaultValue(String.join(",", JSON_FIELD_DEFAULT_DISABLED))
                        .description("Comma-separated list of Keys to disable in JSON log output")
                        .paramLabel("<key>")
                        .expectedValues(JSON_FIELD_LIST)
                        .build(),

                //never use mdc/ndc as we don't support it
                builder().from("log-json-fields-mdc-enabled")
                        .to("quarkus.log.console.json.fields.mdc.enabled")
                        .defaultValue(Boolean.FALSE.toString())
                        .hidden(true)
                        .build(),

                builder().from("log-json-fields-ndc-enabled")
                        .to("quarkus.log.console.json.fields.ndc.enabled")
                        .defaultValue(Boolean.FALSE.toString())
                        .hidden(true)
                        .build(),
        };
    }

    private static Level toLevel(String categoryLevel) throws IllegalArgumentException {
        return LogContext.getLogContext().getLevelForName(categoryLevel.toUpperCase(Locale.ROOT));
    }

    private static void setCategoryLevel(String category, String level) {
        LogContext.getLogContext().getLogger(category).setLevel(toLevel(level));
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.LOGGING);
    }
}
