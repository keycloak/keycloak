package org.keycloak.tests.oauth;

import java.util.List;
import java.util.Map;

import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
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
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = DynamicScopesOAuthGrantTest.DynamicScopesServerConfig.class)
public class DynamicScopesOAuthGrantTest {

    private static final String THIRD_PARTY_APP = "third-party";
    private static final String DEFAULT_USERNAME = "test-user@localhost";
    private static final String DEFAULT_PASSWORD = "password";

    private static String DYNAMIC_SCOPE_ID;

    @InjectRealm(config = DynamicScopesRealmConfig.class)
    ManagedRealm realm;

    @InjectClient(config = ThirdPartyClient.class)
    ManagedClient thirdParty;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    protected Events events;

    @InjectPage
    protected OAuthGrantPage grantPage;

    @TestSetup
    public void configureTestRealm() {
        ClientScopeRepresentation dynamicScope = ClientScopeBuilder.create()
                .name("foo-dynamic-scope")
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .attribute(ClientScopeModel.IS_DYNAMIC_SCOPE, Boolean.TRUE.toString())
                .attribute(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "foo-dynamic-scope:*")
                .build();
        DYNAMIC_SCOPE_ID = ApiUtil.getCreatedId(realm.admin().clientScopes().create(dynamicScope));
        thirdParty.admin().addOptionalClientScope(DYNAMIC_SCOPE_ID);
    }

    @BeforeEach
    public void beforeEach() {
        // logout user and revoke consents if present
        AccountHelper.logout(realm.admin(), DEFAULT_USERNAME);
        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(realm.admin(), DEFAULT_USERNAME);
        if (userConsents.stream().anyMatch(m -> THIRD_PARTY_APP.equals(m.get("clientId")))) {
            AccountHelper.revokeConsents(realm.admin(), DEFAULT_USERNAME, THIRD_PARTY_APP);
        }
    }

    @Test
    public void oauthGrantDynamicScopeParamRequired() {
        realm.updateClientScope(DYNAMIC_SCOPE_ID, s -> s.attribute(ClientScopeModel.CONSENT_SCREEN_TEXT, ""));

        // login using the dynamic scope
        oauth.client(THIRD_PARTY_APP, "password");
        oauth.scope("foo-dynamic-scope:withparam");
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        Assertions.assertTrue(grants.contains("foo-dynamic-scope: withparam"));
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId(THIRD_PARTY_APP)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);

        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN)
                .clientId(THIRD_PARTY_APP)
                .sessionId(loginEvent.getSessionId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID));

        oauth.logoutForm().idTokenHint(res.getIdToken()).open();

        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGOUT)
                .sessionId(loginEvent.getSessionId())
                .clientId(THIRD_PARTY_APP)
                .withoutDetails(Details.REDIRECT_URI);

        // login again to check whether the Dynamic scope and only the dynamic scope is requested again
        oauth.scope("foo-dynamic-scope:withparam");
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        grants = grantPage.getDisplayedGrants();
        Assertions.assertEquals(1, grants.size());
        Assertions.assertTrue(grants.contains("foo-dynamic-scope: withparam"));
        grantPage.accept();

        EventAssertion.expectLoginSuccess(events.poll())
                .clientId(THIRD_PARTY_APP)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
    }

    @Test
    public void oauthGrantDynamicScopeParamRequiredWithConsentText() {
        realm.updateClientScope(DYNAMIC_SCOPE_ID, s -> s.attribute(
                ClientScopeModel.CONSENT_SCREEN_TEXT, "Dynamic scope with parameter {0}"));

        oauth.client(THIRD_PARTY_APP, "password");
        oauth.scope("foo-dynamic-scope:one");
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        Assertions.assertTrue(grants.contains("Dynamic scope with parameter one"));
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId(THIRD_PARTY_APP)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
    }

    @Test
    public void oauthGrantDynamicScopeParamRequiredWithConsentTextKey() {
        realm.admin().localization().saveRealmLocalizationText("en", "dynamicConsentText", "Dynamic scope with parameter {0}");
        realm.updateClientScope(DYNAMIC_SCOPE_ID, s -> s.attribute(
                ClientScopeModel.CONSENT_SCREEN_TEXT, "${dynamicConsentText}"));

        oauth.client(THIRD_PARTY_APP, "password");
        oauth.scope("foo-dynamic-scope:two");
        oauth.openLoginForm();
        oauth.fillLoginForm(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        Assertions.assertTrue(grants.contains("Dynamic scope with parameter two"));
        grantPage.accept();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN)
                .clientId(THIRD_PARTY_APP)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .details(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        realm.admin().localization().deleteRealmLocalizationText("en", "dynamicConsentText");
    }

    public static class DynamicScopesServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.DYNAMIC_SCOPES);
        }
    }

    public static class DynamicScopesRealmConfig implements RealmConfig {
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
                    .secret("password");
        }
    }
}
