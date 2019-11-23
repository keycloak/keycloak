/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import org.jboss.logging.Logger;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;


/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrateTo9_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("9.0.0");

    private static final Logger LOG = Logger.getLogger(MigrateTo9_0_0.class);

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealms().stream().forEach(realm -> addAccountConsoleClient(realm));
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealmCommon(realm);
    }

    protected void migrateRealmCommon(RealmModel realm) {
        addAccountConsoleClient(realm);
    }

    protected void addAccountConsoleClient(RealmModel realm) {
        if (realm.getClientByClientId(Constants.ACCOUNT_CONSOLE_CLIENT_ID) == null) {
            ClientModel client = KeycloakModelUtils.createClient(realm, Constants.ACCOUNT_CONSOLE_CLIENT_ID);
            client.setName("${client_" + Constants.ACCOUNT_CONSOLE_CLIENT_ID + "}");
            client.setEnabled(true);
            client.setFullScopeAllowed(false);
            client.setPublicClient(true);
            client.setDirectAccessGrantsEnabled(false);

            client.setRootUrl(Constants.AUTH_BASE_URL_PROP);
            String baseUrl = "/realms/" + realm.getName() + "/account/";
            client.setBaseUrl(baseUrl);
            client.addRedirectUri(baseUrl + "*");

            client.setProtocol("openid-connect");

            client.addScopeMapping(realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).getRole(AccountRoles.MANAGE_ACCOUNT));

            ProtocolMapperModel audienceMapper = new ProtocolMapperModel();
            audienceMapper.setName("audience resolve");
            audienceMapper.setProtocol("openid-connect");
            audienceMapper.setProtocolMapper("oidc-audience-resolve-mapper");

            client.addProtocolMapper(audienceMapper);
        }
    }
}
