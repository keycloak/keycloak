package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;
import java.util.Optional;

import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;

import org.apache.http.client.methods.CloseableHttpResponse;

public class CredentialOfferUriResponse extends AbstractHttpResponse {

    private CredentialOfferURI credentialOfferURI;

    public CredentialOfferUriResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        credentialOfferURI = asJson(CredentialOfferURI.class);
    }

    public CredentialOfferURI getCredentialOfferURI() {
        return Optional.ofNullable(credentialOfferURI).orElseThrow(() ->
                new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription())));
    }
}
