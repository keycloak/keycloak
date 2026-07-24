package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;
import java.util.Optional;

import org.keycloak.OAuth2Constants;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.CloseableHttpResponse;

public class Oid4vpDirectPostResponse extends AbstractHttpResponse {

    private ObjectNode body;

    public Oid4vpDirectPostResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        body = asJson();
    }

    // The browser url that finishes the login after a verified presentation
    public String getRedirectUri() {
        return Optional.ofNullable(body)
                .map(node -> node.path(OAuth2Constants.REDIRECT_URI).textValue())
                .orElseThrow(() -> new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription())));
    }
}
