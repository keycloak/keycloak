package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;

/**
 *
 * @author rmartinc
 */
public class UnregisterNodeResponse extends AbstractHttpResponse {

    public UnregisterNodeResponse(CloseableHttpResponse response) throws IOException {
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
