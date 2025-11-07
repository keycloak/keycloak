package org.keycloak.protocol.ssf.event.parser;

import org.jboss.logging.Logger;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.keys.SsfTransmitterPublicKeyLoader;
import org.keycloak.protocol.ssf.receiver.spi.SsfReceiver;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;

import java.nio.charset.StandardCharsets;

public class DefaultSsfSecurityEventTokenParser implements SsfSecurityEventTokenParser {

    protected static final Logger log = Logger.getLogger(DefaultSsfSecurityEventTokenParser.class);

    protected final KeycloakSession session;

    public DefaultSsfSecurityEventTokenParser(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfReceiver receiver) {

        try {
            // custom decode method to use keys from ReceiverComponent
            var securityEventToken = decode(encodedSecurityEventToken, receiver);
            return securityEventToken;
        } catch (Exception e) {
            throw new SecurityEventTokenParsingException("Could not parse security event token", e);
        }
    }

    protected SecurityEventToken decode(String encodedSecurityEventToken, SsfReceiver receiver) {

        if (encodedSecurityEventToken == null) {
            return null;
        }

        try {
            JWSInput jws = new JWSInput(encodedSecurityEventToken);
            JWSHeader header = jws.getHeader();
            String kid = header.getKeyId();
            String alg = header.getRawAlgorithm();


            String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(session.getContext().getRealm().getId(), receiver.getReceiverModel().getReceiverProviderConfig().getInternalId());

            PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);
            SsfTransmitterMetadata transmitterMetadata = receiver.getTransmitterMetadata();
            SsfTransmitterPublicKeyLoader loader = new SsfTransmitterPublicKeyLoader(session, transmitterMetadata);
            KeyWrapper publicKey = keyStorage.getPublicKey(modelKey, kid, alg, loader);

            if (publicKey == null) {
                throw new SecurityEventTokenParsingException("Could not find publicKey with kid " + kid);
            }

            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, alg);
            if (signatureProvider == null) {
                throw new SecurityEventTokenParsingException("Could not find verifier for alg " + alg);
            }

            byte[] tokenBytes = jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8);
            boolean valid = signatureProvider.verifier(publicKey)
                    .verify(tokenBytes, jws.getSignature());
            return valid ? jws.readJsonContent(SecurityEventToken.class) : null;
        } catch (Exception e) {
            log.debug("Failed to decode token", e);
            return null;
        }
    }
}
