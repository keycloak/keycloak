package org.keycloak.protocol.saml.clientregistration;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.ClientRegistrationProviderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EntityDescriptorClientRegistrationProviderFactory implements ClientRegistrationProviderFactory {

    public static final String ID = "saml2-entity-descriptor";

    @Override
    public ClientRegistrationProvider create(KeycloakSession session) {
        return new EntityDescriptorClientRegistrationProvider(session);
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
        return ID;
    }

}
