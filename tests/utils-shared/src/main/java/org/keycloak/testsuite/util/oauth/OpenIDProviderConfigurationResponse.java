package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;

import java.io.IOException;

public class OpenIDProviderConfigurationResponse extends AbstractHttpResponse {

    private OIDCConfigurationRepresentation oidcConfiguration;

    public OpenIDProviderConfigurationResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        oidcConfiguration = asJson(OIDCConfigurationRepresentation.class);
    }

    public OIDCConfigurationRepresentation getOidcConfiguration() {
        return oidcConfiguration;
    }

}
