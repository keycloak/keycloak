package org.keycloak.sdjwt;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 * 
 */
public abstract class AbstractSdJwtClaim implements SdJwtClaim {
    private final SdJwtClaimName claimName;

    public AbstractSdJwtClaim(SdJwtClaimName claimName) {
        this.claimName = claimName;
    }

    @Override
    public SdJwtClaimName getClaimName() {
        return claimName;
    }

    @Override
    public String getClaimNameAsString() {
        return claimName.toString();
    }
}
