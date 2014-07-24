package org.keycloak.federation.ldap;

import org.keycloak.Config;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.KeycloakSession;
import org.picketlink.idm.PartitionManager;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LDAPFederationProviderFactory implements UserFederationProviderFactory {
    public static final String PROVIDER_NAME = "ldap";
    PartitionManagerRegistry registry;

    @Override
    public UserFederationProvider create(KeycloakSession session) {
        throw new IllegalAccessError("Illegal to call this method");
    }

    @Override
    public UserFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model) {
        PartitionManager partition = registry.getPartitionManager(model);
        return new LDAPFederationProvider(session, model, partition);
    }

    @Override
    public void init(Config.Scope config) {
        registry = new PartitionManagerRegistry();
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }
}
