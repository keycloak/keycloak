package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;

/**
 *
 * @author rmartinc
 */
public class PlainStringResponse extends AbstractHttpResponse {

    private String response;

    public PlainStringResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        response = asString();
    }

    @Override
    protected void parseError() throws IOException {
        response = asString();
    }

    public String getResponse() {
        return response;
    }

}
