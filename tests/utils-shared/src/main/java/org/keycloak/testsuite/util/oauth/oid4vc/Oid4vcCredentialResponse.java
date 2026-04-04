package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;
import java.util.Optional;

import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;

import org.apache.http.client.methods.CloseableHttpResponse;

public class Oid4vcCredentialResponse extends AbstractHttpResponse {

    private CredentialResponse credentialResponse;

    public Oid4vcCredentialResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        credentialResponse = asJson(CredentialResponse.class);
    }

    public CredentialResponse getCredentialResponse() {
        return Optional.ofNullable(credentialResponse).orElseThrow(() ->
                new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription())));
    }
}
