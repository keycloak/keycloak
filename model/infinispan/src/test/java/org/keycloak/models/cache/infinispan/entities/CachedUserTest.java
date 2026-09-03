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

package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ReadOnlyUserModelDelegate;
import org.keycloak.models.utils.StorageUnavailableUserModelDelegate;
import org.keycloak.models.utils.UserModelDelegate;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CachedUserTest {

    @Test
    public void shouldFindUnavailableStorageInDelegateChain() {
        UserModel unavailable = new StorageUnavailableUserModelDelegate(null, RuntimeException::new);
        UserModel wrapped = new ReadOnlyUserModelDelegate(new UserModelDelegate(unavailable));

        assertFalse(CachedUser.isStorageAvailable(wrapped));
    }

    @Test
    public void shouldAcceptAvailableDelegateChain() {
        assertTrue(CachedUser.isStorageAvailable(new ReadOnlyUserModelDelegate(new UserModelDelegate(null))));
    }
}
