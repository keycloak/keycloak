/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.migration.migrators;

import java.util.List;

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
            if ((adminConsoleClient != null) && !localeMapperAdded(adminConsoleClient)) {
                adminConsoleClient.addProtocolMapper(localeMapper);
            }
        }
    }

    private boolean localeMapperAdded(ClientModel adminConsoleClient) {
        return adminConsoleClient.getProtocolMapperByName("openid-connect", "locale") != null;
    }

}
