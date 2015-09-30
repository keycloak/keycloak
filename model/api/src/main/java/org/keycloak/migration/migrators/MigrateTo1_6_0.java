package org.keycloak.migration.migrators;

import java.util.List;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrateTo1_6_0 {

    public static final ModelVersion VERSION = new ModelVersion("1.6.0");

    public void migrate(KeycloakSession session) {
        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {

            for (RoleModel realmRole : realm.getRoles()) {
                realmRole.setScopeParamRequired(false);
            }
            for (ClientModel client : realm.getClients()) {
                for (RoleModel clientRole : client.getRoles()) {
                    clientRole.setScopeParamRequired(false);
                }
            }

            KeycloakModelUtils.setupOfflineTokens(realm);
        }

    }

}
