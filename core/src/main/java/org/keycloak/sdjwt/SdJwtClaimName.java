package org.keycloak.sdjwt;

/**
 * Strong typing claim name to avoid parameter mismatch.
 * 
 * Used as map key. Beware of the hashcode and equals implementation.
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class SdJwtClaimName {
    private final String claimName;

    public SdJwtClaimName(String claimName) {
        this.claimName = SdJwtUtils.requireNonEmpty(claimName, "claimName must not be empty");
    }

    public static SdJwtClaimName of(String claimName) {
        return new SdJwtClaimName(claimName);
    }

    @Override
    public String toString() {
        return claimName;
    }

    @Override
    public int hashCode() {
        return claimName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SdJwtClaimName) {
            return claimName.equals(((SdJwtClaimName) obj).claimName);
        }
        return false;
    }
}
