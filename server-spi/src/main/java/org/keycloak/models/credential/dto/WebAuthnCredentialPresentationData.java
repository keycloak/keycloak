package org.keycloak.models.credential.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebAuthnCredentialPresentationData extends WebAuthnCredentialData {

    private final String authenticatorProvider;

    @JsonCreator
    public WebAuthnCredentialPresentationData(@JsonProperty("aaguid") String aaguid,
                                              @JsonProperty("credentialId") String credentialId,
                                              @JsonProperty("counter") long counter,
                                              @JsonProperty("attestationStatement") String attestationStatement,
                                              @JsonProperty("credentialPublicKey") String credentialPublicKey,
                                              @JsonProperty("attestationStatementFormat") String attestationStatementFormat,
                                              @JsonProperty("transports") Set<String> transports,
                                              @JsonProperty("authenticatorProvider") String authenticatorProvider) {
        super(aaguid, credentialId, counter, attestationStatement, credentialPublicKey, attestationStatementFormat, transports);
        this.authenticatorProvider = authenticatorProvider;
    }

    public String getAuthenticatorProvider() {
        return authenticatorProvider;
    }
}
