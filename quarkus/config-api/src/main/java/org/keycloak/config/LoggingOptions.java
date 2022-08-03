package org.keycloak.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
        file,
        gelf
    }

    public static final Option LOG = new OptionBuilder("log", List.class, Handler.class)
            .category(OptionCategory.LOGGING)
            .description("Enable one or more log handlers in a comma-separated list. Available log handlers are: " + Arrays.stream(Handler.values()).map(Enum::toString).collect(Collectors.joining(",")))
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
    public static final Option<Output> LOG_CONSOLE_OUTPUT = new OptionBuilder<>("log-console-output", Output.class)
            .category(OptionCategory.LOGGING)
            .defaultValue(DEFAULT_CONSOLE_OUTPUT)
            .description("Set the log output to JSON or default (plain) unstructured logging.")
            .expectedValues(Output.values())
            .build();

    public static final Option<String> LOG_CONSOLE_FORMAT = new OptionBuilder<>("log-console-format", String.class)
            .category(OptionCategory.LOGGING)
            .description("The format of unstructured console log entries. If the format has spaces in it, escape the value using \"<format>\".")
            .defaultValue("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n")
            .build();

    public static final Option<Boolean> LOG_CONSOLE_COLOR = new OptionBuilder<>("log-console-color", Boolean.class)
            .category(OptionCategory.LOGGING)
            .description("Enable or disable colors when logging to console.")
            .defaultValue(Boolean.FALSE) // :-(
            .build();

    public static final Option<Boolean> LOG_CONSOLE_ENABLED = new OptionBuilder<>("log-console-enabled", Boolean.class)
            .category(OptionCategory.LOGGING)
            .hidden()
            .build();

    public static final Option<Boolean> LOG_FILE_ENABLED = new OptionBuilder<>("log-file-enabled", Boolean.class)
            .category(OptionCategory.LOGGING)
            .hidden()
            .build();

    public static final Option<File> LOG_FILE = new OptionBuilder<>("log-file", File.class)
            .category(OptionCategory.LOGGING)
            .description("Set the log file path and filename.")
            .defaultValue(new File(DEFAULT_LOG_PATH))
            .build();

    public static final Option<String> LOG_FILE_FORMAT = new OptionBuilder<>("log-file-format", String.class)
            .category(OptionCategory.LOGGING)
            .description("Set a format specific to file log entries.")
            .defaultValue("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n")
            .build();

    public static final Option<Boolean> LOG_GELF_ENABLED = new OptionBuilder<>("log-gelf-enabled", Boolean.class)
            .category(OptionCategory.LOGGING)
            .hidden()
            .build();

    public static final Option<String> LOG_GELF_LEVEL = new OptionBuilder<>("log-gelf-level", String.class)
            .category(OptionCategory.LOGGING)
            .defaultValue("INFO")
            .description("The log level specifying which message levels will be logged by the GELF logger. Message levels lower than this value will be discarded.")
            .build();

    public static final Option<String> LOG_GELF_HOST = new OptionBuilder<>("log-gelf-host", String.class)
            .category(OptionCategory.LOGGING)
            .description("Hostname of the Logstash or Graylog Host. By default UDP is used, prefix the host with 'tcp:' to switch to TCP. Example: 'tcp:localhost'")
            .defaultValue("localhost")
            .build();

    public static final Option<Integer> LOG_GELF_PORT = new OptionBuilder<>("log-gelf-port", Integer.class)
            .category(OptionCategory.LOGGING)
            .description("The port the Logstash or Graylog Host is called on.")
            .defaultValue(12201)
            .build();

    public static final Option<String> LOG_GELF_VERSION = new OptionBuilder<>("log-gelf-version", String.class)
            .category(OptionCategory.LOGGING)
            .description("The GELF version to be used.")
            .defaultValue("1.1")
            .hidden()
            .expectedValues("1.0", "1.1")
            .build();

    public static final Option<Boolean> LOG_GELF_INCLUDE_STACK_TRACE = new OptionBuilder<>("log-gelf-include-stack-trace", Boolean.class)
            .category(OptionCategory.LOGGING)
            .description("If set to true, occuring stack traces are included in the 'StackTrace' field in the GELF output.")
            .defaultValue(Boolean.TRUE)
            .expectedValues(Boolean.TRUE, Boolean.FALSE)
            .build();

    public static final Option<String> LOG_GELF_TIMESTAMP_FORMAT = new OptionBuilder<>("log-gelf-timestamp-format", String.class)
            .category(OptionCategory.LOGGING)
            .description("Set the format for the GELF timestamp field. Uses Java SimpleDateFormat pattern.")
            .defaultValue("yyyy-MM-dd HH:mm:ss,SSS")
            .build();

    public static final Option<String> LOG_GELF_FACILITY = new OptionBuilder<>("log-gelf-facility", String.class)
            .category(OptionCategory.LOGGING)
            .description("The facility (name of the process) that sends the message.")
            .defaultValue("keycloak")
            .build();

    public static final Option<Integer> LOG_GELF_MAX_MSG_SIZE = new OptionBuilder<>("log-gelf-max-message-size", Integer.class)
            .category(OptionCategory.LOGGING)
            .description("Maximum message size (in bytes). If the message size is exceeded, GELF will submit the message in multiple chunks.")
            .defaultValue(8192)
            .build();

    public static final Option<Boolean> LOG_GELF_INCLUDE_LOG_MSG_PARAMS = new OptionBuilder<>("log-gelf-include-message-parameters", Boolean.class)
            .category(OptionCategory.LOGGING)
            .description("Include message parameters from the log event.")
            .defaultValue(Boolean.TRUE)
            .build();

    public static final Option<Boolean> LOG_GELF_INCLUDE_LOCATION = new OptionBuilder<>("log-gelf-include-location", Boolean.class)
            .category(OptionCategory.LOGGING)
            .description("Include source code location.")
            .defaultValue(Boolean.TRUE)
            .build();

    public static final List<Option<?>> ALL_OPTIONS = new ArrayList<>();

    static {
        ALL_OPTIONS.add(LOG);
        ALL_OPTIONS.add(LOG_LEVEL);
        ALL_OPTIONS.add(LOG_CONSOLE_OUTPUT);
        ALL_OPTIONS.add(LOG_CONSOLE_FORMAT);
        ALL_OPTIONS.add(LOG_CONSOLE_COLOR);
        ALL_OPTIONS.add(LOG_CONSOLE_ENABLED);
        ALL_OPTIONS.add(LOG_FILE_ENABLED);
        ALL_OPTIONS.add(LOG_FILE);
        ALL_OPTIONS.add(LOG_FILE_FORMAT);
        ALL_OPTIONS.add(LOG_GELF_ENABLED);
        ALL_OPTIONS.add(LOG_GELF_HOST);
        ALL_OPTIONS.add(LOG_GELF_PORT);
        ALL_OPTIONS.add(LOG_GELF_VERSION);
        ALL_OPTIONS.add(LOG_GELF_INCLUDE_STACK_TRACE);
        ALL_OPTIONS.add(LOG_GELF_TIMESTAMP_FORMAT);
        ALL_OPTIONS.add(LOG_GELF_FACILITY);
        ALL_OPTIONS.add(LOG_GELF_MAX_MSG_SIZE);
        ALL_OPTIONS.add(LOG_GELF_INCLUDE_LOG_MSG_PARAMS);
        ALL_OPTIONS.add(LOG_GELF_INCLUDE_LOCATION);
    }
}
