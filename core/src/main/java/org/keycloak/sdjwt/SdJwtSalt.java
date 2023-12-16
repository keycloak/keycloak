package org.keycloak.sdjwt;

/**
 * Strong typing salt to avoid parameter mismatch.
 * 
 * Comparable to allow sorting in SD JWT VC.
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class SdJwtSalt implements Comparable<SdJwtSalt> {
    private final String salt;

    public SdJwtSalt(String salt) {
        this.salt = SdJwtUtils.requireNonEmpty(salt, "salt must not be empty");
    }

    // Handy factory method
    public static SdJwtSalt of(String salt) {
        return new SdJwtSalt(salt);
    }

    @Override
    public String toString() {
        return salt;
    }

    @Override
    public int compareTo(SdJwtSalt o) {
        return salt.compareTo(o.salt);
    }
}
