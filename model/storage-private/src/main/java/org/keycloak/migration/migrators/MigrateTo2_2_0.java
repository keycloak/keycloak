/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderQuery;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.RealmRepresentation;

import org.jboss.logging.Logger;

public class MigrateTo2_2_0 implements Migration {
    public static final ModelVersion VERSION = new ModelVersion("2.2.0");

    private static final Logger LOG = Logger.getLogger(MigrateTo2_2_0.class);

    public ModelVersion getVersion() {
        return VERSION;
    }

    public void migrate(KeycloakSession session) {
        RealmModel sessionRealm = session.getContext().getRealm();
        session.realms().getRealmsStream().forEach(realm -> {
            session.getContext().setRealm(realm);
            addIdentityProviderAuthenticator(session, realm);
        });
        session.getContext().setRealm(sessionRealm);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        RealmModel sessionRealm = session.getContext().getRealm();
        session.getContext().setRealm(realm);
        addIdentityProviderAuthenticator(session, realm);
        session.getContext().setRealm(sessionRealm);
    }

    private void addIdentityProviderAuthenticator(KeycloakSession session, RealmModel realm) {
        String defaultProvider = session.identityProviders()
                .getAllStream(IdentityProviderQuery.userAuthentication().with(IdentityProviderModel.ENABLED, "true").with(IdentityProviderModel.AUTHENTICATE_BY_DEFAULT, "true"), 0, 1)
                .map(IdentityProviderModel::getAlias)
                .findFirst().orElse(null);
        DefaultAuthenticationFlows.addIdentityProviderAuthenticator(realm, defaultProvider);
    }

}
