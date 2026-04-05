package org.keycloak.protocol.saml;

/**
 * Exception to indicate a processing error in {@link ArtifactResolver}.
 *
 */
public class ArtifactResolverProcessingException extends Exception{

    public ArtifactResolverProcessingException(Exception e){
        super(e);
    }

    public ArtifactResolverProcessingException(String message) {
        super(message);
    }

    public ArtifactResolverProcessingException(String message, Exception e){
        super(message, e);
    }
}
