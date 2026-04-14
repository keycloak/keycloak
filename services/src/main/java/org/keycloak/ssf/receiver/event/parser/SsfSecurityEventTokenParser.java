package org.keycloak.ssf.receiver.event.parser;

import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.receiver.SsfReceiver;

/**
 * Parser for RFC8417 Security Event Token (SET).
 *
 * @see https://datatracker.ietf.org/doc/html/rfc8417
 */
public interface SsfSecurityEventTokenParser {

    /**
     * Parses the encoded SecurityEventToken in the context of the given {@link SsfReceiver} into a {@link SsfSecurityEventToken}.
     * <p>
     * The parsing should decode the SecurityEventToken and validate it's signature.
     *
     * @param encodedSecurityEventToken
     * @param receiver
     * @return
     */
    SsfSecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfReceiver receiver);
}
