package org.keycloak.saml.processing.core.parsers.saml.metadata;

/**
 * @author mhajas
 */
public class SAMLArtifactResolutionServiceParser extends SAMLIndexedEndpointTypeParser {

    private static final SAMLArtifactResolutionServiceParser INSTANCE = new SAMLArtifactResolutionServiceParser();

    public SAMLArtifactResolutionServiceParser() {
        super(SAMLMetadataQNames.ARTIFACT_RESOLUTION_SERVICE);
    }

    public static SAMLArtifactResolutionServiceParser getInstance() {
        return INSTANCE;
    }
}
