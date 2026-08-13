/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model.clientscope;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.testsuite.model.util.TransactionController;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author hmlnarik
 */
@RequireProvider(RealmProvider.class)
@RequireProvider(ClientProvider.class)
@RequireProvider(ClientScopeProvider.class)
@RequireProvider(RoleProvider.class)
public class ClientScopeModelTest extends KeycloakModelTest {

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "realm");
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testClientScopes() {
        List<String> clientScopes = new LinkedList<>();
        withRealm(realmId, (session, realm) -> {
            ClientModel client = session.clients().addClient(realm, "myClientId");

            ClientScopeModel clientScope1 = session.clientScopes().addClientScope(realm, "myClientScope1");
            clientScopes.add(clientScope1.getId());
            ClientScopeModel clientScope2 = session.clientScopes().addClientScope(realm, "myClientScope2");
            clientScopes.add(clientScope2.getId());


            client.addClientScope(clientScope1, true);
            client.addClientScope(clientScope2, false);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            List<String> actualClientScopes = session.clientScopes().getClientScopesStream(realm).map(ClientScopeModel::getId).collect(Collectors.toList());
            assertThat(actualClientScopes, containsInAnyOrder(clientScopes.toArray()));

            ClientScopeModel clientScopeById = session.clientScopes().getClientScopeById(realm, clientScopes.get(0));
            assertThat(clientScopeById.getId(), is(clientScopes.get(0)));

            session.clientScopes().removeClientScopes(realm);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            List<ClientScopeModel> actualClientScopes = session.clientScopes().getClientScopesStream(realm).collect(Collectors.toList());
            assertThat(actualClientScopes, empty());

            return null;
        });
    }

    @Test
    @RequireProvider(value=ClientScopeProvider.class, only="jpa")
    @RequireProvider(value=CacheRealmProvider.class)
    public void testClientScopesCaching() {
        List<String> clientScopes = new LinkedList<>();
        withRealm(realmId, (session, realm) -> {
            ClientScopeModel clientScope = session.clientScopes().addClientScope(realm, "myClientScopeForCaching");
            clientScopes.add(clientScope.getId());

            assertionsForClientScopesCaching(clientScopes, session, realm);
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            assertionsForClientScopesCaching(clientScopes, session, realm);
            return null;
        });

    }

    @Test
    @RequireProvider(value=ClientScopeProvider.class, only="jpa")
    public void testClientScopesByProtocolForUpdateSerializesRealmChanges() throws Exception {
        String protocol = "client-scope-lock-test";
        String clientScopeName = "client-scope-created-under-lock";
        KeycloakSessionFactory sessionFactory = getFactory();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try (TransactionController firstTransaction = new TransactionController(sessionFactory)) {
            boolean firstTransactionCompleted = false;
            firstTransaction.begin();
            try {
                firstTransaction.runStep(session -> {
                    RealmModel realm = session.realms().getRealm(realmId);
                    session.getContext().setRealm(realm);
                    assertThat(session.clientScopes().getClientScopesByProtocolForUpdate(realm, protocol)
                            .collect(Collectors.toList()), empty());

                    ClientScopeModel clientScope = session.clientScopes().addClientScope(realm, clientScopeName);
                    clientScope.setProtocol(protocol);
                    return null;
                });

                CountDownLatch secondTransactionStarted = new CountDownLatch(1);
                Future<List<String>> secondTransaction = executor.submit(() ->
                        KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, session -> {
                            RealmModel realm = session.realms().getRealm(realmId);
                            session.getContext().setRealm(realm);
                            secondTransactionStarted.countDown();
                            return session.clientScopes().getClientScopesByProtocolForUpdate(realm, protocol)
                                    .map(ClientScopeModel::getName)
                                    .collect(Collectors.toList());
                        }));

                assertTrue(secondTransactionStarted.await(5, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> secondTransaction.get(200, TimeUnit.MILLISECONDS));

                firstTransaction.commit();
                firstTransactionCompleted = true;
                assertThat(secondTransaction.get(5, TimeUnit.SECONDS), containsInAnyOrder(clientScopeName));
            } finally {
                if (!firstTransactionCompleted) {
                    firstTransaction.rollback();
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static void assertionsForClientScopesCaching(List<String> clientScopes, KeycloakSession session, RealmModel realm) {
        assertThat(clientScopes, Matchers.containsInAnyOrder(realm.getClientScopesStream()
                .map(ClientScopeModel::getId).toArray(String[]::new)));

        assertThat(clientScopes, Matchers.containsInAnyOrder(session.clientScopes().getClientScopesStream(realm)
                .map(ClientScopeModel::getId).toArray(String[]::new)));
    }

}
