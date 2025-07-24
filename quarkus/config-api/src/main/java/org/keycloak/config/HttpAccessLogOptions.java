package org.keycloak.config;

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
}
