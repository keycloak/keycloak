package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

public class BackchannelLogoutResponse extends AbstractHttpResponse {

    public BackchannelLogoutResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() {
    }

}
