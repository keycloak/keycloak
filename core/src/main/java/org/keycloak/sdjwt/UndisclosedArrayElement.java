package org.keycloak.sdjwt;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class UndisclosedArrayElement extends Disclosable implements SdJwtArrayElement {
    private final JsonNode arrayElement;

    private UndisclosedArrayElement(SdJwtSalt salt, JsonNode arrayElement) {
        super(salt);
        this.arrayElement = arrayElement;
    }

    @Override
    public JsonNode getVisibleValue(String hashAlg) {
        return SdJwtUtils.mapper.createObjectNode().put("...", getDisclosureDigest(hashAlg));
    }

    @Override
    Object[] toArray() {
        return new Object[] { getSaltAsString(), arrayElement };
    }

    public static class Builder {
        private SdJwtSalt salt;
        private JsonNode arrayElement;

        public Builder withSalt(SdJwtSalt salt) {
            this.salt = salt;
            return this;
        }

        public Builder withArrayElement(JsonNode arrayElement) {
            this.arrayElement = arrayElement;
            return this;
        }

        public UndisclosedArrayElement build() {
            arrayElement = Objects.requireNonNull(arrayElement, "arrayElement must not be null");
            salt = salt == null ? new SdJwtSalt(SdJwtUtils.randomSalt()) : salt;
            return new UndisclosedArrayElement(salt, arrayElement);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
