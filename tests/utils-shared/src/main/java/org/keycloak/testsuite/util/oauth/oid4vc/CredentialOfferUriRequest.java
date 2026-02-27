package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.protocol.oid4vc.model.OfferResponseType;
import org.keycloak.testsuite.util.oauth.AbstractHttpGetRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.util.Strings;

import org.apache.http.client.methods.CloseableHttpResponse;

public class CredentialOfferUriRequest extends AbstractHttpGetRequest<CredentialOfferUriRequest, CredentialOfferUriResponse> {

    private final String credConfigId;
    private Boolean preAuthorized;
    private Boolean txCode;
    private String targetUser;
    private Integer expireAt;
    private OfferResponseType responseType;

    CredentialOfferUriRequest(AbstractOAuthClient<?> client, String credConfigId) {
        super(client);
        this.credConfigId = credConfigId;
    }

    public CredentialOfferUriRequest preAuthorized(Boolean preAuthorized) {
        this.preAuthorized = preAuthorized;
        return this;
    }

    public CredentialOfferUriRequest txCode(Boolean txCode) {
        this.txCode = txCode;
        return this;
    }

    public CredentialOfferUriRequest targetUser(String targetUser) {
        this.targetUser = targetUser;
        return this;
    }

    public CredentialOfferUriRequest expireAt(Integer expireAt) {
        this.expireAt = expireAt;
        return this;
    }

    public CredentialOfferUriRequest responseType(OfferResponseType responseType) {
        this.responseType = responseType;
        return this;
    }

    @Override
    protected String getEndpoint() {
        UriBuilder builder = UriBuilder.fromUri(client.getEndpoints().getOid4vcCredentialOfferUri());
        if (!Strings.isEmpty(credConfigId)) builder.queryParam("credential_configuration_id", credConfigId);
        if (preAuthorized != null) builder.queryParam("pre_authorized", preAuthorized);
        if (txCode != null) builder.queryParam("tx_code", txCode);
        if (!Strings.isEmpty(targetUser)) builder.queryParam("target_user", targetUser);
        if (expireAt != null) builder.queryParam("expire", expireAt);
        if (responseType != null) builder.queryParam("type", responseType.getValue());
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
