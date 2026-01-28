package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;

public class LogoutResponse extends AbstractHttpResponse {

    public LogoutResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() {
    }

    @Override
    protected int getSuccessCode() {
        return 204;
    }

}
