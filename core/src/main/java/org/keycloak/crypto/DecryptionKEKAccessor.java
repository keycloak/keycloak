package org.keycloak.crypto;

import java.security.PrivateKey;

public interface DecryptionKEKAccessor {

    PrivateKey getKEK(String kid, String algorithm);
}
