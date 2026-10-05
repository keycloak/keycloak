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

package org.keycloak.storage;

import org.keycloak.storage.UserStorageProviderModel.SyncMode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link UserStorageProviderModel}.
 */
public class UserStorageProviderModelTest {

    @Test
    public void testLegacyLastSyncFallback() {
        UserStorageProviderModel model = new UserStorageProviderModel();
        model.getConfig().putSingle(UserStorageProviderModel.LAST_SYNC, "1600000000");

        // Verify fallback for both CHANGED and FULL sync modes
        assertEquals(1600000000, model.getLastSync(SyncMode.CHANGED));
        assertEquals(1600000000, model.getLastSync(SyncMode.FULL));

        // Verify mode-specific timestamp takes precedence over legacy fallback
        model.setLastSync(1700000000, SyncMode.CHANGED);
        assertEquals(1700000000, model.getLastSync(SyncMode.CHANGED));
        assertEquals(1600000000, model.getLastSync(SyncMode.FULL));
    }

    @Test
    public void testMissingLastSyncReturnsZero() {
        UserStorageProviderModel model = new UserStorageProviderModel();

        assertEquals(0, model.getLastSync(SyncMode.CHANGED));
        assertEquals(0, model.getLastSync(SyncMode.FULL));
    }

    @Test
    public void testNullSyncModeUsesLegacyLastSync() {
        UserStorageProviderModel model = new UserStorageProviderModel();
        model.getConfig().putSingle(UserStorageProviderModel.LAST_SYNC, "1600000000");

        assertEquals(1600000000, model.getLastSync(null));
    }
}
