/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.model;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;

/**
 * Test for the CRUD scenarios when the operation is called on the object, which is owned by different realm
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OwnerReplacementTest extends AbstractKeycloakTest {

    private static String testRealmId;
    private static String fooRealmId;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        log.debug("Adding test realm for import from testrealm.json");
        RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(testRealm);

        UserRepresentation user = UserBuilder.create()
                .username("foo@user")
                .email("foo@user.com")
                .password("password")
                .build();

        RealmRepresentation realm2 = RealmBuilder.create()
                .name("foo")
                .user(user)
                .build();
        testRealms.add(realm2);
    }

    @Before
    public void before() {
        testingClient.server().run(session -> {
            testRealmId = session.realms().getRealmByName("test").getId();
            fooRealmId = session.realms().getRealmByName("foo").getId();
        });
    }

    @Test
    @ModelTest
    public void componentsTest(KeycloakSession session1) {
        doTest(session1,
            // Get ID of some component from realm1
            ((session, realm1) -> realm1.getComponentsStream().findFirst().get().getId()),
            // Test lookup realm1 component in realm2 should not work
            ((session, realm2, realm1ComponentId) -> {

                ComponentModel component = realm2.getComponent(realm1ComponentId);
                Assert.assertNull(component);

            }),
            // Try to update some component in realm1 through the realm2
            ((session, realm1, realm2, realm1ComponentId) -> {
                session.getContext().setRealm(realm1);
                ComponentModel component = realm1.getComponent(realm1ComponentId);
                component.put("key1", "Val1");
                session.getContext().setRealm(realm2);
                realm2.updateComponent(component);

            }),
            // Test update from above was not successful
            ((session, realm1, realm1ComponentId) -> {

                ComponentModel component = realm1.getComponent(realm1ComponentId);
                Assert.assertNull(component.get("key1"));

            }),
            // Try remove component from realm1 in the context of realm2
            ((session, realm1, realm2, realm1ComponentId) -> {
                session.getContext().setRealm(realm1);
                ComponentModel component = realm1.getComponent(realm1ComponentId);
                session.getContext().setRealm(realm2);
                realm2.removeComponent(component);

            }),
            // Test remove from above was not successful
            ((session, realm1, realm1ComponentId) -> {

                ComponentModel component = realm1.getComponent(realm1ComponentId);
                Assert.assertNotNull(component);

            })
        );
    }

    @Test
    @ModelTest
    public void requiredActionProvidersTest(KeycloakSession session1) {
        doTest(session1,
                // Get ID of some object from realm1
                ((session, realm1) -> realm1.getRequiredActionProvidersStream().findFirst().get().getId()),
                // Test lookup realm1 object in realm2 should not work
                ((session, realm2, realm1ReqActionId) -> {

                    RequiredActionProviderModel reqAction = realm2.getRequiredActionProviderById(realm1ReqActionId);
                    Assert.assertNull(reqAction);

                }),
                // Try to update some object in realm1 through the realm2
                ((session, realm1, realm2, realm1ReqActionId) -> {
                    session.getContext().setRealm(realm1);
                    RequiredActionProviderModel reqAction = realm1.getRequiredActionProviderById(realm1ReqActionId);
                    reqAction.getConfig().put("key1", "Val1");
                    session.getContext().setRealm(realm2);
                    realm2.updateRequiredActionProvider(reqAction);

                }),
                // Test update from above was not successful
                ((session, realm1, realm1ReqActionId) -> {

                    RequiredActionProviderModel reqAction = realm1.getRequiredActionProviderById(realm1ReqActionId);
                    Assert.assertNull(reqAction.getConfig().get("key1"));

                }),
                // Try remove object from realm1 in the context of realm2
                ((session, realm1, realm2, realm1ReqActionId) -> {
                    session.getContext().setRealm(realm1);
                    RequiredActionProviderModel reqAction = realm1.getRequiredActionProviderById(realm1ReqActionId);
                    session.getContext().setRealm(realm2);
                    realm2.removeRequiredActionProvider(reqAction);

                }),
                // Test remove from above was not successful
                ((session, realm1, realm1ReqActionId) -> {

                    RequiredActionProviderModel reqAction = realm1.getRequiredActionProviderById(realm1ReqActionId);
                    Assert.assertNotNull(reqAction);

                })
        );
    }


    @Test
    @ModelTest
    public void authenticationFlowsTest(KeycloakSession session1) {
        doTest(session1,
                // Get ID of some object from realm1
                ((session, realm1) -> {

                    AuthenticationFlowModel flow = realm1.getFlowByAlias(DefaultAuthenticationFlows.BROWSER_FLOW);
                    return flow.getId();

                }),
                // Test lookup realm1 object in realm2 should not work
                ((session, realm2, realm1FlowId) -> {

                    AuthenticationFlowModel flow = realm2.getAuthenticationFlowById(realm1FlowId);
                    Assert.assertNull(flow);

                }),
                // Try to update some object in realm1 through the realm2
                ((session, realm1, realm2, realm1FlowId) -> {
                    session.getContext().setRealm(realm1);
                    AuthenticationFlowModel flow = realm1.getAuthenticationFlowById(realm1FlowId);
                    flow.setDescription("foo");
                    session.getContext().setRealm(realm2);
                    realm2.updateAuthenticationFlow(flow);

                }),
                // Test update from above was not successful
                ((session, realm1, realm1FlowId) -> {

                    AuthenticationFlowModel flow = realm1.getAuthenticationFlowById(realm1FlowId);
                    Assert.assertNotEquals("foo", flow.getDescription());

                }),
                // Try remove object from realm1 in the context of realm2
                ((session, realm1, realm2, realm1FlowId) -> {
                    session.getContext().setRealm(realm1);
                    AuthenticationFlowModel flow = realm1.getAuthenticationFlowById(realm1FlowId);
                    session.getContext().setRealm(realm2);
                    realm2.removeAuthenticationFlow(flow);

                }),
                // Test remove from above was not successful
                ((session, realm1, realm1FlowId) -> {

                    AuthenticationFlowModel flow = realm1.getAuthenticationFlowById(realm1FlowId);
                    Assert.assertNotNull(flow);

                })
        );
    }


    @Test
    @ModelTest
    public void authenticationExecutionsTest(KeycloakSession session1) {
        doTest(session1,
                // Get ID of some object from realm1
                ((session, realm1) -> {

                    AuthenticationFlowModel flow = realm1.getFlowByAlias(DefaultAuthenticationFlows.BROWSER_FLOW);
                    return realm1.getAuthenticationExecutionsStream(flow.getId()).findFirst().get().getId();

                }),
                // Test lookup realm1 object in realm2 should not work
                ((session, realm2, realm1ExecutionId) -> {

                    AuthenticationExecutionModel execution = realm2.getAuthenticationExecutionById(realm1ExecutionId);
                    Assert.assertNull(execution);

                }),
                // Try to update some object in realm1 through the realm2
                ((session, realm1, realm2, realm1ExecutionId) -> {
                    session.getContext().setRealm(realm1);
                    AuthenticationExecutionModel execution = realm1.getAuthenticationExecutionById(realm1ExecutionId);
                    execution.setPriority(1234);
                    session.getContext().setRealm(realm2);
                    realm2.updateAuthenticatorExecution(execution);

                }),
                // Test update from above was not successful
                ((session, realm1, realm1ExecutionId) -> {

                    AuthenticationExecutionModel execution = realm1.getAuthenticationExecutionById(realm1ExecutionId);
                    Assert.assertNotEquals(1234, execution.getPriority());

                }),
                // Try remove object from realm1 in the context of realm2
                ((session, realm1, realm2, realm1ExecutionId) -> {
                    session.getContext().setRealm(realm1);
                    AuthenticationExecutionModel execution = realm1.getAuthenticationExecutionById(realm1ExecutionId);
                    session.getContext().setRealm(realm2);
                    realm2.removeAuthenticatorExecution(execution);

                }),
                // Test remove from above was not successful
                ((session,realm1, realm1ExecutionId) -> {

                    AuthenticationExecutionModel execution = realm1.getAuthenticationExecutionById(realm1ExecutionId);
                    Assert.assertNotNull(execution);

                })
        );
    }


    @Test
    @ModelTest
    public void authenticationConfigsTest(KeycloakSession session1) {
        doTest(session1,
                // Get ID of some object from realm1
                ((session, realm1) -> realm1.getAuthenticatorConfigsStream().findFirst().get().getId()),
                // Test lookup realm1 object in realm2 should not work
                ((session, realm2, realm1AuthConfigId) -> {

                    AuthenticatorConfigModel config = realm2.getAuthenticatorConfigById(realm1AuthConfigId);
                    Assert.assertNull(config);

                }),
                // Try to update some object in realm1 through the realm2
                ((session, realm1, realm2, realm1AuthConfigId) -> {
                    session.getContext().setRealm(realm1);
                    AuthenticatorConfigModel config = realm1.getAuthenticatorConfigById(realm1AuthConfigId);
                    config.getConfig().put("key1", "val1");
                    session.getContext().setRealm(realm2);
                    realm2.updateAuthenticatorConfig(config);

                }),
                // Test update from above was not successful
                ((session, realm1, realm1AuthConfigId) -> {

                    AuthenticatorConfigModel config = realm1.getAuthenticatorConfigById(realm1AuthConfigId);
                    Assert.assertNull(config.getConfig().get("key1"));

                }),
                // Try remove object from realm1 in the context of realm2
                ((session, realm1, realm2, realm1AuthConfigId) -> {
                    session.getContext().setRealm(realm1);
                    AuthenticatorConfigModel config = realm1.getAuthenticatorConfigById(realm1AuthConfigId);
                    session.getContext().setRealm(realm2);
                    realm2.removeAuthenticatorConfig(config);

                }),
                // Test remove from above was not successful
                ((session, realm1, realm1AuthConfigId) -> {

                    AuthenticatorConfigModel config = realm1.getAuthenticatorConfigById(realm1AuthConfigId);
                    Assert.assertNotNull(config);

                })
        );
    }


    @Test
    @ModelTest
    public void clientInitialAccessTest(KeycloakSession session1) {
        doTest(session1,
                // Get ID of some object from realm1
                ((session, realm1) -> {

                    ClientInitialAccessModel clientInitialAccess = session.getProvider(RealmProvider.class).createClientInitialAccessModel(realm1, 10, 20);
                    return clientInitialAccess.getId();

                }),
                // Test lookup realm1 object in realm2 should not work
                ((session, realm2, realm1ClientInitialAccessId) -> {

                    ClientInitialAccessModel clientInitialAccess = session.getProvider(RealmProvider.class).getClientInitialAccessModel(realm2, realm1ClientInitialAccessId);
                    Assert.assertNull(clientInitialAccess);

                }),
                // Try to update some object in realm1 through the realm2
                ((session, realm1, realm2, realm1ClientInitialAccessId) -> {

                    // No-op, update not supported for clientInitialAccessModel

                }),
                // Test update from above was not successful
                ((session, realm1, realm1ClientInitialAccessId) -> {

                    // No-op, update not supported for clientInitialAccessModel

                }),
                // Try remove object from realm1 in the context of realm2
                ((session, realm1, realm2, realm1ClientInitialAccessId) -> {
                    session.getContext().setRealm(realm2);
                    session.getProvider(RealmProvider.class).removeClientInitialAccessModel(realm2, realm1ClientInitialAccessId);

                }),
                // Test remove from above was not successful
                ((session, realm1, realm1ClientInitialAccessId) -> {

                    ClientInitialAccessModel clientInitialAccess = session.getProvider(RealmProvider.class).getClientInitialAccessModel(realm1, realm1ClientInitialAccessId);
                    Assert.assertNotNull(clientInitialAccess);

                })
        );
    }

    @Test
    @ModelTest
    public void rolesTest(KeycloakSession session1) {
        doTest(session1,
                // Get ID of some object from realm1
                ((session, realm1) -> {

                    RoleModel role = session.getProvider(RoleProvider.class).addRealmRole(realm1, "foo");
                    return role.getId();

                }),
                // Test lookup realm1 object in realm2 should not work
                ((session, realm2, realm1RoleId) -> {

                    RoleModel role = session.getProvider(RoleProvider.class).getRoleById(realm2, realm1RoleId);
                    Assert.assertNull(role);

                }),
                // Try to update some object in realm1 through the realm2
                ((session, realm1, realm2, realm1RoleId) -> {

                    // No-op, update done directly by calling operations on RoleModel. No explicit updateRole method on the RealmModel

                }),
                // Test update from above was not successful
                ((session, realm1, realm1RoleId) -> {

                    // No-op, update done directly by calling operations on RoleModel. No explicit updateRole method on the RealmModel

                }),
                // Try remove object from realm1 in the context of realm2
                ((session, realm1, realm2, realm1RoleId) -> {

                    // not possible to remove object from realm1 in the context of realm2 any more

                }),
                // Test remove from above was not successful
                ((session, realm1, realm1RoleId) -> {

                    // nothing to test

                })
        );
    }

    @Test
    @ModelTest
    public void userSessionsTest(KeycloakSession session1) {
        doTest(session1,
                // Get ID of some object from realm1
                ((session, realm1) -> {

                    UserModel user = session.users().getUserByUsername(realm1, "test-user@localhost");
                    UserSessionModel userSession = session.sessions().createUserSession(null, realm1, user, user.getUsername(), "1.2.3.4", "bar", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                    return userSession.getId();

                }),
                // Test lookup realm1 object in realm2 should not work
                ((session, realm2, realm1SessionId) -> {

                    UserSessionModel userSession = session.sessions().getUserSession(realm2, realm1SessionId);
                    Assert.assertNull(userSession);

                }),
                // Try to update some object in realm1 through the realm2
                ((session, realm1, realm2, realm1SessionId) -> {

                    // No-op, update done directly by calling operations on UserSessionModel. No explicit update method

                }),
                // Test update from above was not successful
                ((session, realm1, realm1SessionId) -> {

                    // No-op, update done directly by calling operations on UserSessionModel. No explicit update method.

                }),
                // Try remove object from realm1 in the context of realm2
                ((session, realm1, realm2, realm1SessionId) -> {
                    session.getContext().setRealm(realm1);
                    UserSessionModel userSession = session.sessions().getUserSession(realm1, realm1SessionId);
                    session.getContext().setRealm(realm2);
                    session.sessions().removeUserSession(realm2, userSession);

                }),
                // Test remove from above was not successful
                ((session, realm1, realm1SessionId) -> {

                    UserSessionModel userSession = session.sessions().getUserSession(realm1, realm1SessionId);
                    Assert.assertNotNull(userSession);

                })
        );
    }


    private void doTest(KeycloakSession session1,
                               BiFunction<KeycloakSession, RealmModel, String> realm1ObjectIdProducer,
                               TriConsumer<KeycloakSession, RealmModel, String> testLookupRealm1ObjectInRealm2,
                               TetraConsumer<KeycloakSession, RealmModel, RealmModel, String> updaterRealm1ObjectInRealm2,
                               TriConsumer<KeycloakSession, RealmModel, String> testUpdateFailed,
                               TetraConsumer<KeycloakSession, RealmModel, RealmModel, String> removeRealm1ObjectInRealm2,
                               TriConsumer<KeycloakSession, RealmModel, String> testRemoveFailed
    ) {

        // Transaction 1 - Lookup object of realm1
        AtomicReference<String> realm1ObjectId = new AtomicReference<>();
        KeycloakModelUtils.runJobInTransaction(session1.getKeycloakSessionFactory(), (KeycloakSession session) -> {
            // can't use getRealmByName as that returns the infinispan realm adapter version, meaning the tests will query
            // the cache instead of the actual provider.
            RealmModel realm1 = session.getProvider(RealmProvider.class).getRealm(testRealmId);
            session.getContext().setRealm(realm1);
            realm1ObjectId.set(realm1ObjectIdProducer.apply(session, realm1));

        });

        // Transaction 2
        KeycloakModelUtils.runJobInTransaction(session1.getKeycloakSessionFactory(), (KeycloakSession session) -> {
            RealmModel realm1 = session.getProvider(RealmProvider.class).getRealm(testRealmId);
            RealmModel realm2 = session.getProvider(RealmProvider.class).getRealm(fooRealmId);

            session.getContext().setRealm(realm2);
            testLookupRealm1ObjectInRealm2.accept(session, realm2, realm1ObjectId.get());
            // each implementation of updater should set the realm in context according to the operations executed
            updaterRealm1ObjectInRealm2.accept(session, realm1, realm2, realm1ObjectId.get());

        });

        // Transaction 3
        KeycloakModelUtils.runJobInTransaction(session1.getKeycloakSessionFactory(), (KeycloakSession session) -> {
            RealmModel realm1 = session.getProvider(RealmProvider.class).getRealm(testRealmId);
            session.getContext().setRealm(realm1);
            testUpdateFailed.accept(session, realm1, realm1ObjectId.get());
        });

        // Transaction 4
        try {
            KeycloakModelUtils.runJobInTransaction(session1.getKeycloakSessionFactory(), (KeycloakSession session) -> {
                RealmModel realm1 = session.getProvider(RealmProvider.class).getRealm(testRealmId);
                RealmModel realm2 = session.getProvider(RealmProvider.class).getRealm(fooRealmId);
                // each implementation of remover should set the realm in context according to the operations executed
                removeRealm1ObjectInRealm2.accept(session, realm1, realm2, realm1ObjectId.get());

            });
        } catch (ModelException e) {
            // This is fine. Attempt to remove on incorrect object can throw an exception in some cases, which will enforce transaction rollback
        }

        // Transaction 5
        KeycloakModelUtils.runJobInTransaction(session1.getKeycloakSessionFactory(), (KeycloakSession session) -> {
            RealmModel realm1 = session.getProvider(RealmProvider.class).getRealm(testRealmId);
            session.getContext().setRealm(realm1);
            testRemoveFailed.accept(session, realm1, realm1ObjectId.get());
        });
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T var1, U var2, V var3);
    }

    @FunctionalInterface
    public interface TetraConsumer<T, U, V, W> {
        void accept(T var1, U var2, V var3, W var4);
    }
}
