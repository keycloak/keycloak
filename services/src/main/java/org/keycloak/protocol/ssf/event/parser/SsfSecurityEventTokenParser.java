package org.keycloak.protocol.ssf.event.parser;

import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;

/**
 * Parser for RFC8417 Security Event Token (SET).
 *
 * @see https://datatracker.ietf.org/doc/html/rfc8417
 */
public interface SsfSecurityEventTokenParser {

    /**
     * Parses the encoded SecurityEventToken in the context of the given {@link SsfReceiver} into a {@link SecurityEventToken}.
     * <p>
     * The parsing should decode the SecurityEventToken and validate it's signature.
     *
     * @param encodedSecurityEventToken
     * @param receiver
     * @return
     */
    SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfReceiver receiver);
}
