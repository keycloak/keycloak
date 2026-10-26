package org.keycloak.migration.migrators;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Migrates realms for 26.6.8:
 * <ul>
 *   <li>Admin API client scope for admin clients (scope/audience checks)</li>
 *   <li>Client registration policies that block {@code defaultRoles} and enable
 *       {@code Full Scope Disabled} for authenticated Dynamic Client Registration</li>
 * </ul>
 */
public class MigrateTo26_6_8 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("26.6.8");

    private static final String CLIENT_REGISTRATION_POLICY =
            "org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy";
    private static final String DEFAULT_ROLES_PROVIDER_ID = "default-roles";
    private static final String SCOPE_PROVIDER_ID = "scope";
    private static final String ANONYMOUS = "anonymous";
    private static final String AUTHENTICATED = "authenticated";

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        migrateAdminApiClientScope(session, realm);
        addPolicyIfMissing(realm, "Default Roles Disabled", DEFAULT_ROLES_PROVIDER_ID, ANONYMOUS);
        addPolicyIfMissing(realm, "Default Roles Disabled", DEFAULT_ROLES_PROVIDER_ID, AUTHENTICATED);
        addPolicyIfMissing(realm, "Full Scope Disabled", SCOPE_PROVIDER_ID, AUTHENTICATED);
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

    private static void addPolicyIfMissing(RealmModel realm, String name, String providerId, String policyType) {
        boolean exists = realm.getComponentsStream(realm.getId(), CLIENT_REGISTRATION_POLICY)
                .anyMatch(c -> Objects.equals(providerId, c.getProviderId())
                        && Objects.equals(policyType, c.getSubType()));
        if (exists) {
            return;
        }

        ComponentModel model = new ComponentModel();
        model.setName(name);
        model.setParentId(realm.getId());
        model.setProviderId(providerId);
        model.setProviderType(CLIENT_REGISTRATION_POLICY);
        model.setSubType(policyType);
        realm.addComponentModel(model);
    }
}
