package org.keycloak.admin.client;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * Provides a {@link Keycloak} client builder with the ability to customize the underlying
 * {@link ResteasyClient RESTEasy client} used to communicate with the Keycloak server.
 *
 * <p>Example usage with a connection pool size of 20:</p>
 *
 * <pre>
 *   Keycloak keycloak = KeycloakBuilder.builder()
 *     .serverUrl("https:/sso.example.com/auth")
 *     .realm("realm")
 *     .username("user")
 *     .password("pass")
 *     .clientId("client")
 *     .clientSecret("secret")
 *     .resteasyClient(new ResteasyClientBuilder().connectionPoolSize(20).build())
 *     .build();
 * </pre>
 *
 * @author Scott Rossillo
 * @see ResteasyClientBuilder
 */
public class KeycloakBuilder {
    private String serverUrl;
    private String realm;
    private String username;
    private String password;
    private String clientId;
    private String clientSecret;
    private ResteasyClient resteasyClient;

    public KeycloakBuilder serverUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        return this;
    }

    public KeycloakBuilder realm(String realm) {
        this.realm = realm;
        return this;
    }

    public KeycloakBuilder username(String username) {
        this.username = username;
        return this;
    }

    public KeycloakBuilder password(String password) {
        this.password = password;
        return this;
    }

    public KeycloakBuilder clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public KeycloakBuilder clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public KeycloakBuilder resteasyClient(ResteasyClient resteasyClient) {
        this.resteasyClient = resteasyClient;
        return this;
    }

    /**
     * Builds a new Keycloak client from this builder.
     */
    public Keycloak build() {
        if (serverUrl == null) {
            throw new IllegalStateException("serverUrl required");
        }

        if (realm == null) {
            throw new IllegalStateException("realm required");
        }

        if (username == null) {
            throw new IllegalStateException("username required");
        }

        if (password == null) {
            throw new IllegalStateException("password required");
        }

        if (clientId == null) {
            throw new IllegalStateException("clientId required");
        }

        return new Keycloak(serverUrl, realm, username, password, clientId, clientSecret, resteasyClient);
    }

    private KeycloakBuilder() {
    }

    /**
     * Returns a new Keycloak builder.
     */
    public static KeycloakBuilder builder() {
        return new KeycloakBuilder();
    }
}
