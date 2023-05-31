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

import org.jboss.logging.Logger;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrateTo22_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("22.0.0");

    public static final String HTTP_CHALLENGE_FLOW = "http challenge";

    private static final Logger LOG = Logger.getLogger(MigrateTo22_0_0.class);

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(this::removeHttpChallengeFlow);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        removeHttpChallengeFlow(realm);
    }

    private void removeHttpChallengeFlow(RealmModel realm) {
        AuthenticationFlowModel httpChallenge = realm.getFlowByAlias(HTTP_CHALLENGE_FLOW);
        if (httpChallenge == null) return;

        try {
            realm.removeAuthenticationFlow(httpChallenge);

            // Builtin "Http challenge" flow had subflow of this name, which should be removed as well
            AuthenticationFlowModel subflow = realm.getFlowByAlias("Authentication Options");
            if (subflow != null && subflow.isBuiltIn()) {
                realm.removeAuthenticationFlow(subflow);
            }
            LOG.debugf("Removed '%s' authentication flow in realm '%s'", HTTP_CHALLENGE_FLOW, realm.getName());
        } catch (ModelException me) {
            if (me.getMessage().endsWith("it is currently in use")) {
                // This is the theoretic case when this flow is bind as any realm flow (EG. browser) or as first-broker-login or post-broker-login of some IDP.
                // Which is very unlikely and doesn't have any real use-case, but can happen in theory.
                // It doesn't affect the case when the flow is in use by some client (Authentication flow binding override)
                LOG.warnf("Authentication flow '%s' is in use in realm '%s' and cannot be removed. Please note that authenticators from this flow may not be available anymore, unless you deploy keycloak-openshift-extension to your server",
                        HTTP_CHALLENGE_FLOW, realm.getName());
            } else {
                throw me;
            }
        }
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
