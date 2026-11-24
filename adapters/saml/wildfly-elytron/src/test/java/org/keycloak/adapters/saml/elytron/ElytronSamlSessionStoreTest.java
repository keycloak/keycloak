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

package org.keycloak.adapters.saml.elytron;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapperUpdater;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ElytronSamlSessionStoreTest {

    @Test
    public void logoutBySsoIdInvalidatesAllMatchingSessionsOnce() {
        SessionIdMapper mapper = new InMemorySessionIdMapper();
        mapper.map("shared", "alice", "first");
        mapper.map("shared", "alice", "second");
        mapper.map("other", "alice", "third");

        List<String> invalidated = new ArrayList<>();
        ElytronSamlSessionStore store = new ElytronSamlSessionStore(null, mapper, SessionIdMapperUpdater.DIRECT, null) {
            @Override
            protected void logoutSessionIds(List<String> sessionIds) {
                invalidated.addAll(sessionIds);
            }
        };
        store.logoutBySsoId(List.of("shared", "unknown", "shared"));

        assertEquals(2, invalidated.size());
        assertEquals(Set.of("first", "second"), Set.copyOf(invalidated));
        assertFalse(mapper.hasSession("first"));
        assertFalse(mapper.hasSession("second"));
        assertTrue(mapper.hasSession("third"));
    }
}
