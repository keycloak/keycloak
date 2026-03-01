package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;
import java.util.Optional;

import org.keycloak.protocol.oid4vc.model.NonceResponse;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;

import org.apache.http.client.methods.CloseableHttpResponse;

public class Oid4vcNonceResponse extends AbstractHttpResponse {

    private NonceResponse nonceResponse;

    public Oid4vcNonceResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        nonceResponse = asJson(NonceResponse.class);
    }

    public String getNonce() {
        return Optional.ofNullable(nonceResponse).map(NonceResponse::getNonce).orElseThrow(() ->
                new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription())));
    }
}
