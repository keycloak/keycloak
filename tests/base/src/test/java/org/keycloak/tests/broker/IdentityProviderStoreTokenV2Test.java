package org.keycloak.tests.broker;


import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyMode;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.condition.IdentityProviderConditionFactory;
import org.keycloak.services.clientpolicy.executor.RejectRequestExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = IdentityProviderStoreTokenV2Test.IdentityBrokeringAPIV2ServerConfig.class)
public class IdentityProviderStoreTokenV2Test implements InterfaceIdentityProviderStoreTokenV2Test, InterfaceOIDCIdentityProviderStoreTokenTest {

    @InjectRealm(config = IdpRealmConfig.class)
    ManagedRealm realm;

    @InjectRealm(ref = "external-realm", config = ExternalRealmConfig.class)
    ManagedRealm externalRealm;

    @InjectOAuthClient(ref = "external-realm", realmRef = "external-realm", config = TestClientConfig.class)
    OAuthClient oauthExternal;

    @InjectOAuthClient(config = ExternalClientConfig.class)
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Override
    public ManagedRealm getRealm() {
        return realm;
    }

    @Override
    public ManagedRealm getExternalRealm() {
        return externalRealm;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oauth;
    }

    @Override
    public LoginPage getLoginPage() {
        return loginPage;
    }

    @Override
    public RunOnServerClient getRunOnServer() {
        return runOnServer;
    }

    @Override
    public TimeOffSet getTimeOffSet() {
        return timeOffSet;
    }

    @Override
    public OAuthClient getOauthClientExternal() {
        return oauthExternal;
    }

    @Override
    public void checkSuccessfulTokenResponse(AbstractHttpResponse response) {
        Assertions.assertInstanceOf(AccessTokenResponse.class, response);
        AccessTokenResponse externalTokens = (AccessTokenResponse) response;
        Assertions.assertNotNull(externalTokens.getAccessToken());
        Assertions.assertNull(externalTokens.getRefreshToken());
        Assertions.assertNull(externalTokens.getIdToken());
        UserInfoResponse userInfoResponse = getOauthClientExternal().userInfoRequest(externalTokens.getAccessToken()).send();
        Assertions.assertEquals(200, userInfoResponse.getStatusCode());
        Assertions.assertNotNull(userInfoResponse.getUserInfo().getPreferredUsername());
    }

    @Test
    public void testBrokerClientNotCreatedWhenV1Disabled() {
        List<ClientRepresentation> brokerClients = realm.admin().clients()
                .findByClientId(Constants.BROKER_SERVICE_CLIENT_ID);
        Assertions.assertTrue(brokerClients.isEmpty(), "Broker client should not exist when identity-broker-api:v1 is disabled");
    }

    @Test
    public void testClientPoliciesWithIDPCondition() {
        realm.updateClientProfile(List.of(ClientProfileBuilder.create()
                .name("executor")
                .description("executor description")
                .executor(RejectRequestExecutorFactory.PROVIDER_ID, null)
                .build()));

        realm.updateClientPolicy(List.of(ClientPolicyBuilder.create()
                .name("policy")
                .description("description of policy")
                .condition(IdentityProviderConditionFactory.PROVIDER_ID, ClientPolicyBuilder.identityProviderConditionConfiguration(true, "allowed-idp"))
                .profile("executor")
                .build()));

        OAuthClient oauth = getOAuthClient();
        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        AbstractHttpResponse externalTokens = doFetchExternalIdpToken(tokenResponse.getAccessToken());
        Assertions.assertEquals(400, externalTokens.getStatusCode());
        Assertions.assertEquals("Request not allowed", externalTokens.getErrorDescription());

        //update without cleanup
        ClientPoliciesRepresentation policiesToUpdate = realm.admin().clientPoliciesPoliciesResource().getPolicies();
        policiesToUpdate.setPolicies(List.of(ClientPolicyBuilder.create()
                .name("policy")
                .description("description of policy")
                .condition(IdentityProviderConditionFactory.PROVIDER_ID, ClientPolicyBuilder.identityProviderConditionConfiguration(true, IDP_ALIAS))
                .profile("executor")
                .build()));
        realm.admin().clientPoliciesPoliciesResource().updatePolicies(policiesToUpdate);

        externalTokens = doFetchExternalIdpToken(tokenResponse.getAccessToken());
        Assertions.assertTrue(externalTokens.isSuccess());
        checkSuccessfulTokenResponse(externalTokens);
    }

    // Configure client policies in a way that call identity-brokering API for retrieve external tokens of specified IDP is allowed just if internal token has specified scope (EG. 'phone')
    @Test
    public void testClientPoliciesWithIDPAndScopeCondition() {
        realm.updateClientProfile(List.of(ClientProfileBuilder.create()
                .name("reject-profile")
                .description("Profile to reject request")
                .executor(RejectRequestExecutorFactory.PROVIDER_ID, null)
                .build()));

        realm.updateClientPolicy(List.of(ClientPolicyBuilder.create()
                .name("client policy")
                .description("Policy to allow to call IDP brokering API for tokens from 'allowed-idp' just if incoming token has scope 'phone'")
                .mode(ClientPolicyMode.STRICT)
                .condition(IdentityProviderConditionFactory.PROVIDER_ID, ClientPolicyBuilder.identityProviderConditionConfiguration(false, IDP_ALIAS))
                .condition(ClientScopesConditionFactory.PROVIDER_ID, ClientPolicyBuilder.clientScopesConditionConfiguration(true, ClientScopesConditionFactory.ANY,"phone"))
                .profile("reject-profile")
                .build()));

        // Test calling the brokering API with the token without scope 'phone'. Request should be rejected
        OAuthClient oauth = getOAuthClient();
        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        AbstractHttpResponse externalTokens = doFetchExternalIdpToken(tokenResponse.getAccessToken());
        Assertions.assertEquals(400, externalTokens.getStatusCode());
        Assertions.assertEquals("Request not allowed", externalTokens.getErrorDescription());

        logout();

        // Test calling the brokering API with the token with scope 'phone'. Request should be allowed
        oauth.scope("phone");
        oauth.openLoginForm();
        loginWithIdP();

        tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        externalTokens = doFetchExternalIdpToken(tokenResponse.getAccessToken());
        Assertions.assertTrue(externalTokens.isSuccess());
        checkSuccessfulTokenResponse(externalTokens);
    }

    static class IdentityBrokeringAPIV2ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.IDENTITY_BROKERING_API_V2);
        }
    }
}
