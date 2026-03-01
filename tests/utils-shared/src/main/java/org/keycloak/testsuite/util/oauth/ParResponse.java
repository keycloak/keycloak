package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.CloseableHttpResponse;

public class ParResponse extends AbstractHttpResponse {

    private String requestUri;
    private int expiresIn;

    public ParResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        ObjectNode json = asJson();
        requestUri = json.get("request_uri").asText();
        expiresIn = json.get("expires_in").asInt();
    }

    @Override
    protected int getSuccessCode() {
        return 201;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

}
