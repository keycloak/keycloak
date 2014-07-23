package org.keycloak.federation.ldap;

import org.keycloak.Config;
import org.keycloak.models.FederationProvider;
import org.keycloak.models.FederationProviderFactory;
import org.keycloak.models.FederationProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LDAPFederationProviderFactory implements FederationProviderFactory {
    public static final String PROVIDER_NAME = "ldap";
    PartitionManagerRegistry registry;

    @Override
    public FederationProvider create(KeycloakSession session) {
        throw new IllegalAccessError("Illegal to call this method");
    }

    @Override
    public FederationProvider getInstance(KeycloakSession session, FederationProviderModel model) {
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
