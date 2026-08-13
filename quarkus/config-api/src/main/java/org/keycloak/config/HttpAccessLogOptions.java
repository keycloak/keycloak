package org.keycloak.config;

import java.util.List;

public class HttpAccessLogOptions {

    public static final Option<Boolean> HTTP_ACCESS_LOG_ENABLED = new OptionBuilder<>("http-access-log-enabled", Boolean.class)
            .category(OptionCategory.HTTP_ACCESS_LOG)
            .description("If HTTP access logging is enabled. By default this will log records in console.")
            .defaultValue(Boolean.FALSE)
            .build();

    public static final Option<String> HTTP_ACCESS_LOG_PATTERN = new OptionBuilder<>("http-access-log-pattern", String.class)
            .category(OptionCategory.HTTP_ACCESS_LOG)
            .expectedValues("common", "combined", "long")
            .strictExpectedValues(false)
            .description("The HTTP access log pattern. You can use the available named formats, or use custom format described in Quarkus documentation.")
            .defaultValue("common")
            .build();

    public static final Option<String> HTTP_ACCESS_LOG_EXCLUDE = new OptionBuilder<>("http-access-log-exclude", String.class)
            .category(OptionCategory.HTTP_ACCESS_LOG)
            .description("A regular expression that can be used to exclude some paths from logging. For instance, '/realms/my-realm/.*' will exclude all subsequent endpoints for realm 'my-realm' from the log.")
            .build();

    public static final List<String> DEFAULT_HIDDEN_HEADERS = List.of(
            "Authorization"
    );

    // check the CookieType
    public static final List<String> DEFAULT_HIDDEN_COOKIES = List.of(
            "AUTH_SESSION_ID",
            "KC_AUTH_SESSION_HASH",
            "KEYCLOAK_IDENTITY",
            "KEYCLOAK_SESSION",
            "AUTH_SESSION_ID_LEGACY",
            "KEYCLOAK_IDENTITY_LEGACY",
            "KEYCLOAK_SESSION_LEGACY"
    );

    public static final Option<List<String>> HTTP_ACCESS_LOG_MASKED_HEADERS = OptionBuilder.listOptionBuilder("http-access-log-masked-headers", String.class)
            .category(OptionCategory.HTTP_ACCESS_LOG)
            .description("Set of HTTP headers whose values must be masked when the 'long' pattern or '%{ALL_REQUEST_HEADERS}' format is enabled with the 'http-access-log-pattern' option. Selected security sensitive headers are always masked.")
            .build();

    public static final Option<List<String>> HTTP_ACCESS_LOG_MASKED_COOKIES = OptionBuilder.listOptionBuilder("http-access-log-masked-cookies", String.class)
            .category(OptionCategory.HTTP_ACCESS_LOG)
            .description("Set of HTTP Cookie headers whose values must be masked when the 'long' pattern or '%{ALL_REQUEST_HEADERS}' format is enabled with the 'http-access-log-pattern' option. Selected security sensitive cookies are always masked.")
            .build();

    // File
    public static final Option<Boolean> HTTP_ACCESS_LOG_FILE_ENABLED = new OptionBuilder<>("http-access-log-file-enabled", Boolean.class)
            .category(OptionCategory.HTTP_ACCESS_LOG)
            .description("If HTTP access logging should be done to a separate file.")
            .defaultValue(Boolean.FALSE)
            .build();

    public static final Option<String> HTTP_ACCESS_LOG_FILE_SUFFIX = new OptionBuilder<>("http-access-log-file-suffix", String.class)
            .category(OptionCategory.HTTP_ACCESS_LOG)
            .description("The HTTP access log file suffix. When rotation is enabled, a date-based suffix '.{yyyy-MM-dd}' is added before the specified suffix. If multiple rotations occur on the same day, an incremental index is appended to the date.")
            .defaultValue(".log")
            .build();

    public static final Option<String> HTTP_ACCESS_LOG_FILE_NAME = new OptionBuilder<>("http-access-log-file-name", String.class)
            .category(OptionCategory.HTTP_ACCESS_LOG)
            .description("The HTTP access log file base name, which will create a log file name concatenating base and suffix (e.g. 'keycloak-http-access.log'). The file is located in the '/data/log' directory of the distribution.")
            .defaultValue("keycloak-http-access")
            .build();

    public static final Option<Boolean> HTTP_ACCESS_LOG_FILE_ROTATE = new OptionBuilder<>("http-access-log-file-rotate", Boolean.class)
            .category(OptionCategory.HTTP_ACCESS_LOG)
            .description("If the HTTP Access log file should be rotated daily.")
            .defaultValue(true)
            .build();
}
