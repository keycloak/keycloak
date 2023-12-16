package org.keycloak.sdjwt;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 * 
 */
public interface SdJwtArrayElement {
    /**
     * Returns the value visibly printed as array element
     * in the issuer signed jwt.
     */
    public JsonNode getVisibleValue(String hashAlg);

    public String getDisclosureString();
}
