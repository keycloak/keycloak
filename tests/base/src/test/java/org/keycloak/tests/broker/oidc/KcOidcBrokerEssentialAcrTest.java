package org.keycloak.tests.broker.oidc;

import java.util.List;
import java.util.Map;

import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.access.AllowAccessAuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.LoginUrlBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An essential level requested by the client is enforced when the post broker login flow finishes, not when the first broker
 * login flow finishes, because the post broker login flow can still reach the requested level.
 */
@KeycloakIntegrationTest
public class KcOidcBrokerEssentialAcrTest extends AbstractKcOidcBrokerTest {

    private static final String POST_BROKER_LOGIN_FLOW_ALIAS = "post-broker-login-step-up";

    @InjectRunOnServer(realmRef = "consumer")
    RunOnServerClient runOnServer;

    @InjectPage
    ErrorPage errorPage;

    @Test
    public void firstBrokerLoginReachesEssentialLevelInPostBrokerLoginFlow() {
        configurePostBrokerLoginFlow(true);

        loginFormWithEssentialAcr("2").open();
        logInWithBroker();
        logInAsUserInIDPForFirstTime();
        updateAccountInformation();

        assertLoggedInWithAcr("2");
    }

    @Test
    public void firstBrokerLoginNotReachingEssentialLevelFails() {
        configurePostBrokerLoginFlow(false);

        loginFormWithEssentialAcr("2").open();
        logInWithBroker();
        logInAsUserInIDPForFirstTime();
        updateAccountInformation();

        // the post broker login flow finishes the authentication at level 1
        errorPage.assertCurrent();
        assertEquals("Authentication requirements not fulfilled", errorPage.getError());
    }

    @Test
    public void existingUserReachesEssentialLevelInPostBrokerLoginFlow() {
        configurePostBrokerLoginFlow(true);
        loginUser();
        AccountHelper.logout(consumerRealm.admin(), getUserLogin());
        AccountHelper.logout(providerRealm.admin(), getUserLogin());

        loginFormWithEssentialAcr("2").open();
        logInWithBroker();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        assertLoggedInWithAcr("2");
    }

    /**
     * Post broker login flow of the identity provider which reaches level 2 through a level condition, or which records no level.
     */
    private void configurePostBrokerLoginFlow(boolean withLevel2Condition) {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            AuthenticationFlowModel flow = new AuthenticationFlowModel();
            flow.setAlias(POST_BROKER_LOGIN_FLOW_ALIAS);
            flow.setProviderId(AuthenticationFlow.BASIC_FLOW);
            flow.setTopLevel(true);
            flow.setBuiltIn(false);
            flow = realm.addAuthenticationFlow(flow);

            FlowUtil flowUtil = FlowUtil.inCurrentRealm(session).selectFlow(POST_BROKER_LOGIN_FLOW_ALIAS);
            if (withLevel2Condition) {
                flowUtil.addSubFlowExecution(Requirement.CONDITIONAL, subflow -> subflow
                        .addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                config -> {
                                    config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2");
                                    config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(ConditionalLoaAuthenticator.DEFAULT_MAX_AGE));
                                })
                        .addAuthenticatorExecution(Requirement.REQUIRED, AllowAccessAuthenticatorFactory.PROVIDER_ID));
            } else {
                flowUtil.addAuthenticatorExecution(Requirement.REQUIRED, AllowAccessAuthenticatorFactory.PROVIDER_ID);
            }

            IdentityProviderModel idp = session.identityProviders().getByAlias(IDP_OIDC_ALIAS);
            idp.setPostBrokerLoginFlowId(flow.getId());
            session.identityProviders().update(idp);
        });
    }

    private LoginUrlBuilder loginFormWithEssentialAcr(String acr) {
        ClaimsRepresentation.ClaimValue<String> acrClaim = new ClaimsRepresentation.ClaimValue<>();
        acrClaim.setEssential(true);
        acrClaim.setValues(List.of(acr));

        ClaimsRepresentation claims = new ClaimsRepresentation();
        claims.setIdTokenClaims(Map.of(IDToken.ACR, acrClaim));

        return oauth.loginForm().claims(claims);
    }

    private void assertLoggedInWithAcr(String expectedAcr) {
        webDriver.waiting().waitForOAuthCallback();
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        assertTrue(response.isSuccess(), "Login should succeed, but got error: " + response.getError());
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(response.getCode());
        assertEquals(expectedAcr, oauth.parseToken(tokenResponse.getIdToken(), IDToken.class).getAcr());
    }
}
