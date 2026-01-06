package org.keycloak.models.mapper;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class OIDCClientModelMapperFactory implements ClientModelMapperFactory {
    @Override
    public ClientModelMapper create(KeycloakSession session) {
        return new OIDCClientModelMapper(session);
    }

    @Override
    public String getId() {
        return OIDCClientRepresentation.PROTOCOL;
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
}
