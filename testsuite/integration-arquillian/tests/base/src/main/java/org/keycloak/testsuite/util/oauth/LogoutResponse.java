package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

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
