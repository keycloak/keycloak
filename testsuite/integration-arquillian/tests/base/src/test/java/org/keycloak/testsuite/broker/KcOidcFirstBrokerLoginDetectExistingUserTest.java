package org.keycloak.testsuite.broker;

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
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.ExecutionBuilder;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KcOidcFirstBrokerLoginDetectExistingUserTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Override
    @Before
    public void beforeBrokerTest() {
        super.beforeBrokerTest();
        log.debug("creating detect existing user flow for realm " + bc.providerRealmName());

        final RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        AuthenticationManagementResource authMgmtResource = consumerRealm.flows();

        // Creates detectExistingUserFlow
        String detectExistingFlowAlias = "detectExistingUserFlow";
        final AuthenticationFlowRepresentation authenticationFlowRepresentation = newFlow(detectExistingFlowAlias, detectExistingFlowAlias, "basic-flow", true, false);
        authMgmtResource.createFlow(authenticationFlowRepresentation);

        AuthenticationFlowRepresentation authenticationFlowRepresentation1 = getFlow(authMgmtResource, detectExistingFlowAlias);
        assertNotNull("The authentication flow must exist", authenticationFlowRepresentation1);

        String flowId = authenticationFlowRepresentation1.getId(); // retrieves the id of the newly created flow

        // Adds executions to the flow
        addExecution(authMgmtResource, flowId, IdpDetectExistingBrokerUserAuthenticatorFactory.PROVIDER_ID, 10);
        addExecution(authMgmtResource, flowId, IdpAutoLinkAuthenticatorFactory.PROVIDER_ID, 20);

        // Updates the FirstBrokerLoginFlowAlias for the identity provider
        IdentityProviderResource identityConsumerResource = consumerRealm.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation identityProviderRepresentation = consumerRealm.identityProviders().findAll().get(0);
        identityProviderRepresentation.setFirstBrokerLoginFlowAlias(detectExistingFlowAlias);
        identityProviderRepresentation.getConfig().put(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.FORCE.toString());
        identityConsumerResource.update(identityProviderRepresentation);

        assertEquals("Two executions must have been created", 2, getFlow(authMgmtResource, detectExistingFlowAlias).getAuthenticationExecutions().size());
    }

    private void addExecution(AuthenticationManagementResource authMgmtResource, String flowId, String providerId, int priority) {
        AuthenticationExecutionRepresentation exec = ExecutionBuilder.create()
                .parentFlow(flowId)
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString())
                .authenticator(providerId)
                .priority(priority)
                .authenticatorFlow(false)
                .build();
        authMgmtResource.addExecution(exec);
    }

    private AuthenticationFlowRepresentation getFlow(AuthenticationManagementResource authMgmtResource, String detectExistingFlowAlias) {
        return authMgmtResource.getFlows().stream()
                .filter(v -> detectExistingFlowAlias.equals(v.getAlias()))
                .findFirst().get();
    }


    private AuthenticationFlowRepresentation newFlow(String alias, String description,
                                             String providerId, boolean topLevel, boolean builtIn) {
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(alias);
        flow.setDescription(description);
        flow.setProviderId(providerId);
        flow.setTopLevel(topLevel);
        flow.setBuiltIn(builtIn);
        return flow;
    }

    @Test
    public void loginWhenUserDoesNotExistOnConsumer() {

        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        String firstname = "Firstname";
        String lastname = "Lastname";
        String username = "firstandlastname";
        createUser(bc.providerRealmName(), username, BrokerTestConstants.USER_PASSWORD, firstname, lastname, "firstnamelastname@example.org");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithIdp(bc.getIDPAlias(), username, BrokerTestConstants.USER_PASSWORD);

        loginPage.assertCurrent(bc.consumerRealmName());

        assertEquals("User " +  username + " authenticated with identity provider " + bc.getIDPAlias() + " does not exist. Please contact your administrator.", loginPage.getInstruction());
    }

    @Test
    public void loginWhenUserExistsOnConsumer() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        final String firstname = "Firstname_loginWhenUserExistsOnConsumer";
        final String lastname = "Lastname_loginWhenUserExistsOnConsumer";
        final String username = "firstandlastname";
        final String email = "firstnamelastname@example.org";
        createUser(bc.providerRealmName(), username, BrokerTestConstants.USER_PASSWORD, firstname, lastname, email);
        createUser(bc.consumerRealmName(), username, "THIS PASSWORD IS USELESS", null, null, email);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithIdp(bc.getIDPAlias(), username, BrokerTestConstants.USER_PASSWORD);

        assertTrue(driver.getTitle().contains("AUTH_RESPONSE"));
        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(
                adminClient.realm(bc.consumerRealmName()), username);

        assertEquals("Email is not correct", userRepresentation.getEmail(), email);
        assertEquals("Firstname is not correct", userRepresentation.getFirstName(), firstname);
        assertEquals("Lastname is not correct", userRepresentation.getLastName(), lastname);
    }
}
