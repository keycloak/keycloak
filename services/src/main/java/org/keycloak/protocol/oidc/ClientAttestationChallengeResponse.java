package org.keycloak.protocol.oidc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Challenge response for OAuth 2.0 Attestation-Based Client Authentication.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-10#section-6.1">Challenge endpoint</a>
 * @author <a href="mailto:ogenbertrand@gmail.com">Bertrand Ogen</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientAttestationChallengeResponse {

    @JsonProperty("attestation_challenge")
    private String attestationChallenge;

    public String getAttestationChallenge() {
        return attestationChallenge;
    }

    public void setAttestationChallenge(String attestationChallenge) {
        this.attestationChallenge = attestationChallenge;
    }
}
