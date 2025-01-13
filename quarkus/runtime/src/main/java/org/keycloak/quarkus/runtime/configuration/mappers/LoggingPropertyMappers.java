package org.keycloak.quarkus.runtime.configuration.mappers;

import static java.util.Optional.of;
import static org.keycloak.config.LoggingOptions.DEFAULT_LOG_FORMAT;
import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.stream.Stream;

import io.quarkus.runtime.configuration.MemorySizeConverter;
import org.jboss.logmanager.LogContext;
import org.keycloak.config.LoggingOptions;
import org.keycloak.config.Option;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.cli.PropertyException;

import io.smallrye.config.ConfigSourceInterceptorContext;
import org.keycloak.quarkus.runtime.configuration.Configuration;

public final class LoggingPropertyMappers {

    private static final String CONSOLE_ENABLED_MSG = "Console log handler is activated";
    private static final String FILE_ENABLED_MSG = "File log handler is activated";
    private static final String SYSLOG_ENABLED_MSG = "Syslog is activated";

    private LoggingPropertyMappers() {
    }

    public static PropertyMapper<?>[] getMappers() {
        PropertyMapper<?>[] defaultMappers = new PropertyMapper[]{
                fromOption(LoggingOptions.LOG)
                        .paramLabel("<handler>")
                        .build(),
                // Console
                fromOption(LoggingOptions.LOG_CONSOLE_OUTPUT)
                        .isEnabled(LoggingPropertyMappers::isConsoleEnabled, CONSOLE_ENABLED_MSG)
                        .to("quarkus.log.console.json")
                        .paramLabel("output")
                        .transformer(LoggingPropertyMappers::resolveLogOutput)
                        .build(),
                fromOption(LoggingOptions.LOG_CONSOLE_LEVEL)
                        .isEnabled(LoggingPropertyMappers::isConsoleEnabled, CONSOLE_ENABLED_MSG)
                        .to("quarkus.log.console.level")
                        .paramLabel("level")
                        .build(),
                fromOption(LoggingOptions.LOG_CONSOLE_FORMAT)
                        .isEnabled(LoggingPropertyMappers::isConsoleEnabled, CONSOLE_ENABLED_MSG)
                        .to("quarkus.log.console.format")
                        .paramLabel("format")
                        .transformer((value, ctx) -> addTracingInfo(value, LoggingOptions.LOG_CONSOLE_INCLUDE_TRACE))
                        .build(),
                fromOption(LoggingOptions.LOG_CONSOLE_INCLUDE_TRACE)
                        .isEnabled(() -> LoggingPropertyMappers.isConsoleEnabled() && TracingPropertyMappers.isTracingEnabled(),
                                "Console log handler and Tracing is activated")
                        .build(),
                fromOption(LoggingOptions.LOG_CONSOLE_COLOR)
                        .isEnabled(LoggingPropertyMappers::isConsoleEnabled, CONSOLE_ENABLED_MSG)
                        .to("quarkus.log.console.color")
                        .build(),
                fromOption(LoggingOptions.LOG_CONSOLE_ENABLED)
                        .mapFrom("log")
                        .to("quarkus.log.console.enable")
                        .transformer(LoggingPropertyMappers.resolveLogHandler(LoggingOptions.DEFAULT_LOG_HANDLER.name()))
                        .build(),
                // File
                fromOption(LoggingOptions.LOG_FILE_ENABLED)
                        .mapFrom("log")
                        .to("quarkus.log.file.enable")
                        .transformer(LoggingPropertyMappers.resolveLogHandler("file"))
                        .build(),
                fromOption(LoggingOptions.LOG_FILE)
                        .isEnabled(LoggingPropertyMappers::isFileEnabled, FILE_ENABLED_MSG)
                        .to("quarkus.log.file.path")
                        .paramLabel("file")
                        .transformer(LoggingPropertyMappers::resolveFileLogLocation)
                        .build(),
                fromOption(LoggingOptions.LOG_FILE_LEVEL)
                        .isEnabled(LoggingPropertyMappers::isFileEnabled, FILE_ENABLED_MSG)
                        .to("quarkus.log.file.level")
                        .paramLabel("level")
                        .build(),
                fromOption(LoggingOptions.LOG_FILE_FORMAT)
                        .isEnabled(LoggingPropertyMappers::isFileEnabled, FILE_ENABLED_MSG)
                        .to("quarkus.log.file.format")
                        .paramLabel("format")
                        .transformer((value, ctx) -> addTracingInfo(value, LoggingOptions.LOG_FILE_INCLUDE_TRACE))
                        .build(),
                fromOption(LoggingOptions.LOG_FILE_INCLUDE_TRACE)
                        .isEnabled(() -> LoggingPropertyMappers.isFileEnabled() && TracingPropertyMappers.isTracingEnabled(),
                                "File log handler and Tracing is activated")
                        .build(),
                fromOption(LoggingOptions.LOG_FILE_OUTPUT)
                        .isEnabled(LoggingPropertyMappers::isFileEnabled, FILE_ENABLED_MSG)
                        .to("quarkus.log.file.json")
                        .paramLabel("output")
                        .transformer(LoggingPropertyMappers::resolveLogOutput)
                        .build(),
                // Log level
                fromOption(LoggingOptions.LOG_LEVEL)
                        .to("quarkus.log.level")
                        .transformer(LoggingPropertyMappers::resolveLogLevel)
                        .validator(LoggingPropertyMappers::validateLogLevel)
                        .paramLabel("category:level")
                        .build(),
                // Syslog
                fromOption(LoggingOptions.LOG_SYSLOG_ENABLED)
                        .mapFrom("log")
                        .to("quarkus.log.syslog.enable")
                        .transformer(LoggingPropertyMappers.resolveLogHandler("syslog"))
                        .build(),
                fromOption(LoggingOptions.LOG_SYSLOG_ENDPOINT)
                        .isEnabled(LoggingPropertyMappers::isSyslogEnabled, SYSLOG_ENABLED_MSG)
                        .to("quarkus.log.syslog.endpoint")
                        .paramLabel("host:port")
                        .build(),
                fromOption(LoggingOptions.LOG_SYSLOG_LEVEL)
                        .isEnabled(LoggingPropertyMappers::isSyslogEnabled, SYSLOG_ENABLED_MSG)
                        .to("quarkus.log.syslog.level")
                        .paramLabel("level")
                        .build(),
                fromOption(LoggingOptions.LOG_SYSLOG_APP_NAME)
                        .isEnabled(LoggingPropertyMappers::isSyslogEnabled, SYSLOG_ENABLED_MSG)
                        .to("quarkus.log.syslog.app-name")
                        .paramLabel("name")
                        .build(),
                fromOption(LoggingOptions.LOG_SYSLOG_TYPE)
                        .isEnabled(LoggingPropertyMappers::isSyslogEnabled, SYSLOG_ENABLED_MSG)
                        .to("quarkus.log.syslog.syslog-type")
                        .paramLabel("type")
                        .build(),
                fromOption(LoggingOptions.LOG_SYSLOG_MAX_LENGTH)
                        .isEnabled(LoggingPropertyMappers::isSyslogEnabled, SYSLOG_ENABLED_MSG)
                        .to("quarkus.log.syslog.max-length")
                        .validator(LoggingPropertyMappers::validateSyslogMaxLength)
                        .paramLabel("max-length")
                        .build(),
                fromOption(LoggingOptions.LOG_SYSLOG_PROTOCOL)
                        .isEnabled(LoggingPropertyMappers::isSyslogEnabled, SYSLOG_ENABLED_MSG)
                        .to("quarkus.log.syslog.protocol")
                        .paramLabel("protocol")
                        .build(),
                fromOption(LoggingOptions.LOG_SYSLOG_FORMAT)
                        .isEnabled(LoggingPropertyMappers::isSyslogEnabled, SYSLOG_ENABLED_MSG)
                        .to("quarkus.log.syslog.format")
                        .paramLabel("format")
                        .transformer((value, ctx) -> addTracingInfo(value, LoggingOptions.LOG_SYSLOG_INCLUDE_TRACE))
                        .build(),
                fromOption(LoggingOptions.LOG_SYSLOG_INCLUDE_TRACE)
                        .isEnabled(() -> LoggingPropertyMappers.isSyslogEnabled() && TracingPropertyMappers.isTracingEnabled(),
                                "Syslog handler and Tracing is activated")
                        .build(),
                fromOption(LoggingOptions.LOG_SYSLOG_OUTPUT)
                        .isEnabled(LoggingPropertyMappers::isSyslogEnabled, SYSLOG_ENABLED_MSG)
                        .to("quarkus.log.syslog.json")
                        .paramLabel("output")
                        .transformer(LoggingPropertyMappers::resolveLogOutput)
                        .build(),
        };

        return defaultMappers;
    }

