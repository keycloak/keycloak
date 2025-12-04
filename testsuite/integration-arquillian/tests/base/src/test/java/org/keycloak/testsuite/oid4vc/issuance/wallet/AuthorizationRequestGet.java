package org.keycloak.testsuite.oid4vc.issuance.wallet;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.client.cli.util.IoUtil;
import org.keycloak.protocol.oid4vc.model.AuthorizationRequest;
import org.keycloak.testsuite.util.oauth.AbstractHttpGetRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;

public class AuthorizationRequestGet extends AbstractHttpGetRequest<String> {

    private final AuthorizationRequest authRequest;

    public AuthorizationRequestGet(AbstractOAuthClient<?> client, AuthorizationRequest authRequest) {
        super(client);
        this.authRequest = authRequest;
    }

    @Override
    protected String getEndpoint() {
        String authEndpoint = client.getEndpoints().getAuthorization();
        String endpointUrl = authRequest.toRequestUrl(authEndpoint);
        return endpointUrl;
    }

    @Override
    protected void initRequest() {
    }

    @Override
    protected String toResponse(CloseableHttpResponse response) throws IOException {

        String bodyAsText = IoUtil.readFully(response.getEntity().getContent());
        int status = response.getStatusLine().getStatusCode();
        if (status != HttpServletResponse.SC_FOUND) {
            throw new IOException(String.format("[%d] Unexpected response: %s", status, bodyAsText));
        }

        String location = Optional.ofNullable(response.getFirstHeader("location"))
                .map(NameValuePair::getValue)
                .orElseThrow(() -> new IOException("No location header"));

        return location;
    }
}
