package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;

public class OpenIDProviderConfigurationRequest extends AbstractHttpGetRequest<OpenIDProviderConfigurationRequest, OpenIDProviderConfigurationResponse> {

    public OpenIDProviderConfigurationRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    public OpenIDProviderConfigurationRequest url(String url) {
        return endpoint(url + (url.endsWith("/") ? "" : "/") + ".well-known/openid-configuration");
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getOpenIDConfiguration();
    }

    @Override
    protected void initRequest() {
    }

    @Override
    protected OpenIDProviderConfigurationResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new OpenIDProviderConfigurationResponse(response);
    }

}
