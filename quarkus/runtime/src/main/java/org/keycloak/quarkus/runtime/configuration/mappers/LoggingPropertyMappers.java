package org.keycloak.quarkus.runtime.configuration.mappers;

import static java.util.Optional.of;
import static org.keycloak.config.LoggingOptions.GELF_ACTIVATED;
import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.jboss.logmanager.LogContext;
import org.keycloak.config.LoggingOptions;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.cli.PropertyException;

import io.smallrye.config.ConfigSourceInterceptorContext;

public final class LoggingPropertyMappers {

    private static final String CONSOLE_ENABLED_MSG = "Console log handler is activated";
    private static final String FILE_ENABLED_MSG = "File log handler is activated";
    private static final String SYSLOG_ENABLED_MSG = "Syslog is activated";
    private static final String GELF_ENABLED_MSG = "GELF is activated";

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
                fromOption(LoggingOptions.LOG_CONSOLE_FORMAT)
                        .isEnabled(LoggingPropertyMappers::isConsoleEnabled, CONSOLE_ENABLED_MSG)
                        .to("quarkus.log.console.format")
                        .paramLabel("format")
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
                fromOption(LoggingOptions.LOG_FILE_FORMAT)
                        .isEnabled(LoggingPropertyMappers::isFileEnabled, FILE_ENABLED_MSG)
                        .to("quarkus.log.file.format")
                        .paramLabel("format")
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
                        .validator((mapper, value) -> mapper.validateExpectedValues(value,
                                (c, v) -> validateLogLevel(v)))
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
                fromOption(LoggingOptions.LOG_SYSLOG_APP_NAME)
                        .isEnabled(LoggingPropertyMappers::isSyslogEnabled, SYSLOG_ENABLED_MSG)
                        .to("quarkus.log.syslog.app-name")
                        .paramLabel("name")
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
                        .build(),
                fromOption(LoggingOptions.LOG_SYSLOG_OUTPUT)
                        .isEnabled(LoggingPropertyMappers::isSyslogEnabled, SYSLOG_ENABLED_MSG)
                        .to("quarkus.log.syslog.json")
                        .paramLabel("output")
                        .transformer(LoggingPropertyMappers::resolveLogOutput)
                        .build(),
        };

        return GELF_ACTIVATED ? ArrayUtils.addAll(defaultMappers, getGelfMappers()) : defaultMappers;
    }

    public static PropertyMapper<?>[] getGelfMappers() {
        return new PropertyMapper[]{
                fromOption(LoggingOptions.LOG_GELF_ENABLED)
                        .mapFrom("log")
                        .to("quarkus.log.handler.gelf.enabled")
                        .transformer(LoggingPropertyMappers.resolveLogHandler("gelf"))
                        .build(),
                fromOption(LoggingOptions.LOG_GELF_LEVEL)
                        .isEnabled(LoggingPropertyMappers::isGelfEnabled, GELF_ENABLED_MSG)
                        .to("quarkus.log.handler.gelf.level")
                        .paramLabel("level")
                        .build(),
                fromOption(LoggingOptions.LOG_GELF_HOST)
                        .isEnabled(LoggingPropertyMappers::isGelfEnabled, GELF_ENABLED_MSG)
                        .to("quarkus.log.handler.gelf.host")
                        .paramLabel("hostname")
                        .build(),
                fromOption(LoggingOptions.LOG_GELF_PORT)
                        .isEnabled(LoggingPropertyMappers::isGelfEnabled, GELF_ENABLED_MSG)
                        .to("quarkus.log.handler.gelf.port")
                        .paramLabel("port")
                        .build(),
                fromOption(LoggingOptions.LOG_GELF_VERSION)
                        .isEnabled(LoggingPropertyMappers::isGelfEnabled, GELF_ENABLED_MSG)
                        .to("quarkus.log.handler.gelf.version")
                        .paramLabel("version")
                        .build(),
                fromOption(LoggingOptions.LOG_GELF_INCLUDE_STACK_TRACE)
                        .isEnabled(LoggingPropertyMappers::isGelfEnabled, GELF_ENABLED_MSG)
                        .to("quarkus.log.handler.gelf.extract-stack-trace")
                        .build(),
                fromOption(LoggingOptions.LOG_GELF_TIMESTAMP_FORMAT)
                        .isEnabled(LoggingPropertyMappers::isGelfEnabled, GELF_ENABLED_MSG)
                        .to("quarkus.log.handler.gelf.timestamp-pattern")
                        .paramLabel("pattern")
                        .build(),
                fromOption(LoggingOptions.LOG_GELF_FACILITY)
                        .isEnabled(LoggingPropertyMappers::isGelfEnabled, GELF_ENABLED_MSG)
                        .to("quarkus.log.handler.gelf.facility")
                        .paramLabel("name")
                        .build(),
                fromOption(LoggingOptions.LOG_GELF_MAX_MSG_SIZE)
                        .isEnabled(LoggingPropertyMappers::isGelfEnabled, GELF_ENABLED_MSG)
                        .to("quarkus.log.handler.gelf.maximum-message-size")
                        .paramLabel("size")
                        .build(),
                fromOption(LoggingOptions.LOG_GELF_INCLUDE_LOG_MSG_PARAMS)
                        .isEnabled(LoggingPropertyMappers::isGelfEnabled, GELF_ENABLED_MSG)
                        .to("quarkus.log.handler.gelf.include-log-message-parameters")
                        .build(),
                fromOption(LoggingOptions.LOG_GELF_INCLUDE_LOCATION)
                        .isEnabled(LoggingPropertyMappers::isGelfEnabled, GELF_ENABLED_MSG)
                        .to("quarkus.log.handler.gelf.include-location")
                        .build()
        };
    }

    public static boolean isGelfEnabled() {
        return isTrue(LoggingOptions.LOG_GELF_ENABLED);
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
}
