package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.testsuite.util.oauth.AbstractHttpGetRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;

public class CredentialOfferUriRequest extends AbstractHttpGetRequest<CredentialOfferUriRequest, CredentialOfferUriResponse> {

    private String credentialConfigurationId;
    private Boolean preAuthorized;
    private String username;
    private String clientIdParam;

    public CredentialOfferUriRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    public CredentialOfferUriRequest credentialConfigurationId(String credentialConfigurationId) {
        this.credentialConfigurationId = credentialConfigurationId;
        return this;
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
        this.clientIdParam = clientId;
        return this;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getOid4vcCredentialOfferUri(credentialConfigurationId, preAuthorized, username, clientIdParam);
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
