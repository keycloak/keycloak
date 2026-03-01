/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrateTo22_0_0 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("22.0.0");

    public static final String HTTP_CHALLENGE_FLOW = "http challenge";

    private static final Logger LOG = Logger.getLogger(MigrateTo22_0_0.class);

    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        removeHttpChallengeFlow(session, realm);
        //login, account, email themes are handled by JpaUpdate22_0_0_RemoveRhssoThemes
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        removeHttpChallengeFlow(session, realm);
        updateLoginTheme(realm);
        updateAccountTheme(realm);
        updateEmailTheme(realm);
        updateClientAttributes(realm);
    }

    private void removeHttpChallengeFlow(KeycloakSession session, RealmModel realm) {
        AuthenticationFlowModel httpChallenge = realm.getFlowByAlias(HTTP_CHALLENGE_FLOW);
        if (httpChallenge == null) return;

        try {
            KeycloakModelUtils.deepDeleteAuthenticationFlow(session, realm, httpChallenge, () -> {}, () -> {});
            LOG.debugf("Removed '%s' authentication flow in realm '%s'", HTTP_CHALLENGE_FLOW, realm.getName());
        } catch (ModelException me) {
            LOG.errorf("Authentication flow '%s' is in use in realm '%s' and cannot be removed. Please update your deployment to avoid using this flow before migration to latest Keycloak",
                    HTTP_CHALLENGE_FLOW, realm.getName());
            throw me;
        }
    }

    private void updateAccountTheme(RealmModel realm) {
        String accountTheme = realm.getAccountTheme();
        if ("keycloak".equals(accountTheme) || "rh-sso".equals(accountTheme) || "rh-sso.v2".equals(accountTheme)) {
            realm.setAccountTheme("keycloak.v2");
        }
    }

    private void updateEmailTheme(RealmModel realm) {
        String emailTheme = realm.getEmailTheme();
        if ("rh-sso".equals(emailTheme)) {
            realm.setEmailTheme(null);
        }
    }

    private void updateLoginTheme(RealmModel realm) {
        String loginTheme = realm.getLoginTheme();
        if ("rh-sso".equals(loginTheme)) {
            realm.setLoginTheme(null);
        }
    }

    private void updateClientAttributes(RealmModel realm) {
        realm.getClientsStream()
                .filter(client -> {
                    String clientLoginTheme = client.getAttribute("login_theme");
                    return clientLoginTheme != null && clientLoginTheme.equals("rh-sso");
                })
                .forEach(client -> client.setAttribute("login_theme", null));
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
