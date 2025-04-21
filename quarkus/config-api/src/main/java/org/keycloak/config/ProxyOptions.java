package org.keycloak.config;

import java.util.List;

public class ProxyOptions {

    public enum Headers {
        forwarded,
        xforwarded
    }

    public static final Option<Headers> PROXY_HEADERS = new OptionBuilder<>("proxy-headers", Headers.class)
            .category(OptionCategory.PROXY)
            .description("The proxy headers that should be accepted by the server. Misconfiguration might leave the server exposed to security vulnerabilities. Takes precedence over the deprecated proxy option.")
            .build();

    public static final Option<Boolean> PROXY_PROTOCOL_ENABLED = new OptionBuilder<>("proxy-protocol-enabled", Boolean.class)
            .category(OptionCategory.PROXY)
            .description("Whether the server should use the HA PROXY protocol when serving requests from behind a proxy. When set to true, the remote address returned will be the one from the actual connecting client. Cannot be enabled when the `proxy-headers` is used.")
            .defaultValue(Boolean.FALSE)
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

    public static final Option<Boolean> PROXY_TRUSTED_HEADER_ENABLED = new OptionBuilder<>("proxy-trusted-header-enabled", Boolean.class)
            .category(OptionCategory.PROXY)
            .defaultValue(Boolean.FALSE)
            .hidden()
            .build();

    public static final Option<List<String>> PROXY_TRUSTED_ADDRESSES = OptionBuilder.listOptionBuilder("proxy-trusted-addresses", String.class)
            .category(OptionCategory.PROXY)
            .description("A comma separated list of trusted proxy addresses. If set, then proxy headers from other addresses will be ignored. By default all addresses are trusted. A trusted proxy address is specified as an IP address (IPv4 or IPv6) or Classless Inter-Domain Routing (CIDR) notation.")
            .build();
}
