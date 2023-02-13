package org.keycloak.testsuite.authentication;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.saml.ArtifactResolver;
import org.keycloak.protocol.saml.ArtifactResolverFactory;
import org.keycloak.protocol.saml.util.ArtifactBindingUtils;

/**
 * This ArtifactResolver should be used only for testing purposes.
 */
public class CustomTestingSamlArtifactResolverFactory implements ArtifactResolverFactory {

    public  static final byte[] TYPE_CODE = {0, 5};
    public static final CustomTestingSamlArtifactResolver resolver = new CustomTestingSamlArtifactResolver();
    
    @Override
    public ArtifactResolver create(KeycloakSession session) {
        return resolver;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return ArtifactBindingUtils.byteArrayToResolverProviderId(TYPE_CODE);
    }
}
