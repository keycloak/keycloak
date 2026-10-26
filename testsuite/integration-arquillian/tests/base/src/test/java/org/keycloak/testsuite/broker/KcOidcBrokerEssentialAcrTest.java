package org.keycloak.testsuite.broker;

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
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.LoginUrlBuilder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * An essential level requested by the client is enforced when the post broker login flow finishes, not when the first broker
 * login flow finishes, because the post broker login flow can still reach the requested level.
 */
public class KcOidcBrokerEssentialAcrTest extends AbstractBrokerTest {

    private static final String POST_BROKER_LOGIN_FLOW_ALIAS = "post-broker-login-step-up";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Test
    public void firstBrokerLoginReachesEssentialLevelInPostBrokerLoginFlow() {
        configurePostBrokerLoginFlow(true);

        loginFormWithEssentialAcr("2").open();
        logInWithBroker(bc);
        updateAccountInformation();

        assertLoggedInWithAcr("2");
    }

    @Test
    public void firstBrokerLoginNotReachingEssentialLevelFails() {
        configurePostBrokerLoginFlow(false);

        loginFormWithEssentialAcr("2").open();
        logInWithBroker(bc);
        updateAccountInformation();

        // the post broker login flow finishes the authentication at level 1
        assertErrorPage("Authentication requirements not fulfilled");
    }

    @Test
    public void existingUserReachesEssentialLevelInPostBrokerLoginFlow() {
        configurePostBrokerLoginFlow(true);
        loginUser();
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        loginFormWithEssentialAcr("2").open();
        logInWithBroker(bc);

        assertLoggedInWithAcr("2");
    }

    /**
     * Post broker login flow of the identity provider which reaches level 2 through a level condition, or which records no level.
     */
    private void configurePostBrokerLoginFlow(boolean withLevel2Condition) {
        String idpAlias = bc.getIDPAlias();
        testingClient.server(bc.consumerRealmName()).run(session -> {
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

            IdentityProviderModel idp = session.identityProviders().getByAlias(idpAlias);
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

        oauth.realm(bc.consumerRealmName());
        oauth.client(KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_CLIENT_ID, KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        return oauth.loginForm().claims(claims);
    }

    private void assertLoggedInWithAcr(String expectedAcr) {
        appPage.assertCurrent();
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        assertTrue("Login should succeed, but got error: " + response.getError(), response.isSuccess());
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(response.getCode());
        assertEquals(expectedAcr, oauth.parseToken(tokenResponse.getIdToken(), IDToken.class).getAcr());
    }
}
