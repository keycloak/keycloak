package org.keycloak.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public class LoggingOptions {

    public static final Handler DEFAULT_LOG_HANDLER = Handler.console;
    public static final Level DEFAULT_LOG_LEVEL = Level.INFO;
    public static final Output DEFAULT_CONSOLE_OUTPUT = Output.DEFAULT;
    public static final Output DEFAULT_SYSLOG_OUTPUT = Output.DEFAULT;
    public static final String DEFAULT_LOG_FILENAME = "keycloak.log";
    public static final String DEFAULT_LOG_PATH = "data" + File.separator + "log" + File.separator + DEFAULT_LOG_FILENAME;
    public static final Boolean GELF_ACTIVATED = isGelfActivated();

    public enum Handler {
        console,
        file,
        syslog,
        gelf
    }

    public static List<String> getAvailableHandlerNames() {
        final Predicate<Handler> checkGelf = (handler) -> GELF_ACTIVATED || !handler.equals(Handler.gelf);

        return Arrays.stream(Handler.values())
                .filter(checkGelf)
                .map(Handler::name)
                .toList();
    }

    private static Option<List<Handler>> createLogOption() {
        OptionBuilder<List<Handler>> logOptionBuilder = OptionBuilder.listOptionBuilder("log", Handler.class)
                .category(OptionCategory.LOGGING)
                .description("Enable one or more log handlers in a comma-separated list.")
                .expectedValues(getAvailableHandlerNames())
                .defaultValue(Arrays.asList(DEFAULT_LOG_HANDLER));

        if (GELF_ACTIVATED) {
            logOptionBuilder.deprecatedValues(Set.of("gelf"), "GELF log handler has been deprecated.");
        }

        return logOptionBuilder.build();
    }

    public static final Option<List<Handler>> LOG = createLogOption();

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

    public static final Option<List<String>> LOG_LEVEL = OptionBuilder.listOptionBuilder("log-level", String.class)
            .category(OptionCategory.LOGGING)
            .defaultValue(Arrays.asList(DEFAULT_LOG_LEVEL.toString()))
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

    // Console
    public static final Option<Output> LOG_CONSOLE_OUTPUT = new OptionBuilder<>("log-console-output", Output.class)
            .category(OptionCategory.LOGGING)
            .defaultValue(DEFAULT_CONSOLE_OUTPUT)
            .description("Set the log output to JSON or default (plain) unstructured logging.")
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

    // File
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

    public static final Option<Output> LOG_FILE_OUTPUT = new OptionBuilder<>("log-file-output", Output.class)
            .category(OptionCategory.LOGGING)
            .defaultValue(DEFAULT_CONSOLE_OUTPUT)
            .description("Set the log output to JSON or default (plain) unstructured logging.")
            .build();

    // Syslog
    public static final Option<Boolean> LOG_SYSLOG_ENABLED = new OptionBuilder<>("log-syslog-enabled", Boolean.class)
            .category(OptionCategory.LOGGING)
            .hidden()
            .build();

    public static final Option<String> LOG_SYSLOG_ENDPOINT = new OptionBuilder<>("log-syslog-endpoint", String.class)
            .category(OptionCategory.LOGGING)
            .description("The IP address and port of the syslog server.")
            .defaultValue("localhost:514")
            .build();

    public static final Option<String> LOG_SYSLOG_APP_NAME = new OptionBuilder<>("log-syslog-app-name", String.class)
            .category(OptionCategory.LOGGING)
            .description("The app name used when formatting the message in RFC5424 format.")
            .defaultValue("keycloak")
            .hidden()
            .build();

    public static final Option<String> LOG_SYSLOG_PROTOCOL = new OptionBuilder<>("log-syslog-protocol", String.class)
            .category(OptionCategory.LOGGING)
            .description("Sets the protocol used to connect to the syslog server.")
            .defaultValue("tcp")
            .expectedValues("tcp", "udp", "ssl-tcp")
            .build();

    public static final Option<String> LOG_SYSLOG_FORMAT = new OptionBuilder<>("log-syslog-format", String.class)
            .category(OptionCategory.LOGGING)
            .description("Set a format specific to syslog entries.")
            .defaultValue("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n")
            .build();

    public static final Option<Output> LOG_SYSLOG_OUTPUT = new OptionBuilder<>("log-syslog-output", Output.class)
            .category(OptionCategory.LOGGING)
            .defaultValue(DEFAULT_SYSLOG_OUTPUT)
            .description("Set the syslog output to JSON or default (plain) unstructured logging.")
            .build();

    // GELF
    public static final Option<Boolean> LOG_GELF_ENABLED = new OptionBuilder<>("log-gelf-enabled", Boolean.class)
            .category(OptionCategory.LOGGING)
            .hidden()
            .build();

    public static final Option<String> LOG_GELF_LEVEL = new OptionBuilder<>("log-gelf-level", String.class)
            .category(OptionCategory.LOGGING)
            .defaultValue("INFO")
            .description("The log level specifying which message levels will be logged by the GELF logger. Message levels lower than this value will be discarded.")
            .deprecated()
            .build();

    public static final Option<String> LOG_GELF_HOST = new OptionBuilder<>("log-gelf-host", String.class)
            .category(OptionCategory.LOGGING)
            .description("Hostname of the Logstash or Graylog Host. By default UDP is used, prefix the host with 'tcp:' to switch to TCP. Example: 'tcp:localhost'")
            .defaultValue("localhost")
            .deprecated()
            .build();

    public static final Option<Integer> LOG_GELF_PORT = new OptionBuilder<>("log-gelf-port", Integer.class)
            .category(OptionCategory.LOGGING)
            .description("The port the Logstash or Graylog Host is called on.")
            .deprecated()
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
            .description("If set to true, occurring stack traces are included in the 'StackTrace' field in the GELF output.")
            .defaultValue(Boolean.TRUE)
            .deprecated()
            .build();

    public static final Option<String> LOG_GELF_TIMESTAMP_FORMAT = new OptionBuilder<>("log-gelf-timestamp-format", String.class)
            .category(OptionCategory.LOGGING)
            .description("Set the format for the GELF timestamp field. Uses Java SimpleDateFormat pattern.")
            .defaultValue("yyyy-MM-dd HH:mm:ss,SSS")
            .deprecated()
            .build();

    public static final Option<String> LOG_GELF_FACILITY = new OptionBuilder<>("log-gelf-facility", String.class)
            .category(OptionCategory.LOGGING)
            .description("The facility (name of the process) that sends the message.")
            .defaultValue("keycloak")
            .deprecated()
            .build();

    public static final Option<Integer> LOG_GELF_MAX_MSG_SIZE = new OptionBuilder<>("log-gelf-max-message-size", Integer.class)
            .category(OptionCategory.LOGGING)
            .description("Maximum message size (in bytes). If the message size is exceeded, GELF will submit the message in multiple chunks.")
            .defaultValue(8192)
            .deprecated()
            .build();

    public static final Option<Boolean> LOG_GELF_INCLUDE_LOG_MSG_PARAMS = new OptionBuilder<>("log-gelf-include-message-parameters", Boolean.class)
            .category(OptionCategory.LOGGING)
            .description("Include message parameters from the log event.")
            .defaultValue(Boolean.TRUE)
            .deprecated()
            .build();

    public static final Option<Boolean> LOG_GELF_INCLUDE_LOCATION = new OptionBuilder<>("log-gelf-include-location", Boolean.class)
            .category(OptionCategory.LOGGING)
            .description("Include source code location.")
            .defaultValue(Boolean.TRUE)
            .deprecated()
            .build();

    private static boolean isGelfActivated() {
        try {
            Thread.currentThread().getContextClassLoader().loadClass("io.quarkus.logging.gelf.GelfConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
