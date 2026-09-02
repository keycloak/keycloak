package org.keycloak.ssf.transmitter.event;

import java.util.Map;

import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.event.token.SecurityEventToken;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;

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

        // CAEP Interop Profile 1.0 §2.8.1 MUST: a SET carries exactly
        // one event. Our generators always produce a single entry, but
        // a defensive check here fails loud if a future refactor
        // accidentally ships a multi-event SET rather than silently
        // emitting a spec-violating token.
        if (token instanceof SsfSecurityEventToken ssfToken) {
            Map<String, Object> events = ssfToken.getEvents();
            if (events == null || events.size() != 1) {
                throw new SsfException("SSF SET must carry exactly one event (events map size="
                        + (events == null ? 0 : events.size()) + ")");
            }
        }

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
