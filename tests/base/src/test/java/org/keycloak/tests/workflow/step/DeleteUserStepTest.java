/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.workflow.step;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.workflow.DeleteUserStepProviderFactory;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.events.UserAuthenticatedWorkflowEventFactory;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ConsentPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;
import org.keycloak.tests.workflow.step.DeleteUserStepTest.DeleteUserWorkflowServerConf;
import org.keycloak.testsuite.federation.DummyUserFederationProvider;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.keycloak.models.workflow.DeleteUserStepProvider.PROPAGATE_TO_SP;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests the execution of the 'delete-user' workflow step.
 */
@KeycloakIntegrationTest(config = DeleteUserWorkflowServerConf.class)
public class DeleteUserStepTest extends AbstractWorkflowTest {

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ConsentPage consentPage;

    private static final String USER_NAME = "user1";
    private static final String USER_PASSWORD = "passwd1";

    static Stream<Arguments> deleteFederatedUserTestProvider() {
        return Stream.of(
            Arguments.of("true", true),
            Arguments.of("TRUE", true),
            Arguments.of("false", false),
            Arguments.of("1", false),
            Arguments.of(null, false)
        );
    }

    @ParameterizedTest
    @DisplayName("DeleteUserStep should delete federated user according to flag")
    @MethodSource("deleteFederatedUserTestProvider")
    public void testDeleteFederatedUserWithFlagEnabled(String propagateToSp, boolean userRemoved) {
        WorkflowStepRepresentation.Builder builder = WorkflowStepRepresentation.create().of(DeleteUserStepProviderFactory.ID)
                .after(Duration.ofDays(1));
        if (propagateToSp != null) {
            builder = builder.withConfig(PROPAGATE_TO_SP, propagateToSp);
        }

        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserAuthenticatedWorkflowEventFactory.ID)
                .withSteps(builder.build()).build()).close();

        String componentId = addDummyFederationProvider();
        addFederatedUser(componentId, USER_NAME, USER_PASSWORD);

        // Authenticate using federated user
        oauth.openLoginForm();
        loginPage.fillLogin(USER_NAME, USER_PASSWORD);
        loginPage.submit();
        assertTrue(driver.page().getPageSource().contains("Happy days"), "Test user should be successfully logged in.");

        runScheduledSteps(Duration.ZERO);

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            // user1 is present in local storage
            UserModel user = UserStoragePrivateUtil.userLocalStorage(session)
                                                   .getUserByUsername(realm, USER_NAME);
            assertNotNull(user);
            assertTrue(user.isEnabled());
        });

        runScheduledSteps(Duration.ofDays(2));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            DummyUserFederationProviderFactory providerFactory = (DummyUserFederationProviderFactory) session.getKeycloakSessionFactory()
                                                                        .getProviderFactory(UserStorageProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME);
            ComponentModel model = realm.getStorageProviders(UserStorageProvider.class).findFirst().orElse(null);
            assertNotNull(model);
            DummyUserFederationProvider provider = providerFactory.create(session, model);
            UserModel federatedUser = provider.getUserByUsername(realm, USER_NAME);
            if (userRemoved) {
                assertNull(federatedUser);
            } else {
                assertNotNull(federatedUser);
            }

            // Cleanup
            if (!userRemoved) {
                provider.removeUser(realm, federatedUser);
            }
        });
    }

    static Stream<Arguments> deleteUserRemovesAllScheduledStepsTestProvider() {
        return Stream.of(
                Arguments.of(false),
                Arguments.of(true)
        );
    }

    @ParameterizedTest
    @DisplayName("DeleteUserStep should trigger UserRemovedEvent to remove all scheduled steps for the user")
    @MethodSource("deleteUserRemovesAllScheduledStepsTestProvider")
    public void testDeleteUserRemovesAllScheduledSteps(boolean federated) {
        // create a couple of workflows that will activate for the test user
        // the first one will run the delete user step before the second one runs its first step
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("workflow1")
                .onEvent(UserAuthenticatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DeleteUserStepProviderFactory.ID)
                                .after(Duration.ofDays(1))
                                .build()
                ).build()).close();
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("workflow2")
                .onEvent(UserAuthenticatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("attribute", "attr1")
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        // add a test user and activate the workflows by logging in
        String userId;
        if (federated) {
            String componentId = addDummyFederationProvider();
            userId = addFederatedUser(componentId, USER_NAME, USER_PASSWORD);
        } else {
            try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
                    .username(USER_NAME).password(USER_PASSWORD).firstName("Federated").lastName("User").email(USER_NAME + "@example.com").build())) {
                userId = ApiUtil.getCreatedId(response);
            }
        }

        oauth.openLoginForm();
        loginPage.fillLogin(USER_NAME, USER_PASSWORD);
        loginPage.submit();
        assertTrue(driver.driver().getPageSource().contains("Happy days"), "Test user should be successfully logged in.");

        // check that we have two scheduled steps for the user
        runOnServer.run((RunOnServer) session -> {
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);

            List<Workflow> registeredWorkflows = provider.getWorkflows().toList();
            assertEquals(2, registeredWorkflows.size());

            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<WorkflowStateProvider.ScheduledStep> steps = stateProvider.getScheduledStepsByResource(userId).toList();
            assertThat(steps, hasSize(2));
        });

        // now run the scheduled steps after 2 days - this should delete the user and remove all scheduled steps
        runScheduledSteps(Duration.ofDays(2));
        runOnServer.run((RunOnServer) session -> {
            // ensure user is deleted
            RealmModel realm = session.getContext().getRealm();
            if (federated) {
                // assert federated user was removed locally
                UserModel user = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, USER_NAME);
                assertNull(user);

                // cleanup federated user
                DummyUserFederationProviderFactory providerFactory = (DummyUserFederationProviderFactory) session.getKeycloakSessionFactory()
                        .getProviderFactory(UserStorageProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME);
                ComponentModel model = realm.getStorageProviders(UserStorageProvider.class).findFirst().orElse(null);
                assertNotNull(model);
                DummyUserFederationProvider provider = providerFactory.create(session, model);
                UserModel federatedUser = provider.getUserByUsername(realm, USER_NAME);
                provider.removeUser(realm, federatedUser);
            } else {
                UserModel user = session.users().getUserById(realm, userId);
                assertNull(user);
            }

            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<WorkflowStateProvider.ScheduledStep> steps = stateProvider.getScheduledStepsByResource(userId).toList();
            assertThat(steps, hasSize(0));
        });
    }

    private String addFederatedUser(String componentId, String username, String password) {
        UserRepresentation fedUser = new UserRepresentation();
        fedUser.setUsername(username);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        fedUser.setCredentials(List.of(credential));
        fedUser.setEmail("user1@localhost");
        fedUser.setFirstName("Federated");
        fedUser.setLastName("User");
        fedUser.setEnabled(true);
        fedUser.setFederationLink(componentId);
        Response createResponse = managedRealm.admin().users().create(fedUser);
        assertEquals(201, createResponse.getStatus());
        return ApiUtil.getCreatedId(createResponse);
    }

    private String addDummyFederationProvider() {
        ComponentRepresentation dummyFederationProvider = new ComponentRepresentation();
        String componentId = KeycloakModelUtils.generateId();
        dummyFederationProvider.setId(componentId);
        dummyFederationProvider.setName(DummyUserFederationProviderFactory.PROVIDER_NAME);
        dummyFederationProvider.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
        dummyFederationProvider.setProviderType(UserStorageProvider.class.getName());
        try (Response addResponse = managedRealm.admin().components().add(dummyFederationProvider)) {
            assertEquals(201, addResponse.getStatus());
        }
        return componentId;
    }


    public static class DeleteUserWorkflowServerConf extends WorkflowsBlockingServerConfig {

      @Override
      public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
        return super.configure(builder).dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
      }
    }
}