    public static boolean isConsoleEnabled() {
        return isTrue(LoggingOptions.LOG_CONSOLE_ENABLED);
    }

    public static boolean isFileEnabled() {
        return isTrue(LoggingOptions.LOG_FILE_ENABLED);
    }

    public static boolean isSyslogEnabled() {
        return isTrue(LoggingOptions.LOG_SYSLOG_ENABLED);
    }

    private static BiFunction<Optional<String>, ConfigSourceInterceptorContext, Optional<String>> resolveLogHandler(String handler) {
        return (parentValue, context) -> {
            String handlers = parentValue.get();

            String[] logHandlerValues = handlers.split(",");

            return of(String.valueOf(Stream.of(logHandlerValues).anyMatch(handler::equals)));
        };
    }

    private static Optional<String> resolveFileLogLocation(Optional<String> value, ConfigSourceInterceptorContext configSourceInterceptorContext) {
        return value.map(location -> location.endsWith(File.separator) ? location + LoggingOptions.DEFAULT_LOG_FILENAME : location);
    }

    private static Level toLevel(String categoryLevel) throws IllegalArgumentException {
        return LogContext.getLogContext().getLevelForName(categoryLevel.toUpperCase(Locale.ROOT));
    }

    private static void setCategoryLevel(String category, String level) {
        LogContext.getLogContext().getLogger(category).setLevel(toLevel(level));
    }

