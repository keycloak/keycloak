package org.keycloak.sdjwt;

/**
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class DecoyClaim extends DecoyEntry {

    private DecoyClaim(SdJwtSalt salt) {
        super(salt);
    }

    public static class Builder {
        private SdJwtSalt salt;

        public Builder withSalt(SdJwtSalt salt) {
            this.salt = salt;
            return this;
        }

        public DecoyClaim build() {
            salt = salt == null ? new SdJwtSalt(SdJwtUtils.randomSalt()) : salt;
            return new DecoyClaim(salt);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
