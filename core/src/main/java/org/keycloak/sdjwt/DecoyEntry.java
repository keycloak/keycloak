package org.keycloak.sdjwt;

import java.util.Objects;

import org.keycloak.jose.jws.crypto.HashUtils;

/**
 * Handles hash production for a decoy entry from the given salt.
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 * 
 */
public abstract class DecoyEntry {
    private final SdJwtSalt salt;

    protected DecoyEntry(SdJwtSalt salt) {
        this.salt = Objects.requireNonNull(salt, "DecoyEntry allways requires a non null salt");
    }

    public SdJwtSalt getSalt() {
        return salt;
    }

    public String getDisclosureDigest(String hashAlg) {
        return SdJwtUtils.encodeNoPad(HashUtils.hash(hashAlg, salt.toString().getBytes()));
    }
}
