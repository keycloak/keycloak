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

import java.util.UUID;
import org.jboss.logging.Logger;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

public class MigrateTo9_0_4 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("9.0.4");

    private static final Logger LOG = Logger.getLogger(MigrateTo9_0_4.class);

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(this::checkAuthConfigNullAlias);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
    }

    protected void checkAuthConfigNullAlias(RealmModel realm) {
        realm.getAuthenticatorConfigsStream()
                .filter(this::hasNullAlias)
                .forEach((config) -> this.setRandomAlias(realm, config));
    }

    private boolean hasNullAlias(AuthenticatorConfigModel config) {
        return config.getAlias() == null;
    }

    private void setRandomAlias(RealmModel realm, AuthenticatorConfigModel config) {
        config.setAlias(UUID.randomUUID().toString());
        realm.updateAuthenticatorConfig(config);
        LOG.debugf("Generated random alias for authenticator config with id %s.", config.getId());
    }

}
