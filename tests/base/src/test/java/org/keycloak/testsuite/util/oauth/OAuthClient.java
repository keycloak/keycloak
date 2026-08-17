package org.keycloak.testsuite.util.oauth;

import org.keycloak.testsuite.util.ServerURLs;

public final class OAuthClient {

    public static final String AUTH_SERVER_ROOT = ServerURLs.getAuthServerContextRoot() + "/auth";
    public static String APP_ROOT;
    public static String APP_AUTH_ROOT;

    static {
        updateAppRootRealm("master");
    }

    private OAuthClient() {
    }

    public static void updateAppRootRealm(String realm) {
        APP_ROOT = AUTH_SERVER_ROOT + "/realms/" + realm + "/app";
        APP_AUTH_ROOT = APP_ROOT + "/auth";
    }
}
