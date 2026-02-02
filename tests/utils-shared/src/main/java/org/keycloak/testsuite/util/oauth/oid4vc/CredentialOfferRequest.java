package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.testsuite.util.oauth.AbstractHttpGetRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;

public class CredentialOfferRequest extends AbstractHttpGetRequest<CredentialOfferRequest, CredentialOfferResponse> {

    private String endpoint;
    private String nonce;

    public CredentialOfferRequest(AbstractOAuthClient<?> client, CredentialOfferURI credOfferURI) {
        super(client);
        this.endpoint = credOfferURI.getIssuer();
        this.nonce = credOfferURI.getNonce();
    }

    public CredentialOfferRequest(AbstractOAuthClient<?> client, String credOfferUrl) {
        super(client);
        this.endpointOverride = credOfferUrl;
    }

    public CredentialOfferRequest nonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    @Override
    protected String getEndpoint() {
        String endpointUrl;
        if (endpointOverride != null) {
            endpointUrl = endpointOverride;
        } else {
            if (endpoint == null) {
                endpoint = client.getEndpoints().getOid4vcCredentialOffer();
            }
            if (nonce == null) {
                throw new IllegalStateException("Nonce must be provided");
            }
            endpointUrl = endpoint + "/" + nonce;
        }
        return endpointUrl;
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
