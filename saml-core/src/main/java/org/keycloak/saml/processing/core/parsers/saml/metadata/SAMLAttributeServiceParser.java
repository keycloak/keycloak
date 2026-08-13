package org.keycloak.saml.processing.core.parsers.saml.metadata;

/**
 * @author mhajas
 */
public class SAMLAttributeServiceParser extends SAMLEndpointTypeParser {

    private static final SAMLAttributeServiceParser INSTANCE = new SAMLAttributeServiceParser();

    public SAMLAttributeServiceParser() {
        super(SAMLMetadataQNames.ATTRIBUTE_SERVICE);
    }

    public static SAMLAttributeServiceParser getInstance() {
        return INSTANCE;
    }
}
