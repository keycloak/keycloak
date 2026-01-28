package org.keycloak.saml.processing.core.parsers.saml.metadata;

/**
 * @author mhajas
 */
public class SAMLAssertionConsumerServiceParser extends SAMLIndexedEndpointTypeParser {

    private static final SAMLAssertionConsumerServiceParser INSTANCE = new SAMLAssertionConsumerServiceParser();

    public SAMLAssertionConsumerServiceParser() {
        super(SAMLMetadataQNames.ASSERTION_CONSUMER_SERVICE);
    }

    public static SAMLAssertionConsumerServiceParser getInstance() {
        return INSTANCE;
    }
}
