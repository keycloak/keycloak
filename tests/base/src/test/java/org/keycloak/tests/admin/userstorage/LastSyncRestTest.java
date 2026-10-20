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

package org.keycloak.tests.admin.userstorage;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.SynchronizationResultRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.federation.DummyUserFederationProviderFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the per-mode lastSync timestamps recorded by the user-storage sync are exposed through the admin
 * API, both in the component representation and in a realm partial export. Since the timestamps were split into
 * lastSync_FULL / lastSync_CHANGED they were no longer returned, leaving no supported way to find out when a
 * provider last synced.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/53989">Issue 53989</a>
 */
@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class LastSyncRestTest {

    private static final String LAST_SYNC = UserStorageProviderModel.LAST_SYNC;
    private static final String LAST_SYNC_FULL = LAST_SYNC + "_" + UserStorageProviderModel.SyncMode.FULL.name();
    private static final String LAST_SYNC_CHANGED = LAST_SYNC + "_" + UserStorageProviderModel.SyncMode.CHANGED.name();

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @Test
    public void testLastSyncExposedAfterSync() {
        String componentId = createDummyUserStorage();

        // A provider that has not synced yet must not report any lastSync timestamps
        ComponentRepresentation before = managedRealm.admin().components().component(componentId).toRepresentation();
        Assertions.assertFalse(before.getConfig().containsKey(LAST_SYNC_FULL),
                "A provider that never synced must not report a full-sync timestamp");
        Assertions.assertFalse(before.getConfig().containsKey(LAST_SYNC_CHANGED),
                "A provider that never synced must not report a changed-sync timestamp");

        SynchronizationResultRepresentation fullSync = managedRealm.admin().userStorage().syncUsers(componentId, "triggerFullSync");
        Assertions.assertFalse(fullSync.isIgnored(), "The full sync should have run, not been ignored");

        ComponentRepresentation afterFullSync = managedRealm.admin().components().component(componentId).toRepresentation();
        Assertions.assertTrue(afterFullSync.getConfig().containsKey(LAST_SYNC_FULL),
                "The admin API must expose lastSync_FULL after a full sync, otherwise there is no supported way to "
                        + "find out when a provider last synced (issue #53989)");
        int fullLastSync = Integer.parseInt(afterFullSync.getConfig().getFirst(LAST_SYNC_FULL));
        Assertions.assertTrue(fullLastSync > 0, "The exposed lastSync_FULL must be a timestamp");

        SynchronizationResultRepresentation changedSync = managedRealm.admin().userStorage().syncUsers(componentId, "triggerChangedUsersSync");
        Assertions.assertFalse(changedSync.isIgnored(), "The changed sync should have run, not been ignored");

        ComponentRepresentation afterChangedSync = managedRealm.admin().components().component(componentId).toRepresentation();
        Assertions.assertTrue(afterChangedSync.getConfig().containsKey(LAST_SYNC_CHANGED),
                "The admin API must expose lastSync_CHANGED after a changed sync (issue #53989)");
        Assertions.assertEquals(fullLastSync, Integer.parseInt(afterChangedSync.getConfig().getFirst(LAST_SYNC_FULL)),
                "A changed sync must not move the full-sync timestamp");
    }

    @Test
    public void testLastSyncExposedInRealmPartialExport() {
        String componentId = createDummyUserStorage();

        managedRealm.admin().userStorage().syncUsers(componentId, "triggerFullSync");

        RealmRepresentation export = managedRealm.admin().partialExport(null, null);
        List<ComponentExportRepresentation> providers = export.getComponents().get(UserStorageProvider.class.getName());
        Assertions.assertNotNull(providers, "The partial export must contain the user storage provider");
        ComponentExportRepresentation exported = providers.stream()
                .filter(c -> componentId.equals(c.getId())).findFirst()
                .orElseThrow(() -> new AssertionError("The partial export must contain the synced user storage provider"));

        Assertions.assertTrue(exported.getConfig().containsKey(LAST_SYNC_FULL),
                "The realm partial export must expose lastSync_FULL so upgraded installations can be inspected without "
                        + "querying the database (issue #53989)");
    }

    private String createDummyUserStorage() {
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("last-sync-dummy");
        provider.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
        provider.setProviderType(UserStorageProvider.class.getName());
        provider.setParentId(managedRealm.getId());
        provider.setConfig(new MultivaluedHashMap<>());
        provider.getConfig().putSingle("enabled", "true");

        Response response = managedRealm.admin().components().add(provider);
        Assertions.assertEquals(201, response.getStatus());
        response.close();
        return ApiUtil.getCreatedId(response);
    }
}
