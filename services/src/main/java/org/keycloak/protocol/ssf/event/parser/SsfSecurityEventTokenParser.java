package org.keycloak.protocol.ssf.event.parser;

import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.receiver.spi.SsfReceiver;

public interface SsfSecurityEventTokenParser {

    SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfReceiver receiver);
}
