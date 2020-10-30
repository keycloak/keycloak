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
package org.keycloak.events.jpa;

import org.keycloak.Config.Scope;
import org.keycloak.common.ClientConnection;
import org.keycloak.connections.jpa.DefaultJpaConnectionProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionSpi;
import org.keycloak.connections.jpa.updater.JpaUpdaterProviderFactory;
import org.keycloak.connections.jpa.updater.JpaUpdaterSpi;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionSpi;
import org.keycloak.connections.jpa.updater.liquibase.lock.LiquibaseDBLockProviderFactory;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventStoreProviderFactory;
import org.keycloak.events.EventStoreSpi;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmSpi;
import org.keycloak.models.dblock.DBLockSpi;
import org.keycloak.models.jpa.JpaRealmProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.Spi;
import org.keycloak.services.DefaultKeycloakSession;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 * @author hmlnarik
 */
public class JpaAdminEventQueryTest {

    private static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
      .add(DBLockSpi.class)
      .add(EventStoreSpi.class)
      .add(JpaConnectionSpi.class)
      .add(JpaUpdaterSpi.class)
      .add(LiquibaseConnectionSpi.class)
      .add(RealmSpi.class)
      .build();

    private static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
      .add(DefaultJpaConnectionProviderFactory.class)
      .add(EventStoreProviderFactory.class)
      .add(JpaUpdaterProviderFactory.class)
      .add(JpaRealmProviderFactory.class)
      .add(LiquibaseConnectionProviderFactory.class)
      .add(LiquibaseDBLockProviderFactory.class)
      .build();

    private static final DefaultKeycloakSessionFactory factory = new DefaultKeycloakSessionFactory() {
        @Override
        protected boolean isEnabled(ProviderFactory factory, Scope scope) {
            return super.isEnabled(factory, scope) && ALLOWED_FACTORIES.stream().filter(c -> c.isAssignableFrom(factory.getClass())).findAny().isPresent();
        }

        @Override
        protected Map<Class<? extends Provider>, Map<String, ProviderFactory>> loadFactories(ProviderManager pm) {
            spis.removeIf(s -> ! ALLOWED_SPIS.contains(s.getClass()));
            return super.loadFactories(pm);
        }
    };
    static { factory.init(); }

    private final KeycloakSession session = new DefaultKeycloakSession(factory);
    private final EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

    @Before
    public void startTransaction() {
        session.getTransactionManager().begin();
    }

    @After
    public void stopTransaction() {
        session.getTransactionManager().rollback();
    }

    @Test
    public void testClear() {
        eventStore.clear();
    }

    @Test
    public void testQuery() {
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
