package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrationTo1_2_0_CR1 {
    public static final ModelVersion VERSION = new ModelVersion("1.2.0.CR1");

    public void setupBrokerService(RealmModel realm) {
        ClientModel client = realm.getClientNameMap().get(Constants.BROKER_SERVICE_CLIENT_ID);
        if (client == null) {
            client = KeycloakModelUtils.createClient(realm, Constants.BROKER_SERVICE_CLIENT_ID);
            client.setEnabled(true);
            client.setName("${client_" + Constants.BROKER_SERVICE_CLIENT_ID + "}");
            client.setFullScopeAllowed(false);

            for (String role : Constants.BROKER_SERVICE_ROLES) {
                client.addRole(role).setDescription("${role_"+ role.toLowerCase().replaceAll("_", "-") +"}");
            }
        }
    }

    private void setupClientNames(RealmModel realm) {
        Map<String, ClientModel> clients = realm.getClientNameMap();

        setupClientName(clients, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        setupClientName(clients, Constants.ADMIN_CONSOLE_CLIENT_ID);
        setupClientName(clients, Constants.REALM_MANAGEMENT_CLIENT_ID);
    }

    private void setupClientName(Map<String, ClientModel> clients, String clientId) {
        ClientModel client = clients.get(clientId);
        if (client != null && client.getName() == null) client.setName("${client_" + clientId + "}");
    }

    public void migrate(KeycloakSession session) {
        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {
            setupBrokerService(realm);
            setupClientNames(realm);
        }

    }
}
