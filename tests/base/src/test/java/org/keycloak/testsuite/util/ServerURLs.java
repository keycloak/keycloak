package org.keycloak.testsuite.util;

import static java.lang.Integer.parseInt;

public final class ServerURLs {

    public static final boolean AUTH_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required", "true"));
    public static final String AUTH_SERVER_PORT_HTTP = System.getProperty("auth.server.http.port", "8180");
    public static final String AUTH_SERVER_PORT_HTTPS = System.getProperty("auth.server.https.port", "8543");
    public static final String AUTH_SERVER_PORT = AUTH_SERVER_SSL_REQUIRED ? AUTH_SERVER_PORT_HTTPS : AUTH_SERVER_PORT_HTTP;
    public static final String AUTH_SERVER_SCHEME = AUTH_SERVER_SSL_REQUIRED ? "https" : "http";
    public static final String AUTH_SERVER_HOST = System.getProperty("auth.server.host", "localhost");

    private ServerURLs() {
    }

    public static String getAuthServerContextRoot() {
        return getAuthServerContextRoot(0);
    }

    public static String getAuthServerContextRoot(int clusterPortOffset) {
        return removeDefaultPorts(String.format("%s://%s:%s", AUTH_SERVER_SCHEME, AUTH_SERVER_HOST, parseInt(AUTH_SERVER_PORT) + clusterPortOffset));
    }

    public static String removeDefaultPorts(String url) {
        return url != null ? url.replaceFirst("(.*)(:80)(\\/.*)?$", "$1$3").replaceFirst("(.*)(:443)(\\/.*)?$", "$1$3") : null;
    }
}
