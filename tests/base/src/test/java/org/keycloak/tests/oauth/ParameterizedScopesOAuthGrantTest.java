package org.keycloak.tests.oauth;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantTypeFactory;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.ClientNotificationEndpointRequest;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.CibaProvider;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectCibaProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.ciba.AuthenticationRequestAcknowledgement;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@DatabaseTest
@KeycloakIntegrationTest(config = ParameterizedScopesOAuthGrantTest.ParameterizedScopesServerConfig.class)
public class ParameterizedScopesOAuthGrantTest {

    private static final String THIRD_PARTY_APP = "third-party";
    private static final String DEFAULT_USERNAME = "test-user@localhost";
    private static final String DEFAULT_PASSWORD = "password";

    private static String PARAMETERIZED_SCOPE_ID;

    @InjectRealm(config = ParameterizedScopesRealmConfig.class)
    ManagedRealm realm;

    @InjectClient(config = ThirdPartyClient.class)
    ManagedClient thirdParty;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    protected Events events;

    @InjectCibaProvider
    protected CibaProvider ciba;

    @InjectPage
    protected OAuthGrantPage grantPage;

    @TestSetup
    public void configureTestRealm() {
        ClientScopeRepresentation parameterizedScope = ClientScopeBuilder.create()
                .name("foo-parameter-scope")
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .attribute(ClientScopeModel.IS_PARAMETERIZED_SCOPE, Boolean.TRUE.toString())
                .attribute(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, Boolean.TRUE.toString())
                .attribute(ClientScopeModel.IS_ALWAYS_CONSENT, Boolean.FALSE.toString())
                .attribute(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE, "string")
                .build();
        PARAMETERIZED_SCOPE_ID = ApiUtil.getCreatedId(realm.admin().clientScopes().create(parameterizedScope));
        thirdParty.admin().addOptionalClientScope(PARAMETERIZED_SCOPE_ID);
    }

    @AfterEach
    public void afterEach() {
        // logout user and revoke consents if present
        AccountHelper.logout(realm.admin(), DEFAULT_USERNAME);
        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(realm.admin(), DEFAULT_USERNAME);
        if (userConsents.stream().anyMatch(m -> THIRD_PARTY_APP.equals(m.get("clientId")))) {
            AccountHelper.revokeConsents(realm.admin(), DEFAULT_USERNAME, THIRD_PARTY_APP);
        }
    }

