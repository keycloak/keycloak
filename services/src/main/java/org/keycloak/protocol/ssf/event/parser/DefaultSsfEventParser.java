package org.keycloak.protocol.ssf.event.parser;

import org.jboss.logging.Logger;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;

import java.nio.charset.StandardCharsets;

public class DefaultSsfEventParser implements SsfEventParser {

    protected static final Logger log = Logger.getLogger(DefaultSsfEventParser.class);

    protected final KeycloakSession session;

    public DefaultSsfEventParser(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfReceiver receiver) {

        try {
            // custom decode method to use keys from ReceiverComponent
            var securityEventToken = decode(encodedSecurityEventToken, receiver);
            return securityEventToken;
        } catch (Exception e) {
            throw new SsfParsingException("Could not parse security event token", e);
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

            KeyWrapper key = receiver.getKeys()
                    .filter(kw -> kw.getKid().equals(kid) && kw.getAlgorithm().equals(alg))
                    .findFirst()
                    .orElse(null);
            if (key == null) {
                throw new SsfParsingException("Could not find key with kid " + kid);
            }

            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, alg);
            if (signatureProvider == null) {
                throw new SsfParsingException("Could not find verifier for alg " + alg);
            }

            byte[] tokenBytes = jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8);
            boolean valid = signatureProvider.verifier(key)
                    .verify(tokenBytes, jws.getSignature());
            return valid ? jws.readJsonContent(SecurityEventToken.class) : null;
        } catch (Exception e) {
            log.debug("Failed to decode token", e);
            return null;
        }
    }
}
