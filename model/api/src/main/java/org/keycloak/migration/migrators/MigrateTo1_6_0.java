package org.keycloak.migration.migrators;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrateTo1_6_0 {

    public static final ModelVersion VERSION = new ModelVersion("1.6.0");

    public void migrate(KeycloakSession session) {
        MigrationProvider provider = session.getProvider(MigrationProvider.class);

        List<ProtocolMapperModel> builtinMappers = provider.getBuiltinMappers("openid-connect");
        ProtocolMapperModel localeMapper = null;
        for (ProtocolMapperModel m : builtinMappers) {
            if (m.getName().equals("locale")) {
                localeMapper = m;
            }
        }

        if (localeMapper == null) {
            throw new RuntimeException("Can't find default locale mapper");
        }

        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {
            if (realm.getRole(Constants.OFFLINE_ACCESS_ROLE) == null) {
                for (RoleModel realmRole : realm.getRoles()) {
                    realmRole.setScopeParamRequired(false);
                }
                for (ClientModel client : realm.getClients()) {
                    for (RoleModel clientRole : client.getRoles()) {
                        clientRole.setScopeParamRequired(false);
                    }
                }

                KeycloakModelUtils.setupOfflineTokens(realm);
                RoleModel role = realm.getRole(Constants.OFFLINE_ACCESS_ROLE);

                // Check if possible to avoid iterating over users
                for (UserModel user : session.userStorage().getUsers(realm, true)) {
                    user.grantRole(role);
                }
            }

            ClientModel adminConsoleClient = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
            if (adminConsoleClient != null) {
                adminConsoleClient.addProtocolMapper(localeMapper);
            }
        }
    }

}
