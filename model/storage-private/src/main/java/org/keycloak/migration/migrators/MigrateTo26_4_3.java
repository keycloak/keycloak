package org.keycloak.migration.migrators;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Policy.FilterOption;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class MigrateTo26_4_3 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("26.4.3");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }


    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        ClientModel client = realm.getAdminPermissionsClient();

        if (client == null) {
            return;
        }

        AuthorizationProvider authorizationProvider = session.getProvider(AuthorizationProvider.class);
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(client);

        if (resourceServer == null) {
            return;
        }

        ScopeStore scopeStore = storeFactory.getScopeStore();
        Scope resetPassword = scopeStore.findByName(resourceServer, AdminPermissionsSchema.RESET_PASSWORD);

        if (resetPassword == null) {
            resetPassword = scopeStore.create(resourceServer, AdminPermissionsSchema.RESET_PASSWORD);
        }

        ResourceStore resourceStore = storeFactory.getResourceStore();
        String userResourceType = AdminPermissionsSchema.USERS.getType();
        Resource resourceTypeResource = resourceStore.findByName(resourceServer, userResourceType);
        Set<Scope> newScopes = new HashSet<>(resourceTypeResource.getScopes());

        newScopes.add(resetPassword);

        resourceTypeResource.updateScopes(newScopes);

        for (Policy policy : storeFactory.getPolicyStore().find(resourceServer, Map.of(FilterOption.CONFIG, new String[]{"defaultResourceType", userResourceType}), -1, -1)) {
            for (Resource resource : policy.getResources()) {
                resource.updateScopes(newScopes);
            }
        }
    }
}
