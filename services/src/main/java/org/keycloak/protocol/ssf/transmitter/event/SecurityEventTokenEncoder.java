package org.keycloak.protocol.ssf.transmitter.event;

import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.event.token.SecurityEventToken;

public class SecurityEventTokenEncoder {

    private final KeycloakSession session;

    public SecurityEventTokenEncoder(KeycloakSession session) {
        this.session = session;
    }

    public String encode(SecurityEventToken token) {

        String signatureAlgorithm = getSignatureAlgorithm(token);

        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);
        SignatureSignerContext signer = signatureProvider.signer();

        String encodedToken = newJwsBuilder().jsonContent(token).sign(signer);
        return encodedToken;
    }

    protected String getSignatureAlgorithm(SecurityEventToken token) {
        // TODO configure signature algorithm for SET tokens
        return session.tokens().signatureAlgorithm(token.getCategory());
    }

    protected JWSBuilder newJwsBuilder() {
        return new JWSBuilder().type(Ssf.SECEVENT_JWT_TYPE);
    }
}
