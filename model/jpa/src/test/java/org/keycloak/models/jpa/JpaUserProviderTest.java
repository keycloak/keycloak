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
package org.keycloak.models.jpa;

import org.junit.Test;
import org.keycloak.models.ModelException;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class JpaUserProviderTest {

    private final JpaUserProvider provider = new JpaUserProvider(null, null);

    @Test
    public void rejectsMissingUsername() {
        assertThrows(ModelException.class, () -> provider.addUser(null, null, null, true, true));
    }

    @Test
    public void rejectsEmptyUsername() {
        assertThrows(ModelException.class, () -> provider.addUser(null, null, "", true, true));
    }

    @Test
    public void rejectsWhitespaceUsername() {
        assertThrows(ModelException.class, () -> provider.addUser(null, null, "   ", true, true));
    }

    @Test
    public void lookupByMissingUsernameFindsNobody() {
        assertNull(provider.getUserByUsername(null, null));
    }
}