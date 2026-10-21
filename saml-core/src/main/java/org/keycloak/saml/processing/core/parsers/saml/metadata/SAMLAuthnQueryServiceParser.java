package org.keycloak.saml.processing.core.parsers.saml.metadata;

/**
 * @author mhajas
 */
public class SAMLAuthnQueryServiceParser extends SAMLEndpointTypeParser {

    private static final SAMLAuthnQueryServiceParser INSTANCE = new SAMLAuthnQueryServiceParser();

    public SAMLAuthnQueryServiceParser() {
        super(SAMLMetadataQNames.AUTHN_QUERY_SERVICE);
    }

    public static SAMLAuthnQueryServiceParser getInstance() {
        return INSTANCE;
    }
}
