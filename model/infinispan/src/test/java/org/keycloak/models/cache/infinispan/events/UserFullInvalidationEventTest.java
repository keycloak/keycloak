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
package org.keycloak.models.cache.infinispan.events;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;
import org.keycloak.models.cache.infinispan.UserCacheManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class UserFullInvalidationEventTest {

    private static final String USER_ID = "user-id";
    private static final String REALM_ID = "realm-id";
    private static final String BOGUS_USERNAME_KEY = REALM_ID + ".username.null";

    @Test
    public void accountWithoutUsernameCanBeInvalidated() {
        UserFullInvalidationEvent event = UserFullInvalidationEvent.create(USER_ID, null, null, REALM_ID, false, Stream.empty());

        Set<String> invalidations = invalidate(event);

        assertTrue("the user must still be invalidated by id", invalidations.contains(USER_ID));
        assertFalse("no lookup key may be built for a missing username", invalidations.contains(BOGUS_USERNAME_KEY));
    }

    @Test
    public void accountWithUsernameStillInvalidatesUsernameLookup() {
        UserFullInvalidationEvent event = UserFullInvalidationEvent.create(USER_ID, "jdoe", null, REALM_ID, false, Stream.empty());

        Set<String> invalidations = invalidate(event);

        assertTrue(invalidations.contains(USER_ID));
        assertTrue(invalidations.contains(REALM_ID + ".username.jdoe"));
    }

    @Test
    public void realmIdIsStillMandatory() {
        assertThrows(NullPointerException.class,
                () -> UserFullInvalidationEvent.create(USER_ID, null, null, null, false, Stream.empty()));
    }

    private Set<String> invalidate(UserFullInvalidationEvent event) {
        Set<String> invalidations = new HashSet<>();
        // the caches are not touched while collecting the invalidation keys
        event.addInvalidations(new UserCacheManager(null, null), invalidations);
        return invalidations;
    }
}