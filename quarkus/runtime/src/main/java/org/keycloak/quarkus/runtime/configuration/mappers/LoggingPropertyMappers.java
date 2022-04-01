package org.keycloak.quarkus.runtime.configuration.mappers;

import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.logging.Level;

import org.jboss.logmanager.LogContext;
import org.keycloak.quarkus.runtime.Messages;

import io.smallrye.config.ConfigSourceInterceptorContext;

public final class LoggingPropertyMappers {

    private static final String DEFAULT_LOG_LEVEL = "info";
    private static final String DEFAULT_LOG_HANDLER = "console";
    private static final String DEFAULT_LOG_FILENAME = "keycloak.log";
    public static final String DEFAULT_LOG_PATH = "data" + File.separator + "log" + File.separator + DEFAULT_LOG_FILENAME;
    private static final List<String> AVAILABLE_LOG_HANDLERS = List.of(DEFAULT_LOG_HANDLER,"file");
    private static final String DEFAULT_CONSOLE_OUTPUT = "default";

    private LoggingPropertyMappers(){}

    public static PropertyMapper[] getMappers() {
        return new PropertyMapper[] {
                builder().from("log")
                        .defaultValue(DEFAULT_LOG_HANDLER)
                        .description("Enable one or more log handlers in a comma-separated list. Available log handlers are: " + String.join(",", AVAILABLE_LOG_HANDLERS))
                        .paramLabel("<handler>")
                        .expectedValues("console","file","console,file","file,console")
                        .build(),
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
                builder().from("log-console-output")
                        .to("quarkus.log.console.json")
                        .defaultValue(DEFAULT_CONSOLE_OUTPUT)
                        .description("Set the log output to JSON or default (plain) unstructured logging.")
                        .paramLabel("default|json")
                        .expectedValues(DEFAULT_CONSOLE_OUTPUT,"json")
                        .transformer((value, context) -> {
                            if(value.equals(DEFAULT_CONSOLE_OUTPUT)) {
                                return Boolean.FALSE.toString();
                            }
                            return Boolean.TRUE.toString();
                        })
                        .build(),
                builder().from("log-console-format")
                        .to("quarkus.log.console.format")
                        .defaultValue("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n")
                        .description("The format of unstructured console log entries. If the format has spaces in it, escape the value using \"<format>\".")
                        .paramLabel("format")
                        .build(),
                builder().from("log-console-color")
                        .to("quarkus.log.console.color")
                        .defaultValue(Boolean.FALSE.toString())
                        .description("Enable or disable colors when logging to console.")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                builder().from("log-console-enabled")
                        .mapFrom("log")
                        .to("quarkus.log.console.enable")
                        .hidden(true)
                        .transformer(resolveLogHandler(DEFAULT_LOG_HANDLER))
                        .build(),
                builder().from("log-file-enabled")
                        .mapFrom("log")
                        .to("quarkus.log.file.enable")
                        .hidden(true)
                        .transformer(resolveLogHandler("file"))
                        .build(),
                builder().from("log-file")
                        .to("quarkus.log.file.path")
                        .defaultValue(DEFAULT_LOG_PATH)
                        .description("Set the log file path and filename.")
                        .paramLabel("<path>/<file-name>.log")
                        .transformer(LoggingPropertyMappers::resolveFileLogLocation)
                        .build(),
                builder().from("log-file-format")
                        .to("quarkus.log.file.format")
                        .defaultValue("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n")
                        .description("Set a format specific to file log entries.")
                        .paramLabel("<format>")
                        .build()
        };
    }

    private static BiFunction<String, ConfigSourceInterceptorContext, String> resolveLogHandler(String handler) {
        return (parentValue, context) -> {

            //we want to fall back to console to not have nothing shown up when wrong values are set.
            String consoleDependantErrorResult = handler.equals(DEFAULT_LOG_HANDLER) ? Boolean.TRUE.toString() : Boolean.FALSE.toString();

            if(parentValue.isBlank()) {
                addInitializationException(Messages.emptyValueForKey("log"));
                return consoleDependantErrorResult;
            }

            String[] logHandlerValues = parentValue.split(",");

            if (!AVAILABLE_LOG_HANDLERS.containsAll(List.of(logHandlerValues))) {
                addInitializationException(Messages.notRecognizedValueInList("log", parentValue, String.join(",", AVAILABLE_LOG_HANDLERS)));
                return consoleDependantErrorResult;
            }

            for (String handlerInput : logHandlerValues) {
                if (handlerInput.equals(handler)) {
                    return Boolean.TRUE.toString();
                }
            }

            return Boolean.FALSE.toString();
        };
    }

    private static String resolveFileLogLocation(String value, ConfigSourceInterceptorContext configSourceInterceptorContext) {
        if (value.endsWith(File.separator))
        {
            return value + DEFAULT_LOG_FILENAME;
        }

        return value;
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
