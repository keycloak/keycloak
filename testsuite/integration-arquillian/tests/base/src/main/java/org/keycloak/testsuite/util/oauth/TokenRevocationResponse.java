package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

public class TokenRevocationResponse extends AbstractHttpResponse {

    public TokenRevocationResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() {
    }

}
