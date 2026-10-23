package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;

/**
 * @author <a href="mailto:ogenbertrand@gmail.com">Bertrand Ogen</a>
 */
public class ClientAttestationChallengeRequest extends AbstractHttpPostRequest<ClientAttestationChallengeRequest, ClientAttestationChallengeResponse> {

    public ClientAttestationChallengeRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getClientAttestationChallenge();
    }

    @Override
    protected void initRequest() {
    }

    @Override
    protected ClientAttestationChallengeResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new ClientAttestationChallengeResponse(response);
    }
}
