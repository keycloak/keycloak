package org.keycloak.protocol.saml;

/**
 * Exception to indicate a configuration error in {@link ArtifactResolver}.
 *
 */
public class ArtifactResolverConfigException extends Exception {

    public ArtifactResolverConfigException(Exception e){
        super(e);
    }
}
