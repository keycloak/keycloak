package org.keycloak.config;

public class ProxyOptions {

    public enum Mode {
        none(false),
        edge,
        reencrypt,
        passthrough(false);

        private final boolean proxyHeadersEnabled;

        Mode(boolean proxyHeadersEnabled) {
            this.proxyHeadersEnabled = proxyHeadersEnabled;
        }

        Mode() {
            this(true);
        }

        public boolean isProxyHeadersEnabled() {
            return proxyHeadersEnabled;
        }
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

    public static final Option<Boolean> PROXY_FORWARDED_HEADER_ENABLED = new OptionBuilder<>("proxy-allow-forwarded-header", Boolean.class)
            .category(OptionCategory.PROXY)
            .defaultValue(Boolean.FALSE)
            .build();

    public static final Option<Boolean> PROXY_X_FORWARDED_HEADER_ENABLED = new OptionBuilder<>("proxy-allow-x-forwarded-header", Boolean.class)
            .category(OptionCategory.PROXY)
            .defaultValue(Boolean.FALSE)
            .build();
}
