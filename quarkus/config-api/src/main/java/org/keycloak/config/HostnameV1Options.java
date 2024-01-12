package org.keycloak.config;

public class HostnameV1Options {

    public static final Option<String> HOSTNAME = new OptionBuilder<>("hostname", String.class)
            .category(OptionCategory.HOSTNAME_V1)
            .description("Hostname for the Keycloak server.")
            .build();

    public static final Option<String> HOSTNAME_URL = new OptionBuilder<>("hostname-url", String.class)
            .category(OptionCategory.HOSTNAME_V1)
            .description("Set the base URL for frontend URLs, including scheme, host, port and path.")
            .build();

    public static final Option<String> HOSTNAME_ADMIN = new OptionBuilder<>("hostname-admin", String.class)
            .category(OptionCategory.HOSTNAME_V1)
            .description("The hostname for accessing the administration console. Use this option if you are exposing the administration console using a hostname other than the value set to the 'hostname' option.")
            .build();

    public static final Option<String> HOSTNAME_ADMIN_URL = new OptionBuilder<>("hostname-admin-url", String.class)
            .category(OptionCategory.HOSTNAME_V1)
            .description("Set the base URL for accessing the administration console, including scheme, host, port and path")
            .build();

    public static final Option<Boolean> HOSTNAME_STRICT = new OptionBuilder<>("hostname-strict", Boolean.class)
            .category(OptionCategory.HOSTNAME_V1)
            .description("Disables dynamically resolving the hostname from request headers. Should always be set to true in production, unless proxy verifies the Host header.")
            .defaultValue(Boolean.TRUE)
            .build();

    public static final Option<Boolean> HOSTNAME_STRICT_HTTPS = new OptionBuilder<>("hostname-strict-https", Boolean.class)
            .category(OptionCategory.HOSTNAME_V1)
            .description("Forces frontend URLs to use the 'https' scheme. If set to false, the HTTP scheme is inferred from requests.")
            .hidden()
            .defaultValue(Boolean.TRUE)
            .build();

    public static final Option<Boolean> HOSTNAME_STRICT_BACKCHANNEL = new OptionBuilder<>("hostname-strict-backchannel", Boolean.class)
            .category(OptionCategory.HOSTNAME_V1)
            .description("By default backchannel URLs are dynamically resolved from request headers to allow internal and external applications. If all applications use the public URL this option should be enabled.")
            .build();

    public static final Option<String> HOSTNAME_PATH = new OptionBuilder<>("hostname-path", String.class)
            .category(OptionCategory.HOSTNAME_V1)
            .description("This should be set if proxy uses a different context-path for Keycloak.")
            .build();

    public static final Option<Integer> HOSTNAME_PORT = new OptionBuilder<>("hostname-port", Integer.class)
            .category(OptionCategory.HOSTNAME_V1)
            .description("The port used by the proxy when exposing the hostname. Set this option if the proxy uses a port other than the default HTTP and HTTPS ports.")
            .defaultValue(-1)
            .build();

    public static final Option<Boolean> HOSTNAME_DEBUG = new OptionBuilder<>("hostname-debug", Boolean.class)
            .category(OptionCategory.HOSTNAME_V1)
            .description("Toggle the hostname debug page that is accessible at /realms/master/hostname-debug")
            .defaultValue(Boolean.FALSE)
            .build();

}
