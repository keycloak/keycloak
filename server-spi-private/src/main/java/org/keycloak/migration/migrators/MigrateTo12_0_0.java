/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.migration.migrators;

import java.util.Objects;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;

public class MigrateTo12_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("12.0.0");

    private static void addDeleteAccountAction(RealmModel realm) {
        RequiredActionProviderModel deleteAccount = new RequiredActionProviderModel();
        deleteAccount.setEnabled(false);
        deleteAccount.setAlias("delete_account");
        deleteAccount.setName("Delete Account");
        deleteAccount.setProviderId("delete_account");
        deleteAccount.setDefaultAction(false);
        deleteAccount.setPriority(60);
        realm.addRequiredActionProvider(deleteAccount);
    }

    @Override
    public void migrate(KeycloakSession session) {
        session.realms()
          .getRealmsStream()
          .map(realm -> realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID))
          .filter(Objects::nonNull)
          .filter(client -> Objects.isNull(client.getRole(AccountRoles.DELETE_ACCOUNT)))
          .forEach(client -> client.addRole(AccountRoles.DELETE_ACCOUNT)
          .setDescription("${role_" + AccountRoles.DELETE_ACCOUNT + "}"));

        session.realms().getRealmsStream().filter(realm -> Objects.isNull(realm.getRequiredActionProviderByAlias("delete_account"))).forEach(MigrateTo12_0_0::addDeleteAccountAction);
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
