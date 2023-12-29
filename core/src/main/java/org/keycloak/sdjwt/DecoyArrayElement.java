package org.keycloak.sdjwt;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class DecoyArrayElement extends DecoyEntry {

    private final Integer index;

    private DecoyArrayElement(SdJwtSalt salt, Integer index) {
        super(salt);
        this.index = index;
    }

    public JsonNode getVisibleValue(String hashAlg) {
        return SdJwtUtils.mapper.createObjectNode().put("...", getDisclosureDigest(hashAlg));
    }

    public Integer getIndex() {
        return index;
    }

    public static class Builder {
        private SdJwtSalt salt;
        private Integer index;

        public Builder withSalt(SdJwtSalt salt) {
            this.salt = salt;
            return this;
        }

        public Builder atIndex(Integer index) {
            this.index = index;
            return this;
        }

        public DecoyArrayElement build() {
            salt = salt == null ? new SdJwtSalt(SdJwtUtils.randomSalt()) : salt;
            return new DecoyArrayElement(salt, index);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
