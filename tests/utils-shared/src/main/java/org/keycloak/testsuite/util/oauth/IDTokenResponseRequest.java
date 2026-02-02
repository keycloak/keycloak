package org.keycloak.testsuite.util.oauth;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.protocol.oid4vc.model.IDTokenResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

public class IDTokenResponseRequest extends AbstractHttpPostRequest<IDTokenResponseRequest, AuthorizationRequestResponse> {

    private final String endpointUri;

    public IDTokenResponseRequest(AbstractOAuthClient<?> client, String endpointUri, IDTokenResponse idTokenResponse) {
        super(client);
        this.endpointUri = endpointUri;
        this.parameters.add(new BasicNameValuePair("id_token", idTokenResponse.idToken));
    }

    @Override
    protected String getEndpoint() {
        return endpointUri;
    }

    @Override
    protected void authorization() {
    }

    @Override
    protected void initRequest() {
    }

    @Override
    protected AuthorizationRequestResponse toResponse(CloseableHttpResponse response) throws IOException {
        String location = new AuthorizationRedirectResponse(response).getRedirectLocation();
        try {
            Map<String, String> params = new URIBuilder(location).getQueryParams().stream()
                    .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue, (a, b) -> b));
            return new AuthorizationRequestResponse(params);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
