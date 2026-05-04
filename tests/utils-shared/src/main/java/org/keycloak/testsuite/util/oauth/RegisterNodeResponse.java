package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;

/**
 *
 * @author rmartinc
 */
public class RegisterNodeResponse extends AbstractHttpResponse {

    public RegisterNodeResponse(CloseableHttpResponse response) throws IOException {
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
