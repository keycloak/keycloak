package org.keycloak.testsuite.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;

public class ReverseProxy implements TestRule {

    public static String DEFAULT_PROXY_HOST = "proxy.kc.localtest.me";
    public static final int DEFAULT_HTTP_PORT = 8666;
    public static final int DEFAULT_HTTPS_PORT = 8667;

    public ReverseProxy() {
        this(DEFAULT_PROXY_HOST);
    }
    
    public ReverseProxy(String host) {
        this(host, "");
    }

    public ReverseProxy(String host, String nodes) {
        // No-op compatibility constructor kept for migrated broker tests.
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
        String scheme = AUTH_SERVER_SSL_REQUIRED ? "https" : "http";
        int port = AUTH_SERVER_SSL_REQUIRED ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT;
        return removeDefaultPorts(String.format("%s://%s:%s", scheme, DEFAULT_PROXY_HOST, port));
    }
}
