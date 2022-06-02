package org.keycloak.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LoggingOptions {

    public static final Handler DEFAULT_LOG_HANDLER = Handler.console;
    public static final Level DEFAULT_LOG_LEVEL = Level.INFO;
    public static final Output DEFAULT_CONSOLE_OUTPUT = Output.DEFAULT;
    public static final String DEFAULT_LOG_FILENAME = "keycloak.log";
    public static final String DEFAULT_LOG_PATH = "data" + File.separator + "log" + File.separator + DEFAULT_LOG_FILENAME;

    public enum Handler {
        console,
        file;
    }

    public static final Option log = new OptionBuilder("log", List.class, Handler.class)
            .category(OptionCategory.LOGGING)
            .description("Enable one or more log handlers in a comma-separated list. Available log handlers are: " + Arrays.stream(Handler.values()).limit(2).map(h -> h.toString()).collect(Collectors.joining(",")))
            .defaultValue(DEFAULT_LOG_HANDLER)
            .expectedValues(Handler.values())
            .build();

    public enum Level {
        OFF,
        FATAL,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE,
        ALL;

        @Override
        public String toString() {
            return super.toString().toLowerCase(Locale.ROOT);
        }
    }

    public static final Option<Level> LOG_LEVEL = new OptionBuilder<>("log-level", Level.class)
            .category(OptionCategory.LOGGING)
            .defaultValue(DEFAULT_LOG_LEVEL)
            .description("The log level of the root category or a comma-separated list of individual categories and their levels. For the root category, you don't need to specify a category.")
            .build();

    public enum Output {
        DEFAULT,
        JSON;

        @Override
        public String toString() {
            return super.toString().toLowerCase(Locale.ROOT);
        }
    }
    public static final Option LOG_CONSOLE_OUTPUT = new OptionBuilder<>("log-console-output", Output.class)
            .category(OptionCategory.LOGGING)
            .defaultValue(DEFAULT_CONSOLE_OUTPUT)
            .description("Set the log output to JSON or default (plain) unstructured logging.")
            .expectedValues(Output.values())
            .build();

    public static final Option LOG_CONSOLE_FORMAT = new OptionBuilder<>("log-console-format", String.class)
            .category(OptionCategory.LOGGING)
            .description("The format of unstructured console log entries. If the format has spaces in it, escape the value using \"<format>\".")
            .defaultValue("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n")
            .build();

    public static final Option LOG_CONSOLE_COLOR = new OptionBuilder<>("log-console-color", Boolean.class)
            .category(OptionCategory.LOGGING)
            .description("Enable or disable colors when logging to console.")
            .defaultValue(Boolean.FALSE) // :-(
            .build();

    public static final Option<Boolean> LOG_CONSOLE_ENABLED = new OptionBuilder<>("log-console-enabled", Boolean.class)
            .category(OptionCategory.LOGGING)
            .runtimes(Collections.emptySet())
            .build();

    public static final Option LOG_FILE_ENABLED = new OptionBuilder<>("log-file-enabled", Boolean.class)
            .category(OptionCategory.LOGGING)
            .runtimes(Collections.emptySet())
            .build();

    public static final Option<File> LOG_FILE = new OptionBuilder<>("log-file", File.class)
            .category(OptionCategory.LOGGING)
            .description("Set the log file path and filename.")
            .defaultValue(new File(DEFAULT_LOG_PATH))
            .build();

    public static final Option LOG_FILE_FORMAT = new OptionBuilder<>("log-file-format", String.class)
            .category(OptionCategory.LOGGING)
            .description("Set a format specific to file log entries.")
            .defaultValue("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n")
            .build();

    public static final List<Option<?>> ALL_OPTIONS = new ArrayList<>();

    static {
        ALL_OPTIONS.add(log);
        ALL_OPTIONS.add(LOG_LEVEL);
        ALL_OPTIONS.add(LOG_CONSOLE_OUTPUT);
        ALL_OPTIONS.add(LOG_CONSOLE_FORMAT);
        ALL_OPTIONS.add(LOG_CONSOLE_COLOR);
        ALL_OPTIONS.add(LOG_CONSOLE_ENABLED);
        ALL_OPTIONS.add(LOG_FILE_ENABLED);
        ALL_OPTIONS.add(LOG_FILE);
        ALL_OPTIONS.add(LOG_FILE_FORMAT);
    }
}
