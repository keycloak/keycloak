/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.common.util.Base64Url;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Records existing AES key sizes so saving a legacy provider preserves its key.
 */
public class MigrateTo26_8_3 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("26.8.3");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        realm.getComponentsStream(realm.getId(), "org.keycloak.keys.KeyProvider")
                .filter(c -> "aes-generated".equals(c.getProviderId()))
                .filter(c -> c.getConfig().getFirst("secretSize") == null)
                .filter(c -> c.getConfig().getFirst("secret") != null)
                .forEach(c -> {
                    // Preserve existing keys when the admin console saves the provider with the displayed size.
                    int size = Base64Url.decode(c.getConfig().getFirst("secret")).length;
                    c.getConfig().putSingle("secretSize", Integer.toString(size));
                    realm.updateComponent(c);
                });
    }
}
