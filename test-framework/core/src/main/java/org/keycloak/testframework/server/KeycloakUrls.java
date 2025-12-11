package org.keycloak.testframework.server;

import java.net.MalformedURLException;
import java.net.URL;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

public class KeycloakUrls {

    private final String baseUrl;
    private final String managementBaseUrl;

    public KeycloakUrls(String baseUrl, String managementBaseUrl) {
        this.baseUrl = baseUrl;
        this.managementBaseUrl = managementBaseUrl;
    }

    public String getBase() {
        return baseUrl;
    }

    public URL getBaseUrl() {
        return toUrl(getBase());
    }

    public String getMasterRealm() {
        return baseUrl + "/realms/master";
    }

    public URL getMasterRealmUrl() {
        return toUrl(getMasterRealm());
    }

    public String getAdmin() {
        return baseUrl + "/admin";
    }

    public URL getAdminUrl() {
        return toUrl(getAdmin());
    }

    public KeycloakUriBuilder getBaseBuilder() {
        return toBuilder(getBase());
    }

    public KeycloakUriBuilder getAdminBuilder() {
        return toBuilder(getAdmin());
    }

    public String getMetric() {
        return managementBaseUrl + "/metrics";
    }

    private URL toUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private KeycloakUriBuilder toBuilder(String url) {
        return KeycloakUriBuilder.fromUri(url);
    }

    public String getToken(String realm) {
        return baseUrl + "/realms/" + realm + "/protocol/" + OIDCLoginProtocol.LOGIN_PROTOCOL + "/token";
    }
}
