package org.keycloak.federation.ldap.mappers;

import org.keycloak.Config;
import org.keycloak.mappers.UserFederationMapperFactory;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractLDAPFederationMapperFactory implements UserFederationMapperFactory {

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
