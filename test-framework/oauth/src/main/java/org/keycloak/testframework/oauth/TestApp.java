package org.keycloak.testframework.oauth;

import com.sun.net.httpserver.HttpServer;

/**
 * Mock OAuth client exposed on an HTTP server so Keycloak can send callbacks to the client
 */
public class TestApp {

    public static final String OAUTH_CALLBACK_PATH = "/callback/oauth";
    public static final String K_ADMIN_PATH = "/k_admin";
    public static final String FRONTCHANNEL_LOGOUT_PATH = "/frontchannel-logout";

    private final HttpServer httpServer;

    private final KcAdminInvocations kcAdminInvocations;

    private final String redirectionUri;
    private final String adminUri;
    private final String frontChannelLogoutUri;

    public TestApp(HttpServer httpServer) {
        this.httpServer = httpServer;
        this.kcAdminInvocations = new KcAdminInvocations();

        try {
            httpServer.createContext(OAUTH_CALLBACK_PATH, new OAuthCallbackHandler());
            httpServer.createContext(K_ADMIN_PATH, new KcAdminCallbackHandler(kcAdminInvocations));
            httpServer.createContext(FRONTCHANNEL_LOGOUT_PATH, new FrontChannelLogoutHandler(kcAdminInvocations));

            String base = "http://127.0.0.1:" + httpServer.getAddress().getPort();
            redirectionUri = base + OAUTH_CALLBACK_PATH;
            adminUri = base + K_ADMIN_PATH;
            frontChannelLogoutUri = base + FRONTCHANNEL_LOGOUT_PATH;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public String getRedirectionUri() {
        return redirectionUri;
    }

    public String getAdminUri() {
        return adminUri;
    }

    public String getFrontChannelLogoutUri() {
        return frontChannelLogoutUri;
    }

    public KcAdminInvocations kcAdmin() {
        return kcAdminInvocations;
    }

    public void close() {
        httpServer.removeContext(OAUTH_CALLBACK_PATH);
        httpServer.removeContext(K_ADMIN_PATH);
        httpServer.removeContext(FRONTCHANNEL_LOGOUT_PATH);
    }

}
