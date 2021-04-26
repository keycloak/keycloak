/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model.events;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.models.RealmModel;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 *
 * @author hmlnarik
 */
@RequireProvider(EventStoreProvider.class)
public class AdminEventQueryTest extends KeycloakModelTest {


    @Test
    public void testClear() {
        inRolledBackTransaction(null, (session, t) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            eventStore.clear();
        });
    }

    @Test
    public void testQuery() {
        inRolledBackTransaction(null, (session, t) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            RealmModel realm = session.realms().createRealm("realm");
            ClientConnection cc = new DummyClientConnection();
            eventStore.onEvent(new EventBuilder(realm, null, cc).event(EventType.LOGIN).user("u1").getEvent());
            eventStore.onEvent(new EventBuilder(realm, null, cc).event(EventType.LOGIN).user("u2").getEvent());
            eventStore.onEvent(new EventBuilder(realm, null, cc).event(EventType.LOGIN).user("u3").getEvent());
            eventStore.onEvent(new EventBuilder(realm, null, cc).event(EventType.LOGIN).user("u4").getEvent());

            assertThat(eventStore.createQuery()
              .firstResult(2)
              .getResultStream()
              .collect(Collectors.counting()),
              is(2L)
            );
        });
    }

    private static class DummyClientConnection implements ClientConnection {

        @Override
        public String getRemoteAddr() {
            return "remoteAddr";
        }

        @Override
        public String getRemoteHost() {
            return "remoteHost";
        }

        @Override
        public int getRemotePort() {
            return -1;
        }

        @Override
        public String getLocalAddr() {
            return "localAddr";
        }

        @Override
        public int getLocalPort() {
            return -2;
        }
    }

}
