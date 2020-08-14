package org.keycloak.crypto;

import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;

public interface DecryptionVerifierContext {

    String decrypt(String jweString, JWEHeader jweHeader) throws JWEException;
}
