package org.keycloak.migration.migrators;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class MigrateTo26_8_2 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("26.8.2");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        migrateAdminApiClientScope(session, realm);
    }

    private void migrateAdminApiClientScope(KeycloakSession session, RealmModel realm) {
        MigrationProvider migrationProvider = session.getProvider(MigrationProvider.class);
        ClientScopeModel scope = migrationProvider.addOIDCAdminApiClientScope(realm);

        if (scope == null) {
            return;
        }

        for (String clientId : Set.of(Constants.ADMIN_CONSOLE_CLIENT_ID, Constants.ADMIN_CLI_CLIENT_ID)) {
            ClientModel client = realm.getClientByClientId(clientId);
            if (client != null) {
                addAdminApiClientScope(client, scope);
            }
        }

        if (Config.getAdminRealm().equals(realm.getName())) {
            realm.getClientsStream()
                    .filter(client -> client.isServiceAccountsEnabled()
                            && Boolean.parseBoolean(client.getAttribute(UserModel.IS_TEMP_ADMIN_ATTR_NAME)))
                    .forEach(client -> addAdminApiClientScope(client, scope));
        }
    }

    private void addAdminApiClientScope(ClientModel client, ClientScopeModel scope) {
        Map<String, ClientScopeModel> clientScopes = new HashMap<>(client.getClientScopes(true));
        clientScopes.putAll(client.getClientScopes(false));
        if (!clientScopes.containsKey(Constants.KEYCLOAK_ADMIN_API_SCOPE)) {
            client.addClientScope(scope, true);
        }
    }
}
