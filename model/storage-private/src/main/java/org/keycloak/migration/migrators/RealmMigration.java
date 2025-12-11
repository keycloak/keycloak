/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.connections.jpa.support.EntityManagers;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

import org.jboss.logging.Logger;

public abstract class RealmMigration implements Migration {

    private static final Logger LOG = Logger.getLogger(RealmMigration.class);

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realm -> {
            // empty out the persistence context for each realm
            EntityManagers.flush(session, true);
            // alternatively could be EntityManagers.runInBatch - but that also changes the
            // query modes, which I'm not sure is applicable here
            KeycloakContext context = session.getContext();
            RealmModel oldRealm = session.getContext().getRealm();
            RealmModel mutableRealm = session.realms().getRealmByName(realm.getName());
            try {
                context.setRealm(mutableRealm);
                migrateRealm(session, mutableRealm);
                LOG.infof("migrated realm %s to %s", realm.getName(), getVersion());
            } finally {
                context.setRealm(oldRealm);
            }
        });
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep,
            boolean skipUserDependent) {
        migrateRealm(session, realm);
    }

    public abstract void migrateRealm(KeycloakSession session, RealmModel realm);
}
