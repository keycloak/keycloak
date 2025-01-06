package org.keycloak.config;

import io.quarkus.runtime.configuration.MemorySize;
import org.jboss.logmanager.handlers.SyslogHandler;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static java.lang.String.format;

public class LoggingOptions {

    public static final Handler DEFAULT_LOG_HANDLER = Handler.console;
    public static final Level DEFAULT_LOG_LEVEL = Level.INFO;
    public static final Output DEFAULT_CONSOLE_OUTPUT = Output.DEFAULT;
    public static final Output DEFAULT_SYSLOG_OUTPUT = Output.DEFAULT;
    public static final String DEFAULT_LOG_FILENAME = "keycloak.log";
    public static final String DEFAULT_LOG_PATH = "data" + File.separator + "log" + File.separator + DEFAULT_LOG_FILENAME;

    // Log format + tracing
    private static final Function<String, String> DEFAULT_LOG_FORMAT_FUNC = (additionalFields) ->
            "%d{yyyy-MM-dd HH:mm:ss,SSS} " + additionalFields + "%-5p [%c] (%t) %s%e%n";
    public static final String DEFAULT_LOG_FORMAT = DEFAULT_LOG_FORMAT_FUNC.apply("");
    public static final String DEFAULT_LOG_TRACING_FORMAT = DEFAULT_LOG_FORMAT_FUNC.apply("traceId=%X{traceId}, parentId=%X{parentId}, spanId=%X{spanId}, sampled=%X{sampled} ");

    public enum Handler {
        console,
        file,
        syslog
    }

    public static final Option<List<Handler>> LOG = OptionBuilder.listOptionBuilder("log", Handler.class)
            .category(OptionCategory.LOGGING)
            .description("Enable one or more log handlers in a comma-separated list.")
            .defaultValue(List.of(DEFAULT_LOG_HANDLER))
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

    public static final Option<Level> LOG_CONSOLE_LEVEL = new OptionBuilder<>("log-console-level", Level.class)
            .category(OptionCategory.LOGGING)
            .defaultValue(Level.ALL)
            .description("Set the log level for the console handler. It specifies the most verbose log level for logs shown in the output. "
                    + "It respects levels specified in the 'log-level' option, which represents the maximal verbosity for the whole logging system. "
                    + "For more information, check the Logging guide.")
            .build();

    public static final Option<String> LOG_CONSOLE_FORMAT = new OptionBuilder<>("log-console-format", String.class)
            .category(OptionCategory.LOGGING)
            .description("The format of unstructured console log entries. If the format has spaces in it, escape the value using \"<format>\".")
            .defaultValue(DEFAULT_LOG_FORMAT)
            .build();

    public static final Option<Boolean> LOG_CONSOLE_INCLUDE_TRACE = new OptionBuilder<>("log-console-include-trace", Boolean.class)
            .category(OptionCategory.LOGGING)
            .description(format("Include tracing information in the console log. If the '%s' option is specified, this option has no effect.", LOG_CONSOLE_FORMAT.getKey()))
            .defaultValue(true)
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

    public static final Option<Level> LOG_FILE_LEVEL = new OptionBuilder<>("log-file-level", Level.class)
            .category(OptionCategory.LOGGING)
            .defaultValue(Level.ALL)
            .description("Set the log level for the file handler. It specifies the most verbose log level for logs shown in the output. "
                    + "It respects levels specified in the 'log-level' option, which represents the maximal verbosity for the whole logging system. "
                    + "For more information, check the Logging guide.")
            .build();

    public static final Option<String> LOG_FILE_FORMAT = new OptionBuilder<>("log-file-format", String.class)
            .category(OptionCategory.LOGGING)
            .description("Set a format specific to file log entries.")
            .defaultValue(DEFAULT_LOG_FORMAT)
            .build();

    public static final Option<Boolean> LOG_FILE_INCLUDE_TRACE = new OptionBuilder<>("log-file-include-trace", Boolean.class)
            .category(OptionCategory.LOGGING)
            .description(format("Include tracing information in the file log. If the '%s' option is specified, this option has no effect.", LOG_FILE_FORMAT.getKey()))
            .defaultValue(true)
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
            .description("Set the IP address and port of the Syslog server.")
            .defaultValue("localhost:514")
            .build();

    public static final Option<Level> LOG_SYSLOG_LEVEL = new OptionBuilder<>("log-syslog-level", Level.class)
            .category(OptionCategory.LOGGING)
            .defaultValue(Level.ALL)
            .description("Set the log level for the Syslog handler. It specifies the most verbose log level for logs shown in the output. "
                    + "It respects levels specified in the 'log-level' option, which represents the maximal verbosity for the whole logging system. "
                    + "For more information, check the Logging guide.")
            .build();

    public static final Option<String> LOG_SYSLOG_TYPE = new OptionBuilder<>("log-syslog-type", String.class)
            .category(OptionCategory.LOGGING)
            .expectedValues(Arrays.stream(SyslogHandler.SyslogType.values()).map(f -> f.toString().toLowerCase()).toList())
            .description("Set the Syslog type used to format the sent message.")
            .defaultValue(SyslogHandler.SyslogType.RFC5424.toString().toLowerCase())
            .build();

    public static final Option<String> LOG_SYSLOG_MAX_LENGTH = new OptionBuilder<>("log-syslog-max-length", String.class)
            .category(OptionCategory.LOGGING)
            // based on the 'quarkus.log.syslog.max-length' property
            .description("Set the maximum length, in bytes, of the message allowed to be sent. The length includes the header and the message. " +
                    "If not set, the default value is 2048 when 'log-syslog-type' is rfc5424 (default) and 1024 when 'log-syslog-type' is rfc3164.")
            .build();

    public static final Option<String> LOG_SYSLOG_APP_NAME = new OptionBuilder<>("log-syslog-app-name", String.class)
            .category(OptionCategory.LOGGING)
            .description("Set the app name used when formatting the message in RFC5424 format.")
            .defaultValue("keycloak")
            .build();

    public static final Option<String> LOG_SYSLOG_PROTOCOL = new OptionBuilder<>("log-syslog-protocol", String.class)
            .category(OptionCategory.LOGGING)
            .description("Set the protocol used to connect to the Syslog server.")
            .defaultValue("tcp")
            .expectedValues("tcp", "udp", "ssl-tcp")
            .build();

    public static final Option<String> LOG_SYSLOG_FORMAT = new OptionBuilder<>("log-syslog-format", String.class)
            .category(OptionCategory.LOGGING)
            .description("Set a format specific to Syslog entries.")
            .defaultValue(DEFAULT_LOG_FORMAT)
            .build();

    public static final Option<Boolean> LOG_SYSLOG_INCLUDE_TRACE = new OptionBuilder<>("log-syslog-include-trace", Boolean.class)
            .category(OptionCategory.LOGGING)
            .description(format("Include tracing information in the Syslog. If the '%s' option is specified, this option has no effect.", LOG_SYSLOG_FORMAT.getKey()))
            .defaultValue(true)
            .build();

    public static final Option<Output> LOG_SYSLOG_OUTPUT = new OptionBuilder<>("log-syslog-output", Output.class)
            .category(OptionCategory.LOGGING)
            .defaultValue(DEFAULT_SYSLOG_OUTPUT)
            .description("Set the Syslog output to JSON or default (plain) unstructured logging.")
            .build();

}