    @Test
    public void oauthGrantParameterizedScopeParamRequired() {
        realm.updateClientScope(PARAMETERIZED_SCOPE_ID, s -> s.attribute(ClientScopeModel.CONSENT_SCREEN_TEXT, ""));

        // login using the parameterized scope
        oauth.client(THIRD_PARTY_APP, "password");
        oauth.scope("foo-parameter-scope:param1");
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        Assertions.assertTrue(grants.contains("foo-parameter-scope: param1"));
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId(THIRD_PARTY_APP)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.USERNAME, DEFAULT_USERNAME)
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);

        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN)
                .clientId(THIRD_PARTY_APP)
                .sessionId(loginEvent.getSessionId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID));

        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(realm.admin(), DEFAULT_USERNAME);
        Assertions.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().anyMatch(p -> p.equals("foo-parameter-scope:param1")));

        res = oauth.doRefreshTokenRequest(res.getRefreshToken());
        MatcherAssert.assertThat(List.of(res.getScope().split(" ")), Matchers.hasItems("foo-parameter-scope:param1"));

        oauth.logoutForm().idTokenHint(res.getIdToken()).open();

        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGOUT)
                .sessionId(loginEvent.getSessionId())
                .clientId(THIRD_PARTY_APP)
                .withoutDetails(Details.REDIRECT_URI);

        // login again with the same param and a new one, only param2 should be requested
        oauth.scope("foo-parameter-scope:param1 foo-parameter-scope:param2");
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        grants = grantPage.getDisplayedGrants();
        Assertions.assertEquals(1, grants.size());
        Assertions.assertTrue(grants.contains("foo-parameter-scope: param2"));
        grantPage.accept();

        loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent)
                .clientId(THIRD_PARTY_APP)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        code = oauth.parseLoginResponse().getCode();
        res = oauth.doAccessTokenRequest(code);

        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN)
                .clientId(THIRD_PARTY_APP)
                .sessionId(loginEvent.getSessionId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID));

        userConsents = AccountHelper.getUserConsents(realm.admin(), DEFAULT_USERNAME);
        Assertions.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().anyMatch(p -> p.equals("foo-parameter-scope:param1")));
        Assertions.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().anyMatch(p -> p.equals("foo-parameter-scope:param2")));

        res = oauth.doRefreshTokenRequest(res.getRefreshToken());
        MatcherAssert.assertThat(List.of(res.getScope().split(" ")), Matchers.hasItems("foo-parameter-scope:param1", "foo-parameter-scope:param2"));

        res = oauth.scope("foo-parameter-scope:param2").doRefreshTokenRequest(res.getRefreshToken());
        MatcherAssert.assertThat(List.of(res.getScope().split(" ")), Matchers.not(Matchers.hasItems("foo-parameter-scope:param1")));
        MatcherAssert.assertThat(List.of(res.getScope().split(" ")), Matchers.hasItems("foo-parameter-scope:param2"));

        oauth.logoutForm().idTokenHint(res.getIdToken()).open();

        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGOUT)
                .sessionId(loginEvent.getSessionId())
                .clientId(THIRD_PARTY_APP)
                .withoutDetails(Details.REDIRECT_URI);

        // login again with the same two params
        oauth.scope("foo-parameter-scope:param1 foo-parameter-scope:param2");
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);

        EventAssertion.expectLoginSuccess(events.poll())
                .clientId(THIRD_PARTY_APP)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.CONSENT, Details.CONSENT_VALUE_PERSISTED_CONSENT);
    }

    @Test
    public void oauthGrantParameterizedScopeAlwaysConsent() {
        realm.updateClientScope(PARAMETERIZED_SCOPE_ID, s -> s
                .attribute(ClientScopeModel.CONSENT_SCREEN_TEXT, "")
                .attribute(ClientScopeModel.IS_ALWAYS_CONSENT, Boolean.TRUE.toString()));

        // login using the parameterized scope
        oauth.client(THIRD_PARTY_APP, "password");
        oauth.scope("foo-parameter-scope:param1");
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        Assertions.assertTrue(grants.contains("foo-parameter-scope: param1"));
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId(THIRD_PARTY_APP)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.USERNAME, DEFAULT_USERNAME)
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);

        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN)
                .clientId(THIRD_PARTY_APP)
                .sessionId(loginEvent.getSessionId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID));

        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(realm.admin(), DEFAULT_USERNAME);
        Assertions.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().noneMatch(p -> p.equals("foo-parameter-scope:param1")));

        res = oauth.doRefreshTokenRequest(res.getRefreshToken());
        MatcherAssert.assertThat(List.of(res.getScope().split(" ")), Matchers.hasItems("foo-parameter-scope:param1"));

        oauth.logoutForm().idTokenHint(res.getIdToken()).open();

        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGOUT)
                .sessionId(loginEvent.getSessionId())
                .clientId(THIRD_PARTY_APP)
                .withoutDetails(Details.REDIRECT_URI);

        // login again with the same param and a new one, both should be requested
        oauth.scope("foo-parameter-scope:param1 foo-parameter-scope:param2");
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        grants = grantPage.getDisplayedGrants();
        Assertions.assertEquals(2, grants.size());
        Assertions.assertTrue(grants.contains("foo-parameter-scope: param1"));
        Assertions.assertTrue(grants.contains("foo-parameter-scope: param2"));
        grantPage.accept();

        loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent)
                .clientId(THIRD_PARTY_APP)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        code = oauth.parseLoginResponse().getCode();
        res = oauth.doAccessTokenRequest(code);

        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN)
                .clientId(THIRD_PARTY_APP)
                .sessionId(loginEvent.getSessionId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID));

        userConsents = AccountHelper.getUserConsents(realm.admin(), DEFAULT_USERNAME);
        Assertions.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().noneMatch(p -> p.equals("foo-parameter-scope:param1")));
        Assertions.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().noneMatch(p -> p.equals("foo-parameter-scope:param2")));

        res = oauth.doRefreshTokenRequest(res.getRefreshToken());
        MatcherAssert.assertThat(List.of(res.getScope().split(" ")), Matchers.hasItems("foo-parameter-scope:param1", "foo-parameter-scope:param2"));

        res = oauth.scope("foo-parameter-scope:param2").doRefreshTokenRequest(res.getRefreshToken());
        MatcherAssert.assertThat(List.of(res.getScope().split(" ")), Matchers.not(Matchers.hasItems("foo-parameter-scope:param1")));
        MatcherAssert.assertThat(List.of(res.getScope().split(" ")), Matchers.hasItems("foo-parameter-scope:param2"));
    }

    @Test
    public void oauthGrantParameterizedScopeAlwaysConsentFailsOnNonConsentClient() {
        // set scope to always consent and update the third party app to not be consent required
        realm.updateClientScope(PARAMETERIZED_SCOPE_ID, s -> s.attribute(ClientScopeModel.IS_ALWAYS_CONSENT, Boolean.TRUE.toString()));
        ClientResource clientRes = realm.admin().clients().get(thirdParty.getId());
        ClientRepresentation clientRep = clientRes.toRepresentation();
        clientRep.setConsentRequired(Boolean.FALSE);
        clientRes.update(clientRep);
        realm.cleanup().add(r -> {
            clientRep.setConsentRequired(Boolean.TRUE);
            r.clients().get(thirdParty.getId()).update(clientRep);
        });

        oauth.client(THIRD_PARTY_APP, "password");
        oauth.scope("foo-parameter-scope:param1");
        oauth.openLoginForm();
        AuthorizationEndpointResponse res = oauth.parseLoginResponse();
        Assertions.assertEquals(OAuthErrorException.INVALID_SCOPE, res.getError());
        MatcherAssert.assertThat(res.getErrorDescription(), Matchers.startsWith("Invalid scopes:"));
    }

    @Test
    public void oauthGrantParameterizedScopeParamRequiredWithConsentText() {
        realm.updateClientScope(PARAMETERIZED_SCOPE_ID, s -> s.attribute(
                ClientScopeModel.CONSENT_SCREEN_TEXT, "Parameterized scope with parameter {0}"));

        oauth.client(THIRD_PARTY_APP, "password");
        oauth.scope("foo-parameter-scope:one");
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        Assertions.assertTrue(grants.contains("Parameterized scope with parameter one"));
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId(THIRD_PARTY_APP)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
    }

    @Test
    public void oauthGrantParameterizedScopeParamRequiredWithConsentTextKey() {
        realm.admin().localization().saveRealmLocalizationText("en", "parameterConsentText", "Parameterized scope with parameter {0}");
        realm.updateClientScope(PARAMETERIZED_SCOPE_ID, s -> s
                .attribute(ClientScopeModel.CONSENT_SCREEN_TEXT, "${parameterConsentText}")
                .attribute(ClientScopeModel.IS_ALWAYS_CONSENT, Boolean.FALSE.toString()));

        oauth.client(THIRD_PARTY_APP, "password");
        oauth.scope("foo-parameter-scope:two");
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        Assertions.assertTrue(grants.contains("Parameterized scope with parameter two"));
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId(THIRD_PARTY_APP)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        realm.admin().localization().deleteRealmLocalizationText("en", "parameterConsentText");
    }

    @Test
    public void cibaGrant() throws Exception {
        // client Backchannel Authentication Request
        oauth.client(THIRD_PARTY_APP, "password");
        oauth.scope("foo-parameter-scope:param1");
        AuthenticationRequestAcknowledgement response = oauth.ciba().backchannelAuthenticationRequest(DEFAULT_USERNAME)
                .bindingMessage("asdfghjkl")
                .clientNotificationToken("client-notification-token")
                .additionalParams(Map.of("user_device", "mobile"))
                .send();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getAuthReqId());

        // client Authentication Channel Request
        CibaProvider.CibaAuthenticationChannelRequest clientAuthenticationChannelReq = ciba.getAuthChannel("asdfghjkl");
        Assertions.assertTrue(clientAuthenticationChannelReq.getRequest().getConsentRequired());
        MatcherAssert.assertThat(List.of(clientAuthenticationChannelReq.getRequest().getScope().split(" ")), Matchers.hasItems("foo-parameter-scope:param1"));

        // check ping is not still there
        ClientNotificationEndpointRequest pushedClientNotification = ciba.getPushedCibaClientNotification("client-notification-token");
        Assertions.assertNull(pushedClientNotification.getAuthReqId());

        // client Authentication Channel completed
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                oauth.ciba().doAuthenticationChannelCallback(clientAuthenticationChannelReq.getBearerToken(), AuthenticationChannelResponse.Status.SUCCEED));

        // Check clientNotification exists now for our authReqId
        pushedClientNotification = ciba.getPushedCibaClientNotification("client-notification-token");
        Assertions.assertEquals(pushedClientNotification.getAuthReqId(), response.getAuthReqId());

        // client Token Request should be OK now
        AccessTokenResponse tokenRes = oauth.ciba().doBackchannelAuthenticationTokenRequest(response.getAuthReqId());
        Assertions.assertTrue(tokenRes.isSuccess());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.AUTHREQID_TO_TOKEN)
                .hasSessionId()
                .hasIpAddress()
                .hasCodeId()
                .hasUserId()
                .clientId(THIRD_PARTY_APP)
                .hasTokenId(Details.REFRESH_TOKEN_ID)
                .hasAccessTokenId(CibaGrantTypeFactory.GRANT_SHORTCUT)
                .details(Details.USERNAME, DEFAULT_USERNAME)
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
        MatcherAssert.assertThat(List.of(tokenRes.getScope().split(" ")), Matchers.hasItems("foo-parameter-scope:param1"));

        // assert consent is granted
        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(realm.admin(), DEFAULT_USERNAME);
        Assertions.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().anyMatch(p -> p.equals("foo-parameter-scope:param1")));

        // do a refresh
        tokenRes = oauth.doRefreshTokenRequest(tokenRes.getRefreshToken());
        MatcherAssert.assertThat(List.of(tokenRes.getScope().split(" ")), Matchers.hasItems("foo-parameter-scope:param1"));
    }

    @Test
    public void cibaGrantAlwaysConsentScope() throws Exception {
        realm.updateClientScope(PARAMETERIZED_SCOPE_ID, s -> s.attribute(ClientScopeModel.IS_ALWAYS_CONSENT, Boolean.TRUE.toString()));

        // client Backchannel Authentication Request
        oauth.client(THIRD_PARTY_APP, "password");
        oauth.scope("foo-parameter-scope:param1");
        AuthenticationRequestAcknowledgement response = oauth.ciba().backchannelAuthenticationRequest(DEFAULT_USERNAME)
                .bindingMessage("asdfghjkl")
                .clientNotificationToken("client-notification-token")
                .additionalParams(Map.of("user_device", "mobile"))
                .send();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getAuthReqId());

        // client Authentication Channel Request
        CibaProvider.CibaAuthenticationChannelRequest clientAuthenticationChannelReq = ciba.getAuthChannel("asdfghjkl");
        Assertions.assertTrue(clientAuthenticationChannelReq.getRequest().getConsentRequired());
        MatcherAssert.assertThat(List.of(clientAuthenticationChannelReq.getRequest().getScope().split(" ")), Matchers.hasItems("foo-parameter-scope:param1"));

        // check ping is not still there
        ClientNotificationEndpointRequest pushedClientNotification = ciba.getPushedCibaClientNotification("client-notification-token");
        Assertions.assertNull(pushedClientNotification.getAuthReqId());

        // client Authentication Channel completed
        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                oauth.ciba().doAuthenticationChannelCallback(clientAuthenticationChannelReq.getBearerToken(), AuthenticationChannelResponse.Status.SUCCEED));

        // Check clientNotification exists now for our authReqId
        pushedClientNotification = ciba.getPushedCibaClientNotification("client-notification-token");
        Assertions.assertEquals(pushedClientNotification.getAuthReqId(), response.getAuthReqId());

        // client Token Request should be OK now
        AccessTokenResponse tokenRes = oauth.ciba().doBackchannelAuthenticationTokenRequest(response.getAuthReqId());
        Assertions.assertTrue(tokenRes.isSuccess());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.AUTHREQID_TO_TOKEN)
                .hasSessionId()
                .hasIpAddress()
                .hasCodeId()
                .hasUserId()
                .clientId(THIRD_PARTY_APP)
                .hasTokenId(Details.REFRESH_TOKEN_ID)
                .hasAccessTokenId(CibaGrantTypeFactory.GRANT_SHORTCUT)
                .details(Details.USERNAME, DEFAULT_USERNAME)
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
        MatcherAssert.assertThat(List.of(tokenRes.getScope().split(" ")), Matchers.hasItems("foo-parameter-scope:param1"));

        // assert consent is not granted as it always consent
        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(realm.admin(), DEFAULT_USERNAME);
        Assertions.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().noneMatch(p -> p.equals("foo-parameter-scope:param1")));

        // do a refresh
        tokenRes = oauth.doRefreshTokenRequest(tokenRes.getRefreshToken());
        MatcherAssert.assertThat(List.of(tokenRes.getScope().split(" ")), Matchers.hasItems("foo-parameter-scope:param1"));
    }

    @Test
    public void oauthGrantCustomRegexScopeValidation() {
        ClientScopeRepresentation customScope = ClientScopeBuilder.create()
                .name("custom-regex-scope")
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .attribute(ClientScopeModel.IS_PARAMETERIZED_SCOPE, Boolean.TRUE.toString())
                .attribute(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE, "custom")
                .attribute(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, "[a-z]+")
                .build();
        String customScopeId = ApiUtil.getCreatedId(realm.admin().clientScopes().create(customScope));
        realm.cleanup().add(r -> {
            r.clients().get(thirdParty.getId()).removeOptionalClientScope(customScopeId);
            r.clientScopes().get(customScopeId).remove();
        });
        thirdParty.admin().addOptionalClientScope(customScopeId);

        oauth.client(THIRD_PARTY_APP, "password");

        // non-matching parameter should be rejected
        oauth.scope("custom-regex-scope:123");
        oauth.openLoginForm();
        Assertions.assertEquals(OAuthErrorException.INVALID_SCOPE, oauth.parseLoginResponse().getError());

        // matching parameter should be accepted
        oauth.scope("custom-regex-scope:abc");
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        grantPage.accept();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        MatcherAssert.assertThat(List.of(res.getScope().split(" ")), Matchers.hasItems("custom-regex-scope:abc"));
    }

    public static class ParameterizedScopesServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES)
                    .option("spi-ciba-auth-channel-ciba-http-auth-channel-http-authentication-channel-uri",
                            "http://localhost:8500/ciba/request-authentication-channel");
        }
    }

    public static class ParameterizedScopesRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.users(UserBuilder.create(DEFAULT_USERNAME)
                    .email(DEFAULT_USERNAME)
                    .name("Test", "User")
                    .emailVerified(true)
                    .password(DEFAULT_PASSWORD)
                    .enabled(true));
            return realm;
        }
    }

    public static class ThirdPartyClient implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId(THIRD_PARTY_APP)
                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                    .consentRequired(Boolean.TRUE)
                    .redirectUris("*")
                    .secret("password")
                    .attribute(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, "ping")
                    .attribute(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT, "http://localhost:8500/ciba/push-ciba-client-notification")
                    .attribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.TRUE.toString());
        }
    }
}
