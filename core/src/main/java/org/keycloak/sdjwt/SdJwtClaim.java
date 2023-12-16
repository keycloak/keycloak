package org.keycloak.sdjwt;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a top level claim in the payload of a JWT.
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public interface SdJwtClaim {

    public SdJwtClaimName getClaimName();

    public String getClaimNameAsString();

    public JsonNode getVisibleClaimValue(String hashAlgo);

    public List<String> getDisclosureStrings();

}
