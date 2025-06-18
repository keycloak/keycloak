package org.keycloak.protocol.saml;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultSamlArtifactResolverFactory implements ArtifactResolverFactory {
    
    /** SAML 2 artifact type code (0x0004). */
    public static final byte[] TYPE_CODE = {0, 4};

    private DefaultSamlArtifactResolver artifactResolver;

    @Override
    public DefaultSamlArtifactResolver create(KeycloakSession session) {
        return artifactResolver;
    }

    @Override
    public void init(Config.Scope config) {
        // Nothing to initialize
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        artifactResolver = new DefaultSamlArtifactResolver();
    }

    @Override
    public void close() {
        // Nothing to close
    }

    @Override
    public String getId() {
        return "default";
    }

}
