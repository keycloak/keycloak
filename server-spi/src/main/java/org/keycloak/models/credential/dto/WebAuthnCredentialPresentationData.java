package org.keycloak.models.credential.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebAuthnCredentialPresentationData extends WebAuthnCredentialData {

    private final String authenticatorProvider;
    private final String iconLight;
    private final String iconDark;

    @JsonCreator
    public WebAuthnCredentialPresentationData(@JsonProperty("aaguid") String aaguid,
                                              @JsonProperty("credentialId") String credentialId,
                                              @JsonProperty("counter") long counter,
                                              @JsonProperty("attestationStatement") String attestationStatement,
                                              @JsonProperty("credentialPublicKey") String credentialPublicKey,
                                              @JsonProperty("attestationStatementFormat") String attestationStatementFormat,
                                              @JsonProperty("transports") Set<String> transports,
                                              @JsonProperty("authenticatorProvider") String authenticatorProvider,
                                              @JsonProperty("iconLight") String iconLight,
                                              @JsonProperty("iconDark") String iconDark) {
        super(aaguid, credentialId, counter, attestationStatement, credentialPublicKey, attestationStatementFormat, transports);
        this.authenticatorProvider = authenticatorProvider;
        this.iconLight = iconLight;
        this.iconDark = iconDark;
    }

    public String getAuthenticatorProvider() {
        return authenticatorProvider;
    }

    public String getIconLight() {
        return iconLight;
    }

    public String getIconDark() {
        return iconDark;
    }
}
