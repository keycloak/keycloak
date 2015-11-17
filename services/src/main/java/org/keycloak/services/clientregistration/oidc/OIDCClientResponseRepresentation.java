package org.keycloak.services.clientregistration.oidc;

import org.codehaus.jackson.annotate.JsonProperty;
import org.keycloak.protocol.oidc.representations.OIDCClientRepresentation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientResponseRepresentation extends OIDCClientRepresentation {

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;

    @JsonProperty("client_id_issued_at")
    private int clientIdIssuedAt;

    @JsonProperty("client_secret_expires_at")
    private int clientSecretExpiresAt;

    @JsonProperty("registration_client_uri")
    private String registrationClientUri;

    @JsonProperty("registration_access_token")
    private String registrationAccessToken;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public int getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    public void setClientIdIssuedAt(int clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    public int getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    public void setClientSecretExpiresAt(int clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
    }

    public String getRegistrationClientUri() {
        return registrationClientUri;
    }

    public void setRegistrationClientUri(String registrationClientUri) {
        this.registrationClientUri = registrationClientUri;
    }

    public String getRegistrationAccessToken() {
        return registrationAccessToken;
    }

    public void setRegistrationAccessToken(String registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
    }

}
