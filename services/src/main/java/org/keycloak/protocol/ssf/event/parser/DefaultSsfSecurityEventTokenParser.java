package org.keycloak.protocol.ssf.event.parser;

import java.nio.charset.StandardCharsets;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.keys.SsfTransmitterPublicKeyLoader;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterMetadata;

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
     * Parses the encoded SecurityEventToken in the context of the given {@link SsfReceiver} into a {@link SecurityEventToken}.
     *
     * The parsing decodes the SecurityEventToken and validates it's signature.
     *
     * @param encodedSecurityEventToken
     * @param receiver
     * @return
     */
    @Override
    public SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfReceiver receiver) {

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
    protected SecurityEventToken decode(String encodedSecurityEventToken, SsfReceiver receiver) {

        if (encodedSecurityEventToken == null) {
            return null;
        }

        try {
            JWSInput jws = new JWSInput(encodedSecurityEventToken);
            JWSHeader header = jws.getHeader();
            String kid = header.getKeyId();
            String alg = header.getRawAlgorithm();

            String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(session.getContext().getRealm().getId(), receiver.getConfig().getInternalId());

            KeyWrapper publicKey = resolveTransmitterPublicKey(receiver, modelKey, kid, alg);

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
                return null;
            }

            return jws.readJsonContent(SecurityEventToken.class);
        } catch (Exception e) {
            LOG.debug("Failed to decode token", e);
            return null;
        }
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
        SsfTransmitterMetadata transmitterMetadata = receiver.getTransmitterMetadata();
        SsfTransmitterPublicKeyLoader loader = new SsfTransmitterPublicKeyLoader(session, transmitterMetadata);
        KeyWrapper publicKey = keyStorage.getPublicKey(modelKey, kid, alg, loader);
        return publicKey;
    }
}
