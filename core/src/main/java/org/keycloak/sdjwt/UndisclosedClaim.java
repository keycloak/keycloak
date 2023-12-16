package org.keycloak.sdjwt;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class UndisclosedClaim extends Disclosable implements SdJwtClaim {
    private final SdJwtClaimName claimName;
    private final JsonNode claimValue;

    private UndisclosedClaim(SdJwtClaimName claimName, SdJwtSalt salt, JsonNode claimValue) {
        super(salt);
        this.claimName = claimName;
        this.claimValue = claimValue;
    }

    @Override
    Object[] toArray() {
        return new Object[] { getSaltAsString(), getClaimNameAsString(), claimValue };
    }

    @Override
    public SdJwtClaimName getClaimName() {
        return claimName;
    }

    @Override
    public String getClaimNameAsString() {
        return claimName.toString();
    }

    /**
     * Recall no info is visible on these claims in the JWT.
     */
    @Override
    public JsonNode getVisibleClaimValue(String hashAlgo) {
        throw new UnsupportedOperationException("Unimplemented method 'getVisibleClaimValue'");
    }

    public static class Builder {
        private SdJwtClaimName claimName;
        private SdJwtSalt salt;
        private JsonNode claimValue;

        public Builder withClaimName(String claimName) {
            this.claimName = new SdJwtClaimName(claimName);
            return this;
        }

        public Builder withSalt(SdJwtSalt salt) {
            this.salt = salt;
            return this;
        }

        public Builder withClaimValue(JsonNode claimValue) {
            this.claimValue = claimValue;
            return this;
        }

        public UndisclosedClaim build() {
            claimName = Objects.requireNonNull(claimName, "claimName must not be null");
            claimValue = Objects.requireNonNull(claimValue, "claimValue must not be null");
            salt = salt == null ? new SdJwtSalt(SdJwtUtils.randomSalt()) : salt;
            return new UndisclosedClaim(claimName, salt, claimValue);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<String> getDisclosureStrings() {
        return Collections.singletonList(getDisclosureString());
    }
}
