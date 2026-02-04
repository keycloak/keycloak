package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;
import java.util.Optional;

import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.testsuite.util.oauth.AbstractHttpGetRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;

public class CredentialOfferRequest extends AbstractHttpGetRequest<CredentialOfferRequest, CredentialOfferResponse> {

    private final CredentialOfferURI credOfferURI;

    CredentialOfferRequest(AbstractOAuthClient<?> client, CredentialOfferURI credOfferUri) {
        super(client);
        this.credOfferURI = credOfferUri;
    }

    CredentialOfferRequest(AbstractOAuthClient<?> client, String nonce) {
        super(client);
        credOfferURI = new CredentialOfferURI();
        credOfferURI.setIssuer(client.getEndpoints().getOid4vcCredentialOffer());
        credOfferURI.setNonce(nonce);
    }

    @Override
    protected String getEndpoint() {
        return Optional.ofNullable(credOfferURI)
                .map(CredentialOfferURI::getCredentialOfferUri)
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
