package org.keycloak.protocol.ssf.transmitter.event;

import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.SsfException;
import org.keycloak.protocol.ssf.event.token.SecurityEventToken;

/**
 * Produces a signed, base64url-encoded JWS for a given SSF Security Event
 * Token. Deliberately kept as a thin JWS wrapper: algorithm selection and
 * allow-list enforcement live in
 * {@link SsfSignatureAlgorithms} and are resolved by the dispatcher before
 * calling {@link #encode(SecurityEventToken, String)}. This keeps the
 * encoder decoupled from the transmitter config and receiver-stream state.
 */
public class SecurityEventTokenEncoder {

    private final KeycloakSession session;

    public SecurityEventTokenEncoder(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Signs and encodes the given token with the requested JWS algorithm.
     *
     * @throws SsfException if the realm has no {@link SignatureProvider}
     *         registered for {@code signatureAlgorithm}. This surfaces
     *         FIPS-restricted or misconfigured realms with a clear error
     *         instead of NPE'ing deeper in {@link JWSBuilder}.
     */
    public String encode(SecurityEventToken token, String signatureAlgorithm) {

        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);
        if (signatureProvider == null) {
            throw new SsfException("No signer available for SSF SET signature algorithm " + signatureAlgorithm
                    + " — check the realm's active keys and FIPS/BCFIPS configuration.");
        }

        SignatureSignerContext signer = signatureProvider.signer();
        return newJwsBuilder().jsonContent(token).sign(signer);
    }

    protected JWSBuilder newJwsBuilder() {
        return new JWSBuilder().type(Ssf.SECEVENT_JWT_TYPE);
    }
}
