package org.keycloak.saml.processing.core.parsers.saml.metadata;

/**
 * @author mhajas
 */
public class SAMLAuthzServiceParser extends SAMLEndpointTypeParser {

    private static final SAMLAuthzServiceParser INSTANCE = new SAMLAuthzServiceParser();

    public SAMLAuthzServiceParser() {
        super(SAMLMetadataQNames.AUTHZ_SERVICE);
    }

    public static SAMLAuthzServiceParser getInstance() {
        return INSTANCE;
    }
}
