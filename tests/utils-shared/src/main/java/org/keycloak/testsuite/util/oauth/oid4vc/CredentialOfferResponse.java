package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;

import org.apache.http.client.methods.CloseableHttpResponse;

public class CredentialOfferResponse extends AbstractHttpResponse {

    private CredentialsOffer credentialsOffer;

    public CredentialOfferResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        credentialsOffer = asJson(CredentialsOffer.class);
    }

    public CredentialsOffer getCredentialsOffer() {
        return credentialsOffer;
    }
}
