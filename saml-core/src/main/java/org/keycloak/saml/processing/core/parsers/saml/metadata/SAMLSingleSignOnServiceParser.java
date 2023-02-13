package org.keycloak.saml.processing.core.parsers.saml.metadata;

/**
 * @author mhajas
 */
public class SAMLSingleSignOnServiceParser extends SAMLEndpointTypeParser {

    private static final SAMLSingleSignOnServiceParser INSTANCE = new SAMLSingleSignOnServiceParser();

    public SAMLSingleSignOnServiceParser() {
        super(SAMLMetadataQNames.SINGLE_SIGNON_SERVICE);
    }

    public static SAMLSingleSignOnServiceParser getInstance() {
        return INSTANCE;
    }
}
