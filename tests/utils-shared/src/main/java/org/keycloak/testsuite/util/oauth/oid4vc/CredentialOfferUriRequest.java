package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.testsuite.util.oauth.AbstractHttpGetRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;

public class CredentialOfferUriRequest extends AbstractHttpGetRequest<CredentialOfferUriRequest, CredentialOfferUriResponse> {

    private final String credConfigId;
    private Boolean preAuthorized;
    private String username;
    private String clientId;

    public CredentialOfferUriRequest(AbstractOAuthClient<?> client, String credConfigId) {
        super(client);
        this.credConfigId = credConfigId;
    }

    public CredentialOfferUriRequest preAuthorized(Boolean preAuthorized) {
        this.preAuthorized = preAuthorized;
        return this;
    }

    public CredentialOfferUriRequest username(String username) {
        this.username = username;
        return this;
    }

    public CredentialOfferUriRequest clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    @Override
    protected String getEndpoint() {
        UriBuilder builder = UriBuilder.fromUri(client.getEndpoints().getOid4vcCredentialOfferUri());
        if (credConfigId != null && !credConfigId.isBlank()) builder.queryParam("credential_configuration_id", credConfigId);
        if (preAuthorized != null) builder.queryParam("pre_authorized", preAuthorized);
        if (clientId != null && !clientId.isBlank()) builder.queryParam("client_id", clientId);
        if (username != null && !username.isBlank()) builder.queryParam("username", username);
        return builder.build().toString();
    }

    @Override
    protected void initRequest() {
        // All parameters are in the URL for this specific Keycloak test endpoint
    }

    @Override
    protected CredentialOfferUriResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new CredentialOfferUriResponse(response);
    }
}
