package org.keycloak.saml.processing.core.parsers.saml.metadata;

/**
 * @author mhajas
 */
public class SAMLAssertinIDRequestServiceParser extends SAMLEndpointTypeParser {

    private static final SAMLAssertinIDRequestServiceParser INSTANCE = new SAMLAssertinIDRequestServiceParser();

    public SAMLAssertinIDRequestServiceParser() {
        super(SAMLMetadataQNames.ASSERTION_ID_REQUEST_SERVICE);
    }

    public static SAMLAssertinIDRequestServiceParser getInstance() {
        return INSTANCE;
    }
}
