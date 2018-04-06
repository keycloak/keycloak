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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MigrateTo1_9_2 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("1.9.2");

    public ModelVersion getVersion() {
        return VERSION;
    }

    public void migrate(KeycloakSession session) {
        for (RealmModel realm : session.realms().getRealms()) {
            migrateRealm(realm);
        }
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealm(realm);
    }

    protected void migrateRealm(RealmModel realm) {
        if (realm.getBrowserSecurityHeaders() != null) {

            Map<String, String> browserSecurityHeaders = new HashMap<>(realm.getBrowserSecurityHeaders());
            browserSecurityHeaders.put("xContentTypeOptions", "nosniff");

            realm.setBrowserSecurityHeaders(Collections.unmodifiableMap(browserSecurityHeaders));
        }
    }

}
