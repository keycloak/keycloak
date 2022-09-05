package org.keycloak.config;

public class ProxyOptions {

    public enum Mode {
        none,
        edge,
        reencrypt,
        passthrough;
    }

    public static final Option<Mode> PROXY = new OptionBuilder<>("proxy", Mode.class)
            .category(OptionCategory.PROXY)
            .description("The proxy address forwarding mode if the server is behind a reverse proxy.")
            .defaultValue(Mode.none)
            .build();

    public static final Option<Boolean> PROXY_FORWARDED_HOST = new OptionBuilder<>("proxy-forwarded-host", Boolean.class)
            .category(OptionCategory.PROXY)
            .defaultValue(Boolean.FALSE)
            .build();
}
