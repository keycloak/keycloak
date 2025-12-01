package org.keycloak.testsuite.util.oauth.ciba;

import java.io.IOException;

import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.CloseableHttpResponse;

public class AuthenticationRequestAcknowledgement extends AbstractHttpResponse {

    private String authReqId;
    private int expiresIn;
    private int interval;

    public AuthenticationRequestAcknowledgement(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        ObjectNode json = asJson();
        authReqId = json.get("auth_req_id").asText();
        expiresIn = json.get("expires_in").asInt();
        interval = json.has("interval") ? json.get("interval").asInt() : -1;
    }

    public String getAuthReqId() {
        return authReqId;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public int getInterval() {
        return interval;
    }

}
