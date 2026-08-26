package org.keycloak.testsuite.util;

import java.net.URI;

import org.keycloak.testframework.oauth.OAuthClient;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;

public class ReverseProxy implements TestRule {

    public static String DEFAULT_PROXY_HOST = "proxy.kc.localtest.me";
    public static final int DEFAULT_HTTP_PORT = 8666;
    public static final int DEFAULT_HTTPS_PORT = 8667;
    private final String url;

    public ReverseProxy() {
        this(DEFAULT_PROXY_HOST);
    }
    
    public ReverseProxy(String host) {
        this(host, "");
    }

    public ReverseProxy(String host, String nodes) {
        String effectiveHost = host == null || host.isBlank() ? DEFAULT_PROXY_HOST : host;
        this.url = resolveProxyUrl(effectiveHost);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
            }
        };
    }

    public String getUrl() {
        return url;
    }

    private static String resolveProxyUrl(String host) {
        String root = OAuthClient.SERVER_ROOT;
        if (root == null || root.isBlank()) {
            root = getAuthServerContextRoot();
        }

        URI rootUri = URI.create(root);
        String scheme = rootUri.getScheme() != null ? rootUri.getScheme() : (AUTH_SERVER_SSL_REQUIRED ? "https" : "http");
        int defaultPort = "https".equalsIgnoreCase(scheme) ? 443 : 80;
        int port = rootUri.getPort() == -1 ? defaultPort : rootUri.getPort();

        String path = rootUri.getPath();
        String normalizedPath = (path == null || path.isBlank() || "/".equals(path))
                ? ""
                : (path.endsWith("/") ? path.substring(0, path.length() - 1) : path);

        return removeDefaultPorts(String.format("%s://%s:%s%s", scheme, host, port, normalizedPath));
    }
}
