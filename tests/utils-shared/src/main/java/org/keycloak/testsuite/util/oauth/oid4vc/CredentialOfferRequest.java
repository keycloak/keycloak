package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;
import java.util.Optional;

import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.testsuite.util.oauth.AbstractHttpGetRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;

public class CredentialOfferRequest extends AbstractHttpGetRequest<CredentialOfferRequest, CredentialOfferResponse> {

    private CredentialOfferURI credOfferURI;

    public CredentialOfferRequest(AbstractOAuthClient<?> client, CredentialOfferURI credOfferURI) {
        super(client);
        this.credOfferURI = credOfferURI;
    }

    public CredentialOfferRequest(AbstractOAuthClient<?> client, String credOfferUrl) {
        super(client);
        this.endpointOverride = credOfferUrl;
    }

    @Override
    protected String getEndpoint() {
        return Optional.ofNullable(credOfferURI)
                .map(CredentialOfferURI::getCredentialOfferUrl)
                .orElseThrow(() -> new IllegalStateException("No credOfferURI"));
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
