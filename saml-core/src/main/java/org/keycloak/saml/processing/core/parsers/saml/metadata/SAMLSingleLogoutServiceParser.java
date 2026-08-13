package org.keycloak.saml.processing.core.parsers.saml.metadata;

/**
 * @author mhajas
 */
public class SAMLSingleLogoutServiceParser extends SAMLEndpointTypeParser {

    private static final SAMLSingleLogoutServiceParser INSTANCE = new SAMLSingleLogoutServiceParser();

    public SAMLSingleLogoutServiceParser() {
        super(SAMLMetadataQNames.SINGLE_LOGOUT_SERVICE);
    }

    public static SAMLSingleLogoutServiceParser getInstance() {
        return INSTANCE;
    }
}
