package org.keycloak.sdjwt;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class VisibleArrayElement implements SdJwtArrayElement {
    private final JsonNode arrayElement;

    public VisibleArrayElement(JsonNode arrayElement) {
        this.arrayElement = arrayElement;
    }

    @Override
    public JsonNode getVisibleValue(String hashAlg) {
        return arrayElement;
    }

    @Override
    public String getDisclosureString() {
        return null;
    }
}
