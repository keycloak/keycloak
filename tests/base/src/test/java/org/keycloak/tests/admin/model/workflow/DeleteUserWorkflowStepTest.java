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

package org.keycloak.tests.admin.model.workflow;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.workflow.DeleteUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ConsentPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.model.workflow.DeleteUserWorkflowStepTest.DeleteUserWorkflowServerConf;
import org.keycloak.testsuite.federation.DummyUserFederationProvider;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.WebDriver;

import static org.keycloak.models.workflow.DeleteUserStepProvider.PROPAGATE_TO_SP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
@KeycloakIntegrationTest(config = DeleteUserWorkflowServerConf.class)
public class DeleteUserWorkflowStepTest extends AbstractWorkflowTest {

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests", realmRef = "consumer")
    RunOnServerClient runOnServer;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(ref = "consumer", realmRef = "consumer")
    OAuthClient consumerRealmOAuth;

    @InjectWebDriver
    WebDriver driver;

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

        consumerRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_LOGGED_IN.toString())
                .withSteps(builder.build()).build()).close();

        String componentId = addDummyFederationProvider();
        String userId = addFederatedUser(componentId, USER_NAME, USER_PASSWORD);

        // Authenticate using federated user
        consumerRealmOAuth.openLoginForm();
        loginPage.fillLogin(USER_NAME, USER_PASSWORD);
        loginPage.submit();
        assertTrue(driver.getPageSource().contains("Happy days"), "Test user should be successfully logged in.");

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
        Response createResponse = consumerRealm.admin().users().create(fedUser);
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
        try (Response addResponse = consumerRealm.admin().components().add(dummyFederationProvider)) {
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
