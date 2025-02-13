package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.keycloak.utils.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class IntrospectionRequest extends AbstractHttpPostRequest<IntrospectionRequest, String> {

    private final String tokenType;
    private final String tokenToIntrospect;
    private boolean jwtResponse = false;

    IntrospectionRequest(String tokenToIntrospect, String tokenType, OAuthClient client) {
        super(client);
        this.tokenType = tokenType;
        this.tokenToIntrospect = tokenToIntrospect;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getIntrospection();
    }

    public IntrospectionRequest jwtResponse() {
        this.jwtResponse = true;
        return this;
    }

    protected void initRequest() {
        parameter("token", tokenToIntrospect);
        parameter("token_type_hint", tokenType);
    }

    @Override
    protected String toResponse(CloseableHttpResponse response) throws IOException {
        return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    }

    @Override
    protected String getAccept() {
        return jwtResponse ? MediaType.APPLICATION_JWT : super.getAccept();
    }
}
