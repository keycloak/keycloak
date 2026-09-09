package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.representations.account.UserRepresentation;

import org.apache.http.client.methods.CloseableHttpResponse;


public class AccountResponse extends AbstractHttpResponse {

    private UserRepresentation userRepresentation;

    public AccountResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        userRepresentation = asJson(UserRepresentation.class);
    }

    public UserRepresentation getUserRepresentation() {
        return userRepresentation;
    }
}
