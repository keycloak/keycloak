package org.keycloak.tests.oid4vc;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.util.JsonSerialization;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_DETAILS;

/**
 * Utility access to OID4VCAuthorizationDetails
 */
public final class OID4VCAuthorizationDetailsUtil {

    // Hide ctor
    private OID4VCAuthorizationDetailsUtil() {}

    /**
     * Access AuthorizationDetails from the AccessToken JWT
     * rather than from the Token endpoint response
     */
    public static List<OID4VCAuthorizationDetail> getAuthorizationDetailsFromAccessToken(String accessToken) {
        String tokenContent;
        try {
            tokenContent = new JWSInput(accessToken).readContentAsString();
        } catch (JWSInputException e) {
            throw new IllegalArgumentException("Cannot parse access token", e);
        }
        LinkedHashMap<?, ?> contentMap = JsonSerialization.valueFromString(tokenContent, LinkedHashMap.class);
        return Optional.ofNullable(contentMap.get(AUTHORIZATION_DETAILS))
                .map(JsonSerialization::valueAsString)
                .map(it -> JsonSerialization.valueFromString(it, OID4VCAuthorizationDetail[].class))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
    }
}
