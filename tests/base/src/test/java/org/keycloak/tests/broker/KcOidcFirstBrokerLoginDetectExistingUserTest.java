package org.keycloak.tests.broker;

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
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = BrokerServerConfig.class)
public class KcOidcFirstBrokerLoginDetectExistingUserTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Override
    @BeforeEach
    public void beforeBrokerTest() {
        super.beforeBrokerTest();
        log.debug("creating detect existing user flow for realm " + bc.providerRealmName());

        final RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        AuthenticationManagementResource authMgmtResource = consumerRealm.flows();

        String detectExistingFlowAlias = "detectExistingUserFlow";
        final AuthenticationFlowRepresentation authenticationFlowRepresentation =
                newFlow(detectExistingFlowAlias, detectExistingFlowAlias, "basic-flow", true, false);
        authMgmtResource.createFlow(authenticationFlowRepresentation);

        AuthenticationFlowRepresentation authenticationFlowRepresentation1 = getFlow(authMgmtResource, detectExistingFlowAlias);
        assertNotNull(authenticationFlowRepresentation1, "The authentication flow must exist");

        String flowId = authenticationFlowRepresentation1.getId();

        addExecution(authMgmtResource, flowId, IdpDetectExistingBrokerUserAuthenticatorFactory.PROVIDER_ID, 10);
        addExecution(authMgmtResource, flowId, IdpAutoLinkAuthenticatorFactory.PROVIDER_ID, 20);

        IdentityProviderResource identityConsumerResource = consumerRealm.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation identityProviderRepresentation = consumerRealm.identityProviders().findAll().get(0);
        identityProviderRepresentation.setFirstBrokerLoginFlowAlias(detectExistingFlowAlias);
        identityProviderRepresentation.getConfig().put(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.FORCE.toString());
        identityConsumerResource.update(identityProviderRepresentation);

        assertEquals(2, getFlow(authMgmtResource, detectExistingFlowAlias).getAuthenticationExecutions().size(),
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
        updateExecutions(BrokerFirstLoginFlowMutators::disableUpdateProfileOnFirstLogin);

        String firstname = "Firstname";
        String lastname = "Lastname";
        String username = "firstandlastname";
        createUser(bc.providerRealmName(), username, BrokerTestConstants.USER_PASSWORD, firstname, lastname, "firstnamelastname@example.org");

        openConsumerBrokerLoginForm();

        logInWithIdp(bc.getIDPAlias(), username, BrokerTestConstants.USER_PASSWORD);

        errorPage.assertCurrent();
        assertEquals("User " + username + " authenticated with identity provider " + bc.getIDPAlias()
                + " does not exist. Please contact your administrator.", errorPage.getError());
    }

    @Test
    public void loginWhenUserExistsOnConsumer() {
        updateExecutions(BrokerFirstLoginFlowMutators::disableUpdateProfileOnFirstLogin);

        final String firstname = "Firstname_loginWhenUserExistsOnConsumer";
        final String lastname = "Lastname_loginWhenUserExistsOnConsumer";
        final String username = "firstandlastname";
        final String email = "firstnamelastname@example.org";
        createUser(bc.providerRealmName(), username, BrokerTestConstants.USER_PASSWORD, firstname, lastname, email);
        createUser(bc.consumerRealmName(), username, "THIS PASSWORD IS USELESS", null, null, email);

        openConsumerBrokerLoginForm();

        logInWithIdp(bc.getIDPAlias(), username, BrokerTestConstants.USER_PASSWORD);

        assertTrue(oauth.parseLoginResponse().isSuccess(), "Broker login should complete successfully");
        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(
                adminClient.realm(bc.consumerRealmName()), username);

        assertEquals(email, userRepresentation.getEmail(), "Email is not correct");
        assertEquals(firstname, userRepresentation.getFirstName(), "Firstname is not correct");
        assertEquals(lastname, userRepresentation.getLastName(), "Lastname is not correct");
    }
}
