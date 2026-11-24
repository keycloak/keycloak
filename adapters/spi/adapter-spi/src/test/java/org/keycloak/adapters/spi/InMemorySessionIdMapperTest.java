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

package org.keycloak.adapters.spi;

import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InMemorySessionIdMapperTest {

    private final SessionIdMapper mapper = new InMemorySessionIdMapper();

    @Test
    public void removingOneSessionPreservesOtherSessionsForTheSameIndex() {
        mapper.map("index", "alice", "first");
        mapper.map("index", "alice", "second");
        mapper.map("other-index", "alice", "third");

        Set<String> sessions = mapper.getSessionsFromSSO("index");
        mapper.removeSession("first");
        mapper.removeSession("first");

        assertEquals(Set.of("first", "second"), sessions);
        assertEquals(Set.of("second"), mapper.getSessionsFromSSO("index"));
        assertEquals(Set.of("second", "third"), mapper.getUserSessions("alice"));
        mapper.removeSession("second");
        assertEquals(Set.of(), mapper.getSessionsFromSSO("index"));
        assertEquals(Set.of("third"), mapper.getSessionsFromSSO("other-index"));
    }

    @Test
    public void tracksLocalSessionsWithoutASubjectOrSessionIndex() {
        mapper.map(null, null, "local");
        assertTrue(mapper.hasSession("local"));
        mapper.removeSession("local");
        assertFalse(mapper.hasSession("local"));
    }

    @Test
    public void remappingReplacesPreviousIndexAndSubject() {
        mapper.map("index", "alice", "local");
        mapper.map(null, "bob", "local");

        assertEquals(Set.of(), mapper.getSessionsFromSSO("index"));
        assertNull(mapper.getUserSessions("alice"));
        assertEquals(Set.of("local"), mapper.getUserSessions("bob"));

        mapper.removeSession("local");
        assertFalse(mapper.hasSession("local"));
        assertNull(mapper.getUserSessions("bob"));
    }
}
