package org.keycloak.sdjwt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Handles selective disclosure of elements within a top-level array claim,
 * supporting both visible and undisclosed elements.
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 * 
 */
public class ArrayDisclosure extends AbstractSdJwtClaim {
    private final List<SdJwtArrayElement> elements;
    private JsonNode visibleClaimValue = null;

    private ArrayDisclosure(SdJwtClaimName claimName, List<SdJwtArrayElement> elements) {
        super(claimName);
        this.elements = Collections.unmodifiableList(elements == null ? Collections.emptyList() : elements);
    }

    /**
     * Print the array with visible and invisible elements.
     */
    @Override
    public JsonNode getVisibleClaimValue(String hashAlgo) {
        if (visibleClaimValue != null)
            return visibleClaimValue;

        final ArrayNode n = SdJwtUtils.mapper.createArrayNode();
        elements.stream()
                .filter(Objects::nonNull)
                .forEach(e -> n.add(e.getVisibleValue(hashAlgo)));

        visibleClaimValue = n;
        return visibleClaimValue;
    }

    @Override
    public List<String> getDisclosureStrings() {
        final List<String> disclosureStrings = new ArrayList<>();
        elements.stream()
                .filter(Objects::nonNull)
                .forEach(e -> {
                    String disclosureString = e.getDisclosureString();
                    if (disclosureString != null)
                        disclosureStrings.add(disclosureString);
                });
        return disclosureStrings;
    }

    public static class Builder {
        private SdJwtClaimName claimName;
        private List<SdJwtArrayElement> elements = new ArrayList<>();

        public Builder withClaimName(String claimName) {
            this.claimName = new SdJwtClaimName(claimName);
            return this;
        }

        public Builder withVisibleElement(JsonNode elementValue) {
            this.elements.add(new VisibleArrayElement(elementValue));
            return this;
        }

        public Builder withUndisclosedElement(JsonNode elementValue) {
            SdJwtSalt sdJwtSalt = new SdJwtSalt(SdJwtUtils.randomSalt());
            this.elements.add(UndisclosedArrayElement.builder()
                    .withSalt(sdJwtSalt)
                    .withArrayElement(elementValue)
                    .build());
            return this;
        }

        public Builder withUndisclosedElement(String salt, JsonNode elementValue) {
            SdJwtSalt sdJwtSalt = new SdJwtSalt(salt == null ? SdJwtUtils.randomSalt() : salt);
            this.elements.add(UndisclosedArrayElement.builder()
                    .withSalt(sdJwtSalt)
                    .withArrayElement(elementValue)
                    .build());
            return this;
        }

        public ArrayDisclosure build() {
            return new ArrayDisclosure(claimName, elements);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
