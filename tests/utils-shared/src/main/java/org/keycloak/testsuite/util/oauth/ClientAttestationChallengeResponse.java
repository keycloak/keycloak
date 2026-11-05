package org.keycloak.testsuite.util.oauth;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.CloseableHttpResponse;

/**
 * @author <a href="mailto:ogenbertrand@gmail.com">Bertrand Ogen</a>
 */
public class ClientAttestationChallengeResponse extends AbstractHttpResponse {

    private ObjectNode challengeResponse;

    public ClientAttestationChallengeResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        challengeResponse = asJson();
    }

    public String getAttestationChallenge() {
        return Optional.ofNullable(challengeResponse)
                .filter(json -> json.hasNonNull("attestation_challenge"))
                .map(json -> json.get("attestation_challenge").asText())
                .orElseThrow(() -> new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription())));
    }

    public ObjectNode getChallengeResponse() {
        return challengeResponse;
    }
}
