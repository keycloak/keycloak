package org.keycloak.quarkus.runtime.configuration.mappers;

import static java.util.Optional.of;
import static org.keycloak.config.LoggingOptions.DEFAULT_LOG_FORMAT;
import static org.keycloak.config.LoggingOptions.LOG_LEVEL;
import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.stream.Stream;

import jakarta.ws.rs.core.MultivaluedHashMap;
import org.jboss.logmanager.LogContext;
import org.keycloak.config.LoggingOptions;
import org.keycloak.config.Option;

import io.smallrye.config.ConfigSourceInterceptorContext;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.LogLevelFormat;

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
                        .transformer(LoggingPropertyMappers::resolveRootLoggerDefault)
                        .validator(LogLevelFormat::validateLogLevel)
                        .mapFrom(LOG_LEVEL)
                        .paramLabel("category:level")
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
                        .validator(LogLevelFormat::validateLogLevel)
                        .transformer(LoggingPropertyMappers::resolveRootLoggerDefault)
                        .mapFrom(LOG_LEVEL)
                        .paramLabel("category:level")
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
                fromOption(LOG_LEVEL)
                        .to("quarkus.log.level")
                        .transformer(LoggingPropertyMappers::resolveLogLevel)
                        .validator(LogLevelFormat::validateLogLevel)
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
                        .validator(LogLevelFormat::validateLogLevel)
                        .transformer(LoggingPropertyMappers::resolveRootLoggerDefault)
                        .mapFrom(LOG_LEVEL)
                        .paramLabel("category:level")
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

    private static Optional<String> resolveRootLoggerDefault(Optional<String> value, ConfigSourceInterceptorContext configSourceInterceptorContext) {
        if (value.isPresent()) {
            return value;
        }

        var rootLevel = LogLevelFormat.parseFromProperty(LOG_LEVEL);
        return rootLevel.getRootLevel().map(Level::toString).or(() -> of(LoggingOptions.DEFAULT_LOG_LEVEL.toString()));
    }

    private static void setCategoryLevel(String category, Level level) {
        LogContext.getLogContext().getLogger(category).setLevel(level);
    }

    /**
     * The 'quarkus.log.level' needs to be set the most fine-grained based on the log handlers configuration to properly see the log records
     *
     * @param value parent LogLevelContext
     * @return the finest log level in log levels configuration
     */
    private static Optional<String> resolveLogLevel(Optional<String> value, ConfigSourceInterceptorContext configSourceInterceptorContext) {
        final List<LogLevelFormat> levelContexts = new ArrayList<>(LoggingOptions.Handler.values().length + 3);

        // find parent log levels
        var parentLevelContext = LogLevelFormat.parseValue(value.get());
        levelContexts.add(parentLevelContext);

        // find all log handlers levels
        for (var key : Configuration.getConfig().getPropertyNames()) {
            if (key.startsWith(NS_KEYCLOAK_PREFIX + "log-") && key.endsWith("-level") && !key.equals(NS_KEYCLOAK_PREFIX + "log-level")) {
                var keyWithoutPrefix = key.substring(NS_KEYCLOAK_PREFIX.length());
                levelContexts.add(LogLevelFormat.parseFromProperty(keyWithoutPrefix));
            }
        }

        // set the finest level for categories
        var categoryLevels = new MultivaluedHashMap<String, Level>();
        levelContexts.stream()
                .map(LogLevelFormat::getCategories)
                .flatMap(f -> f.entrySet().stream())
                .forEach(f -> categoryLevels.putSingle(f.getKey(), f.getValue()));

        categoryLevels.forEach((k, v) -> {
            var finestLevel = v.stream().min(Comparator.comparingInt(Level::intValue));
            finestLevel.ifPresent(level -> setCategoryLevel(k, level)); // set finest category levels
        });

        // obtain the finest level of root levels of all log handlers
        var finestLevel = levelContexts.stream()
                .map(LogLevelFormat::getRootLevel)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Comparator.comparingInt(Level::intValue));

        return finestLevel.map(Level::toString)
                .or(() -> Optional.of(LoggingOptions.DEFAULT_LOG_LEVEL.toString()))
                .map(String::toLowerCase);
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
}
