package org.keycloak.ssf.receiver.event.parser;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.metadata.TransmitterMetadata;
import org.keycloak.ssf.receiver.SsfReceiver;
import org.keycloak.ssf.receiver.keys.TransmitterPublicKeyLoader;

import org.jboss.logging.Logger;

/**
 * Default implementation of a {@link SsfSecurityEventTokenParser}.
 */
public class DefaultSsfSecurityEventTokenParser implements SsfSecurityEventTokenParser {

    protected static final Logger LOG = Logger.getLogger(DefaultSsfSecurityEventTokenParser.class);

    protected final KeycloakSession session;

    public DefaultSsfSecurityEventTokenParser(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Parses the encoded SecurityEventToken in the context of the given {@link SsfReceiver} into a {@link SsfSecurityEventToken}.
     *
     * The parsing decodes the SecurityEventToken and validates it's signature.
     *
     * @param encodedSecurityEventToken
     * @param receiver
     * @return
     */
    @Override
    public SsfSecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfReceiver receiver) {

        try {
            return decode(encodedSecurityEventToken, receiver);
        } catch (Exception e) {
            throw new SecurityEventTokenParsingException("Could not parse security event token", e);
        }
    }

    /**
     * Decode and validate the given encoded Security Event Token string.
     * @param encodedSecurityEventToken
     * @param receiver
     * @return
     */
    protected SsfSecurityEventToken decode(String encodedSecurityEventToken, SsfReceiver receiver) {

        if (encodedSecurityEventToken == null) {
            return null;
        }

        try {
            JWSInput jws = new JWSInput(encodedSecurityEventToken);
            JWSHeader header = jws.getHeader();

            String typ = header.getType();
            if (!Ssf.SECEVENT_JWT_TYPE.equals(typ)) {
                throw new SecurityEventTokenParsingException("Invalid SET typ " + typ + ". Expected: " + Ssf.SECEVENT_JWT_TYPE);
            }

            String kid = header.getKeyId();
            String alg = header.getRawAlgorithm();

            // Gate on the receiver's allow-list BEFORE any key lookup or
            // signer construction. Rejects the classic JWS alg-confusion
            // surface where a compromised transmitter could downgrade or
            // swap the signing algorithm, and avoids hitting the JWKS
            // loader with a disallowed alg. Defaults to RS256 when the
            // receiver config does not explicitly override it, per CAEP
            // interoperability profile 1.0 §2.6.
            Set<String> expectedAlgorithms = receiver.getConfig().getExpectedSignatureAlgorithms();
            if (alg == null || !expectedAlgorithms.contains(alg)) {
                throw new SecurityEventTokenParsingException(
                        "SET signed with disallowed alg=" + alg + " (expected one of " + expectedAlgorithms + ")");
            }

            KeyWrapper publicKey = getKeyWrapper(receiver, kid, alg);

            if (publicKey == null) {
                throw new SecurityEventTokenParsingException("Could not find publicKey with kid " + kid);
            }

            SignatureProvider signatureProvider = resolveSignatureProvider(alg);
            if (signatureProvider == null) {
                throw new SecurityEventTokenParsingException("Could not find verifier for alg " + alg);
            }

            byte[] tokenBytes = jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8);
            boolean valid = verify(signatureProvider, publicKey, tokenBytes, jws);
            if (!valid) {
                throw new SecurityEventTokenParsingException("Invalid signature");
            }

            return jws.readJsonContent(SsfSecurityEventToken.class);
        } catch (Exception e) {
            LOG.debug("Failed to decode token", e);
            throw new SecurityEventTokenParsingException("Failed to decore token", e);
        }
    }

    protected KeyWrapper getKeyWrapper(SsfReceiver receiver, String kid, String alg) {
        String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(session.getContext().getRealm().getId(), receiver.getConfig().getInternalId());
        KeyWrapper publicKey = resolveTransmitterPublicKey(receiver, modelKey, kid, alg);
        return publicKey;
    }

    /**
     * Verify the token signature.
     * @param signatureProvider
     * @param publicKey
     * @param tokenBytes
     * @param jws
     * @return
     * @throws VerificationException
     */
    protected boolean verify(SignatureProvider signatureProvider, KeyWrapper publicKey, byte[] tokenBytes, JWSInput jws) throws VerificationException {
        return signatureProvider.verifier(publicKey)
                .verify(tokenBytes, jws.getSignature());
    }

    /**
     * Resolve Signature provider.
     * @param alg
     * @return
     */
    protected SignatureProvider resolveSignatureProvider(String alg) {
        return session.getProvider(SignatureProvider.class, alg);
    }

    /**
     * Resolve public key of SSF Transmitter for signature validation.
     * @param receiver
     * @param modelKey
     * @param kid
     * @param alg
     * @return
     */
    protected KeyWrapper resolveTransmitterPublicKey(SsfReceiver receiver, String modelKey, String kid, String alg) {
        PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);
        TransmitterMetadata transmitterMetadata = receiver.getTransmitterMetadata();
        TransmitterPublicKeyLoader loader = new TransmitterPublicKeyLoader(session, transmitterMetadata);
        KeyWrapper publicKey = keyStorage.getPublicKey(modelKey, kid, alg, loader);
        return publicKey;
    }
}
