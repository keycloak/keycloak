package org.keycloak.tests.broker;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.broker.IdpAutoLinkAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpDetectExistingBrokerUserAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.AuthenticationExecutionBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcOidcFirstBrokerLoginDetectExistingUserTest extends AbstractKcOidcBrokerTest {

    @InjectPage
    ErrorPage errorPage;

    @BeforeEach
    void setupDetectExistingUserFlow() {
        RealmResource consumer = consumerRealm.admin();
        AuthenticationManagementResource authMgmtResource = consumer.flows();

        String detectExistingFlowAlias = "detectExistingUserFlow";
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(detectExistingFlowAlias);
        flow.setDescription(detectExistingFlowAlias);
        flow.setProviderId("basic-flow");
        flow.setTopLevel(true);
        flow.setBuiltIn(false);
        authMgmtResource.createFlow(flow);

        AuthenticationFlowRepresentation created = authMgmtResource.getFlows().stream()
                .filter(v -> detectExistingFlowAlias.equals(v.getAlias()))
                .findFirst()
                .orElse(null);
        assertNotNull(created, "The authentication flow must exist");

        String flowId = created.getId();
        addExecution(authMgmtResource, flowId, IdpDetectExistingBrokerUserAuthenticatorFactory.PROVIDER_ID, 10);
        addExecution(authMgmtResource, flowId, IdpAutoLinkAuthenticatorFactory.PROVIDER_ID, 20);

        IdentityProviderResource identityConsumerResource = consumer.identityProviders().get(IDP_OIDC_ALIAS);
        IdentityProviderRepresentation identityProviderRepresentation = consumer.identityProviders().findAll().get(0);
        identityProviderRepresentation.setFirstBrokerLoginFlowAlias(detectExistingFlowAlias);
        identityProviderRepresentation.getConfig().put(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.FORCE.toString());
        identityConsumerResource.update(identityProviderRepresentation);

        assertEquals(2, authMgmtResource.getFlows().stream()
                        .filter(v -> detectExistingFlowAlias.equals(v.getAlias()))
                        .findFirst()
                        .orElseThrow()
                        .getAuthenticationExecutions()
                        .size(),
                "Two executions must have been created");
    }

    private void addExecution(AuthenticationManagementResource authMgmtResource, String flowId, String providerId, int priority) {
        AuthenticationExecutionRepresentation exec = AuthenticationExecutionBuilder.create()
                .parentFlow(flowId)
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString())
                .authenticator(providerId)
                .priority(priority)
                .authenticatorFlow(false)
                .build();
        authMgmtResource.addExecution(exec);
    }

    private void createUser(ManagedRealm realm, String username, String password, String firstName, String lastName, String email) {
        UserBuilder builder = UserBuilder.create(username)
                .email(email)
                .password(password)
                .enabled(true);
        if (firstName != null) {
            builder.firstName(firstName);
        }
        if (lastName != null) {
            builder.lastName(lastName);
        }
        try (Response response = realm.admin().users().create(builder.build())) {
            assertEquals(201, response.getStatus());
            String userId = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.users().get(userId).remove());
        }
    }

    private void logInWithIdp(String username, String password) {
        oauth.openLoginForm();
        logInWithBroker();
        loginPage.fillLogin(username, password);
        loginPage.submit();
    }

    // Default broker login scenarios do not apply with the custom detect-existing first-broker-login flow.
    @Override
    @Test
    @Disabled("Uses a custom detect-existing first-broker-login flow")
    public void testLogInAsUserInIDP() {
    }

    @Override
    @Test
    @Disabled("Uses a custom detect-existing first-broker-login flow")
    public void testLoginWithExistingUser() {
    }

    @Test
    public void loginWhenUserDoesNotExistOnConsumer() {
        disableUpdateProfileOnFirstLogin();

        String firstname = "Firstname";
        String lastname = "Lastname";
        String username = "firstandlastname";
        createUser(providerRealm, username, USER_PASSWORD, firstname, lastname, "firstnamelastname@example.org");

        logInWithIdp(username, USER_PASSWORD);

        errorPage.assertCurrent();
        assertEquals("User " + username + " authenticated with identity provider " + IDP_OIDC_ALIAS
                + " does not exist. Please contact your administrator.", errorPage.getError());
    }

    @Test
    public void loginWhenUserExistsOnConsumer() {
        disableUpdateProfileOnFirstLogin();

        final String firstname = "Firstname_loginWhenUserExistsOnConsumer";
        final String lastname = "Lastname_loginWhenUserExistsOnConsumer";
        final String username = "firstandlastname";
        final String email = "firstnamelastname@example.org";
        createUser(providerRealm, username, USER_PASSWORD, firstname, lastname, email);
        createUser(consumerRealm, username, "THIS PASSWORD IS USELESS", null, null, email);

        logInWithIdp(username, USER_PASSWORD);

        assertTrue(oauth.parseLoginResponse().isSuccess(), "Broker login should complete successfully");
        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(consumerRealm.admin(), username);

        assertEquals(email, userRepresentation.getEmail(), "Email is not correct");
        assertEquals(firstname, userRepresentation.getFirstName(), "Firstname is not correct");
        assertEquals(lastname, userRepresentation.getLastName(), "Lastname is not correct");
    }
}
