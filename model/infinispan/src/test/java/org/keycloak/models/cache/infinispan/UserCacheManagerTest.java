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
package org.keycloak.models.cache.infinispan;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserCacheManagerTest {

    private static final String USER_ID = "user-id";
    private static final String REALM_ID = "realm-id";

    private final UserCacheManager manager = new UserCacheManager(null, null);

    @Test
    public void skipsUsernameLookupKeyWhenUsernameIsMissing() {
        Set<String> invalidations = invalidationsFor(null, null);

        assertEquals(Set.of(USER_ID), invalidations);
        assertFalse(invalidations.contains(REALM_ID + ".username.null"));
    }

    @Test
    public void invalidatesUsernameAndEmailLookupsWhenPresent() {
        Set<String> invalidations = invalidationsFor("jdoe", "jdoe@example.com");

        assertEquals(Set.of(USER_ID, REALM_ID + ".username.jdoe", REALM_ID + ".email.jdoe@example.com"), invalidations);
    }

    @Test
    public void skipsEmailLookupKeyWhenEmailIsMissing() {
        assertTrue(invalidationsFor("jdoe", null).contains(REALM_ID + ".username.jdoe"));
    }

    private Set<String> invalidationsFor(String username, String email) {
        Set<String> invalidations = new HashSet<>();
        manager.userUpdatedInvalidations(USER_ID, username, email, REALM_ID, invalidations);
        return invalidations;
    }
}