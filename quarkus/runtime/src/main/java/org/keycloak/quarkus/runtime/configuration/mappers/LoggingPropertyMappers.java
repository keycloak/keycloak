package org.keycloak.quarkus.runtime.configuration.mappers;

import static java.util.Optional.of;
import static org.keycloak.config.LoggingOptions.GELF_ACTIVATED;
import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;
import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Level;

import org.apache.commons.lang3.ArrayUtils;
import org.jboss.logmanager.LogContext;
import org.keycloak.config.LoggingOptions;
import org.keycloak.quarkus.runtime.Messages;

import io.smallrye.config.ConfigSourceInterceptorContext;

public final class LoggingPropertyMappers {

    private static final String CONSOLE_ENABLED_MSG = "Console log handler is activated";
    private static final String FILE_ENABLED_MSG = "File log handler is activated";
    private static final String GELF_ENABLED_MSG = "GELF is activated";

    private LoggingPropertyMappers() {
    }

    public static PropertyMapper<?>[] getMappers() {
        PropertyMapper<?>[] defaultMappers = new PropertyMapper[]{
                fromOption(LoggingOptions.LOG)
                        .paramLabel("<handler>")
                        .build(),
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
                        .paramLabel("<format>")
                        .build(),
                fromOption(LoggingOptions.LOG_FILE_OUTPUT)
                        .isEnabled(LoggingPropertyMappers::isFileEnabled, FILE_ENABLED_MSG)
                        .to("quarkus.log.file.json")
                        .paramLabel("output")
                        .transformer(LoggingPropertyMappers::resolveLogOutput)
                        .build(),
                fromOption(LoggingOptions.LOG_LEVEL)
                        .to("quarkus.log.level")
                        .transformer(LoggingPropertyMappers::resolveLogLevel)
                        .paramLabel("category:level")
                        .build()
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

    private static BiFunction<Optional<String>, ConfigSourceInterceptorContext, Optional<String>> resolveLogHandler(String handler) {
        return (parentValue, context) -> {
            //we want to fall back to console to not have nothing shown up when wrong values are set.
            String consoleDependantErrorResult = handler.equals(LoggingOptions.DEFAULT_LOG_HANDLER.name()) ? Boolean.TRUE.toString() : Boolean.FALSE.toString();
            String handlers = parentValue.get();

            if (handlers.isBlank()) {
                addInitializationException(Messages.emptyValueForKey("log"));
                return of(consoleDependantErrorResult);
            }

            String[] logHandlerValues = handlers.split(",");
            final List<String> availableLogHandlers = LoggingOptions.getAvailableHandlerNames();

            if (!availableLogHandlers.containsAll(List.of(logHandlerValues))) {
                addInitializationException(Messages.notRecognizedValueInList("log", handlers, String.join(",", availableLogHandlers)));
                return of(consoleDependantErrorResult);
            }

            for (String handlerInput : logHandlerValues) {
                if (handlerInput.equals(handler)) {
                    return of(Boolean.TRUE.toString());
                }
            }

            return of(Boolean.FALSE.toString());
        };
    }

    private static Optional<String> resolveFileLogLocation(Optional<String> value, ConfigSourceInterceptorContext configSourceInterceptorContext) {
        String location = value.get();

        if (location.endsWith(File.separator)) {
            return of(location + LoggingOptions.DEFAULT_LOG_FILENAME);
        }

        return value;
    }

    private static Level toLevel(String categoryLevel) throws IllegalArgumentException {
        return LogContext.getLogContext().getLevelForName(categoryLevel.toUpperCase(Locale.ROOT));
    }

    private static void setCategoryLevel(String category, String level) {
        LogContext.getLogContext().getLogger(category).setLevel(toLevel(level));
    }

    private static Optional<String> resolveLogLevel(Optional<String> value, ConfigSourceInterceptorContext configSourceInterceptorContext) {
        Optional<String> rootLevel = of(LoggingOptions.DEFAULT_LOG_LEVEL.name());

        for (String level : value.get().split(",")) {
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
                rootLevel = of(levelType.getName());
            } else {
                setCategoryLevel(category, levelType.getName());
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