    record CategoryLevel(String category, String levelName) {}

    private static CategoryLevel validateLogLevel(String level) {
        String[] parts = level.split(":");
        String category = null;
        String categoryLevel;

        if (parts.length == 1) {
            categoryLevel = parts[0];
        } else if (parts.length == 2) {
            category = parts[0];
            categoryLevel = parts[1];
        } else {
            throw new PropertyException(Messages.invalidLogCategoryFormat(level));
        }

        try {
            Level levelType = toLevel(categoryLevel);
            return new CategoryLevel(category, levelType.getName());
        } catch (IllegalArgumentException iae) {
            throw new PropertyException(Messages.invalidLogCategoryFormat(level));
        }
    }

    private static Optional<String> resolveLogLevel(Optional<String> value, ConfigSourceInterceptorContext configSourceInterceptorContext) {
        Optional<String> rootLevel = of(LoggingOptions.DEFAULT_LOG_LEVEL.name());

        for (String level : value.get().split(",")) {
            var categoryLevel = validateLogLevel(level);
            if (categoryLevel.category == null) {
                rootLevel = of(categoryLevel.levelName);
            } else {
                setCategoryLevel(categoryLevel.category, categoryLevel.levelName);
            }
        }

        return rootLevel;
    }

    private static Optional<String> resolveLogOutput(Optional<String> value, ConfigSourceInterceptorContext context) {
        if (value.get().equals(LoggingOptions.DEFAULT_CONSOLE_OUTPUT.name().toLowerCase(Locale.ROOT))) {
            return of(Boolean.FALSE.toString());
        }

        return of(Boolean.TRUE.toString());
    }

    /**
     * Add tracing info to the log if the format is not explicitly set, and tracing and {@code includeTraceOption} options are enabled
     */
    private static Optional<String> addTracingInfo(Optional<String> value, Option<Boolean> includeTraceOption) {
        var isTracingEnabled = TracingPropertyMappers.isTracingEnabled();
        var includeTrace = Configuration.isTrue(includeTraceOption);
        var isChangedLogFormat = !DEFAULT_LOG_FORMAT.equals(value.get());

        if (!isTracingEnabled || !includeTrace || isChangedLogFormat) {
            return value;
        }

        return Optional.of(LoggingOptions.DEFAULT_LOG_TRACING_FORMAT);
    }

    private static void validateSyslogMaxLength(String value) {
        var converter = new MemorySizeConverter();
        try {
            converter.convert(value);
        } catch (IllegalArgumentException e) {
            throw new PropertyException(String.format("Invalid value for option '--log-syslog-max-length': %s", e.getMessage()));
        }
    }
}
