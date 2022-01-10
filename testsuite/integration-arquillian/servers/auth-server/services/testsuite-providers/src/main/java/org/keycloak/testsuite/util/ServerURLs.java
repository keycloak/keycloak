/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.util;

import static java.lang.Integer.parseInt;

public class ServerURLs {
    public static final boolean AUTH_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required", "true"));
    public static final String AUTH_SERVER_PORT = AUTH_SERVER_SSL_REQUIRED ? System.getProperty("auth.server.https.port", "8543") : System.getProperty("auth.server.http.port", "8180");
    public static final String AUTH_SERVER_SCHEME = AUTH_SERVER_SSL_REQUIRED ? "https" : "http";
    public static final String AUTH_SERVER_HOST = System.getProperty("auth.server.host", "localhost");
    public static final String AUTH_SERVER_HOST2 = System.getProperty("auth.server.host2", AUTH_SERVER_HOST);

    public static final boolean APP_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("app.server.ssl.required", "false"));
    public static final String APP_SERVER_PORT = APP_SERVER_SSL_REQUIRED ? System.getProperty("app.server.https.port", "8643") : System.getProperty("app.server.http.port", "8280");
    public static final String APP_SERVER_SCHEME = APP_SERVER_SSL_REQUIRED ? "https" : "http";
    public static final String APP_SERVER_HOST = System.getProperty("app.server.host", "localhost");

    public static String getAuthServerContextRoot() {
        return getAuthServerContextRoot(0);
    }

    public static String getAuthServerContextRoot(int clusterPortOffset) {
        return removeDefaultPorts(String.format("%s://%s:%s", AUTH_SERVER_SCHEME, AUTH_SERVER_HOST, parseInt(AUTH_SERVER_PORT) + clusterPortOffset));
    }

    public static String getAppServerContextRoot() {
        return getAppServerContextRoot(0);
    }

    public static String getAppServerContextRoot(int clusterPortOffset) {
        return removeDefaultPorts(String.format("%s://%s:%s", APP_SERVER_SCHEME, APP_SERVER_HOST, parseInt(APP_SERVER_PORT) + clusterPortOffset));
    }

    /**
     * Removes default ports: 80 and 443 from url
     */
    public static String removeDefaultPorts(String url) {
        return url != null ? url.replaceFirst("(.*)(:80)(\\/.*)?$", "$1$3").replaceFirst("(.*)(:443)(\\/.*)?$", "$1$3") : null;
    }

    private static int parsePort(String property) {
        try {
            return parseInt(System.getProperty(property));
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Failed to get " + property, ex);
        }
    }
}
