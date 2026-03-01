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

    /**
     * The base string representation of the URL of the Keycloak server (for example <code>http://localhost:8080</code>)
     *
     * @return the server base URL as a string
     */
    public String getBase() {
        return baseUrl;
    }


    /**
     * The URL of the Keycloak server (for example <code>http://localhost:8080</code>)
     *
     * @return the server base URL
     */
    public URL getBaseUrl() {
        return toUrl(getBase());
    }

    /**
     * The string representation of the base URL of the master realm
     *
     * @return
     */
    public String getMasterRealm() {
        return baseUrl + "/realms/master";
    }

    /**
     * The URL of the master realm
     *
     * @return master realm URL
     */
    public URL getMasterRealmUrl() {
        return toUrl(getMasterRealm());
    }

    /**
     * The string representation of the URL of Admin endpoints
     *
     * @return admin URL as a string
     */
    public String getAdmin() {
        return baseUrl + "/admin";
    }

    /**
     * The URL of Admin endpoints
     *
     * @return admin URL
     */
    public URL getAdminUrl() {
        return toUrl(getAdmin());
    }

    /**
     * Builder to resolve paths from the Keycloak server base URL
     *
     * @return base URL builder
     */
    public KeycloakUriBuilder getBaseBuilder() {
        return toBuilder(getBase());
    }

    /**
     * Builder to resolve paths from the admin URL
     *
     * @return admin URL builder
     */
    public KeycloakUriBuilder getAdminBuilder() {
        return toBuilder(getAdmin());
    }

    /**
     * String representation of the URL of the metrics endpoint
     *
     * @return metrics endpoint
     */
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
