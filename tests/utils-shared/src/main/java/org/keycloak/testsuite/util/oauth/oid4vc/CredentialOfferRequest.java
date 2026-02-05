package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.testsuite.util.oauth.AbstractHttpGetRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;

public class CredentialOfferRequest extends AbstractHttpGetRequest<CredentialOfferRequest, CredentialOfferResponse> {

    private String nonce;

    public CredentialOfferRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    public CredentialOfferRequest(String nonce, AbstractOAuthClient<?> client) {
        super(client);
        this.nonce = nonce;
    }

    public CredentialOfferRequest nonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    @Override
    protected String getEndpoint() {
        if (nonce == null) {
            throw new IllegalStateException("Nonce must be provided either via constructor, nonce() method, or endpoint must be overridden");
        }
        return client.getEndpoints().getOid4vcCredentialOffer(nonce);
    }

    @Override
    protected void initRequest() {
        // No additional step needed for basic GET
    }

    @Override
    protected CredentialOfferResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new CredentialOfferResponse(response);
    }
}
