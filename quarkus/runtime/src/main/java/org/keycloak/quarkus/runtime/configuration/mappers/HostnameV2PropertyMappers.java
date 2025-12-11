package org.keycloak.quarkus.runtime.configuration.mappers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.config.HostnameV2Options;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.ProxyOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.utils.SecureContextResolver;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class HostnameV2PropertyMappers implements PropertyMapperGrouping {

    private static final String CONTEXT_WARNING = "the server is running in an insecure context. Secure contexts are required for full functionality, including cross-origin cookies.";
    private static final List<String> REMOVED_OPTIONS = Arrays.asList("hostname-admin-url", "hostname-path", "hostname-port", "hostname-strict-backchannel", "hostname-url", "proxy", "hostname-strict-https");

    @Override
    public List<? extends PropertyMapper<?>> getPropertyMappers() {
        return Stream.of(
                fromOption(HostnameV2Options.HOSTNAME)
                        .to("kc.spi-hostname--v2--hostname")
                        .paramLabel("hostname|URL"),
                fromOption(HostnameV2Options.HOSTNAME_ADMIN)
                        .to("kc.spi-hostname--v2--hostname-admin")
                        .paramLabel("URL"),
                fromOption(HostnameV2Options.HOSTNAME_BACKCHANNEL_DYNAMIC)
                        .to("kc.spi-hostname--v2--hostname-backchannel-dynamic"),
                fromOption(HostnameV2Options.HOSTNAME_STRICT)
                        .to("kc.spi-hostname--v2--hostname-strict"),
                fromOption(HostnameV2Options.HOSTNAME_DEBUG)
        )
        .map(b -> b.isEnabled(() -> Profile.isFeatureEnabled(Profile.Feature.HOSTNAME_V2), "hostname:v2 feature is enabled").build())
        .toList();
    }

    @Override
    public void validateConfig(Picocli picocli) {
        if (picocli.getParsedCommand().filter(AbstractCommand::isServing).isPresent()) {
            validateConfig(picocli::warn);
        }
    }

    public static void validateConfig(Consumer<String> warn) {
        List<String> inUse = REMOVED_OPTIONS.stream().filter(s -> Configuration.getOptionalKcValue(s).isPresent()).toList();

        if (!inUse.isEmpty()) {
            warn.accept("Hostname v1 options %s are still in use, please review your configuration".formatted(inUse));
        }
        boolean isProd = Environment.PROD_PROFILE_VALUE.equals(org.keycloak.common.util.Environment.getProfile());
        boolean httpsEnabled = HttpPropertyMappers.isHttpsEnabled();
        String host = Configuration.getConfigValue(HostnameV2Options.HOSTNAME).getValue();
        String proxyHeaders = Configuration.getConfigValue(ProxyOptions.PROXY_HEADERS).getValue();
        if (host != null && validateFullHostname(httpsEnabled, isProd, host, proxyHeaders, warn)) {
            return;
        }
        if (httpsEnabled) {
            return; // must be re-encrypt or passthrough. if passthrough, proxy headers must not be set
        }
        // should be edge
        if (proxyHeaders != null) {
            return;
        }
        if (isProd) {
            String warning = CONTEXT_WARNING + " Also if you are using a proxy, requests from the proxy to the server will fail CORS checks with 403s because the wrong origin will be determined. Make sure `proxy-headers` are configured properly.";
            if (host == null) {
                warn.accept("With HTTPS not enabled, `proxy-headers` unset, and `hostname-strict=false`, " + warning);
            } else if (!SecureContextResolver.isLocal(host)) {
                warn.accept("Likely misconfiguration detected. With HTTPS not enabled, `proxy-headers` unset, and a non-URL `hostname`, " + warning);
            } // else warn on prod
        }
    }

    static boolean validateFullHostname(boolean httpsEnabled, boolean isProd, String host, String proxyHeaders, Consumer<String> warn) {
        try {
            URL url = new URL(host);

            if (!url.getProtocol().toUpperCase().equals("HTTPS")) {
                if (isProd) {
                    if (!SecureContextResolver.isLocal(url.getHost())) {
                        warn.accept("Likely misconfiguration detected. `hostname` is configured to use HTTP instead of HTTPS, " + CONTEXT_WARNING);

                        // TODO: any hostname-admin specific validation?

                    } // else warn on prod?
                    if (httpsEnabled) {
                        warn.accept("Likely misconfiguration detected. HTTPS is enabled on the server, but `hostname` specifies HTTP.");
                    }
                }
            } else if (proxyHeaders == null) {
                if (!httpsEnabled) {
                    // edge case
                    warn.accept("Likely misconfiguration detected. When using an edge proxy, you must use `proxy-headers`.");
                }
                // else might be allowable if HOST is overwritten
            }

            if (proxyHeaders == null && !url.getPath().isEmpty() && !normalizePath(url.getPath()).equals(
                            normalizePath(Configuration.getConfigValue(HttpOptions.HTTP_RELATIVE_PATH).getValue()))) {
                warn.accept("Likely misconfiguration detected. When using a `hostname` that includes a path that does not match the `http-relative-path` you must use `proxy-headers`");
            }

            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return path;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

}
