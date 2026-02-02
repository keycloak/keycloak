package org.keycloak.testsuite.util.oauth;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;

public class AuthorizationRedirectResponse extends AbstractHttpResponse {

    public AuthorizationRedirectResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    protected int getSuccessCode() {
        return HttpStatus.SC_MOVED_TEMPORARILY;
    }

    @Override
    protected void parseContent() throws IOException {
    }

    public String getRedirectLocation() {
        return Optional.ofNullable(getHeader("location")).orElseThrow(() ->
                new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription())));
    }
}
