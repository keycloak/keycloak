package org.keycloak.testsuite.login;

import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;
import static org.keycloak.testsuite.forms.BrowserFlowTest.revertFlows;

import org.apache.http.client.utils.URLEncodedUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.limit.UserSessionsLimiterAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.TokenUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AuthServerContainerExclude(REMOTE)
public class UserSessionsLimiterAuthenticatorTest extends AbstractTestRealmKeycloakTest {

    public static final String USERNAME = "UserSessionsLimiterAuthenticatorTest";
    public static final String PASSWORD = "Test123!";

    @ArquillianResource
    protected OAuthClient oauth;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AppPage appPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    private String userId;

    @Before
    public void init() {
        userId = createUser(TEST_REALM_NAME, USERNAME, PASSWORD);
        ApiUtil.assignRealmRoles(testRealm(), userId, Constants.OFFLINE_ACCESS_ROLE);
    }

    @After
    public void cleanUp() {
        ApiUtil.removeUserByUsername(testRealm(), USERNAME);
    }

    @Test
    public void testLimitUserSessions() throws Exception {
        ClientResource authorizationClient = ApiUtil.findClientResourceByClientId(testRealm(), oauth.getClientId());
        String clientSecret = authorizationClient.getSecret().getValue();

        final String flowAlias = "browser - session limit 2";

        Map<String, String> limitConfigMap = new HashMap<>();
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_MAX_SESSIONS, "2");
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_SORT,
                UserSessionsLimiterAuthenticatorFactory.ConfAttrSortOptions.STARTED.toString());
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_OFFLINE_SESSIONS, "false");

        configureBrowserFlowWithSessionLimit(flowAlias, limitConfigMap);
        try {
            performLogin(clientSecret);

            EventRepresentation firstLogin =
                    events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
            events.expectCodeToToken(firstLogin.getDetails().get("code_id"), firstLogin.getSessionId()).user(userId)
                    .assertEvent();

            List<UserSessionRepresentation> userSessionsAfterFirstLogin =
                    authorizationClient.getUserSessions(0, 100);
            Assert.assertEquals(1, userSessionsAfterFirstLogin.size());

            // wait a sec to make sure that start time of first session is before second one
            Thread.sleep(1000);

            performLogin(clientSecret);

            EventRepresentation secondLogin =
                    events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
            events.expectCodeToToken(secondLogin.getDetails().get("code_id"), secondLogin.getSessionId()).user(userId)
                    .assertEvent();

            List<UserSessionRepresentation> userSessionsAfterSecondLogin =
                    authorizationClient.getUserSessions(0, 100);
            Assert.assertEquals(2, userSessionsAfterSecondLogin.size());

            performLogin(clientSecret);

            events.expectLogout(firstLogin.getSessionId())
                    .user(userId)
                    .detail(Details.REASON, "session_limit_reached")
                    .removeDetail(Details.REDIRECT_URI)
                    .assertEvent();

            EventRepresentation thirdLogin =
                    events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
            events.expectCodeToToken(thirdLogin.getDetails().get("code_id"), thirdLogin.getSessionId()).user(userId)
                    .assertEvent();

            List<UserSessionRepresentation> userSessionsAfterThirdLogin =
                    authorizationClient.getUserSessions(0, 100);
            Assert.assertEquals(2, userSessionsAfterThirdLogin.size());
        } finally {
            revertFlows(testRealm(), flowAlias);
        }
    }

    @Test
    public void testCleanUserSessionAfterLimitActivation() throws Exception {
        ClientResource authorizationClient = ApiUtil.findClientResourceByClientId(testRealm(), oauth.getClientId());
        String clientSecret = authorizationClient.getSecret().getValue();

        final String flowAlias = "browser - session limit 1";

        Map<String, String> limitConfigMap = new HashMap<>();
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_MAX_SESSIONS, "1");
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_SORT,
                UserSessionsLimiterAuthenticatorFactory.ConfAttrSortOptions.STARTED.toString());
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_OFFLINE_SESSIONS, "false");

        performLogin(clientSecret);

        EventRepresentation firstLogin =
                events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
        events.expectCodeToToken(firstLogin.getDetails().get("code_id"), firstLogin.getSessionId()).user(userId)
                .assertEvent();

        List<UserSessionRepresentation> userSessionsAfterFirstLogin =
                authorizationClient.getUserSessions(0, 100);
        Assert.assertEquals(1, userSessionsAfterFirstLogin.size());

        // wait a sec to make sure that start time of first session is before second one
        Thread.sleep(1000);

        performLogin(clientSecret);

        EventRepresentation secondLogin =
                events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
        events.expectCodeToToken(secondLogin.getDetails().get("code_id"), secondLogin.getSessionId()).user(userId)
                .assertEvent();

        List<UserSessionRepresentation> userSessionsAfterSecondLogin =
                authorizationClient.getUserSessions(0, 100);
        Assert.assertEquals(2, userSessionsAfterSecondLogin.size());

        configureBrowserFlowWithSessionLimit(flowAlias, limitConfigMap);
        try {
            performLogin(clientSecret);

            events.expectLogout(secondLogin.getSessionId())
                    .user(userId)
                    .detail(Details.REASON, "session_limit_reached")
                    .removeDetail(Details.REDIRECT_URI)
                    .assertEvent();

            events.expectLogout(firstLogin.getSessionId())
                    .user(userId)
                    .detail(Details.REASON, "session_limit_reached")
                    .removeDetail(Details.REDIRECT_URI)
                    .assertEvent();

            EventRepresentation thirdLogin =
                    events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
            events.expectCodeToToken(thirdLogin.getDetails().get("code_id"), thirdLogin.getSessionId()).user(userId)
                    .assertEvent();

            List<UserSessionRepresentation> userSessionsAfterThirdLogin =
                    authorizationClient.getUserSessions(0, 100);
            Assert.assertEquals(1, userSessionsAfterThirdLogin.size());
        } finally {
            revertFlows(testRealm(), flowAlias);
        }
    }

    @Test
    public void testOfflineSessionsLimit() throws Exception {
        ClientResource authorizationClient = ApiUtil.findClientResourceByClientId(testRealm(), oauth.getClientId());
        String clientSecret = authorizationClient.getSecret().getValue();

        String offlineScopeId =
                ApiUtil.findClientScopeByName(testRealm(), OAuth2Constants.OFFLINE_ACCESS).toRepresentation().getId();

        authorizationClient.removeOptionalClientScope(offlineScopeId);
        authorizationClient.addDefaultClientScope(offlineScopeId);

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);

        final String flowAlias = "browser - offline session limit 2";

        Map<String, String> limitConfigMap = new HashMap<>();
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_MAX_SESSIONS, "100");
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_SORT,
                UserSessionsLimiterAuthenticatorFactory.ConfAttrSortOptions.STARTED.toString());
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_OFFLINE_SESSIONS, "true");
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_MAX_OFFLINE_SESSIONS, "2");
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_OFFLINE_SORT,
                UserSessionsLimiterAuthenticatorFactory.ConfAttrSortOptions.STARTED.toString());

        configureBrowserFlowWithSessionLimit(flowAlias, limitConfigMap);
        try {
            performLogin(clientSecret);

            EventRepresentation firstLogin =
                    events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
            events.expectCodeToToken(firstLogin.getDetails().get("code_id"), firstLogin.getSessionId()).user(userId)
                    .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE).assertEvent();

            List<UserSessionRepresentation> offlineUserSessionsAfterFirstLogin =
                    authorizationClient.getOfflineUserSessions(0, 100);
            Assert.assertEquals(1, offlineUserSessionsAfterFirstLogin.size());

            // wait a sec to make sure that start time of first session is before second one
            Thread.sleep(1000);

            performLogin(clientSecret);

            EventRepresentation secondLogin =
                    events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
            events.expectCodeToToken(secondLogin.getDetails().get("code_id"), secondLogin.getSessionId()).user(userId)
                    .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE).assertEvent();

            List<UserSessionRepresentation> offlineUserSessionsAfterSecondLogin =
                    authorizationClient.getOfflineUserSessions(0, 100);
            Assert.assertEquals(2, offlineUserSessionsAfterSecondLogin.size());

            performLogin(clientSecret);

            events.expectLogout(firstLogin.getSessionId())
                    .user(userId)
                    .detail(Details.REASON, "session_limit_reached")
                    .removeDetail(Details.REDIRECT_URI)
                    .assertEvent();

            EventRepresentation thirdLogin =
                    events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
            events.expectCodeToToken(thirdLogin.getDetails().get("code_id"), thirdLogin.getSessionId()).user(userId)
                    .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE).assertEvent();

            List<UserSessionRepresentation> offlineUserSessionsAfterThirdLogin =
                    authorizationClient.getOfflineUserSessions(0, 100);
            Assert.assertEquals(2, offlineUserSessionsAfterThirdLogin.size());
        } finally {
            revertFlows(testRealm(), flowAlias);
            authorizationClient.removeDefaultClientScope(offlineScopeId);
            authorizationClient.addOptionalClientScope(offlineScopeId);
        }
    }

    @Test
    public void testCleanOfflineSessionsAfterLimitActivation() throws Exception {
        ClientResource authorizationClient = ApiUtil.findClientResourceByClientId(testRealm(), oauth.getClientId());
        String clientSecret = authorizationClient.getSecret().getValue();

        String offlineScopeId =
                ApiUtil.findClientScopeByName(testRealm(), OAuth2Constants.OFFLINE_ACCESS).toRepresentation().getId();

        authorizationClient.removeOptionalClientScope(offlineScopeId);
        authorizationClient.addDefaultClientScope(offlineScopeId);

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);

        final String flowAlias = "browser - offline session limit 1";

        Map<String, String> limitConfigMap = new HashMap<>();
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_MAX_SESSIONS, "100");
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_SORT,
                UserSessionsLimiterAuthenticatorFactory.ConfAttrSortOptions.STARTED.toString());
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_OFFLINE_SESSIONS, "true");
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_MAX_OFFLINE_SESSIONS, "1");
        limitConfigMap.put(UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_OFFLINE_SORT,
                UserSessionsLimiterAuthenticatorFactory.ConfAttrSortOptions.STARTED.toString());


        try {
            performLogin(clientSecret);

            EventRepresentation firstLogin =
                    events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
            events.expectCodeToToken(firstLogin.getDetails().get("code_id"), firstLogin.getSessionId()).user(userId)
                    .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE).assertEvent();

            List<UserSessionRepresentation> offlineUserSessionsAfterFirstLogin =
                    authorizationClient.getOfflineUserSessions(0, 100);
            Assert.assertEquals(1, offlineUserSessionsAfterFirstLogin.size());

            // wait a sec to make sure that start time of first session is before second one
            Thread.sleep(1000);

            performLogin(clientSecret);

            EventRepresentation secondLogin =
                    events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
            events.expectCodeToToken(secondLogin.getDetails().get("code_id"), secondLogin.getSessionId()).user(userId)
                    .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE).assertEvent();

            List<UserSessionRepresentation> offlineUserSessionsAfterSecondLogin =
                    authorizationClient.getOfflineUserSessions(0, 100);
            Assert.assertEquals(2, offlineUserSessionsAfterSecondLogin.size());

            configureBrowserFlowWithSessionLimit(flowAlias, limitConfigMap);
            performLogin(clientSecret);

            events.expectLogout(secondLogin.getSessionId())
                    .user(userId)
                    .detail(Details.REASON, "session_limit_reached")
                    .removeDetail(Details.REDIRECT_URI)
                    .assertEvent();

            events.expectLogout(firstLogin.getSessionId())
                    .user(userId)
                    .detail(Details.REASON, "session_limit_reached")
                    .removeDetail(Details.REDIRECT_URI)
                    .assertEvent();

            EventRepresentation thirdLogin =
                    events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
            events.expectCodeToToken(thirdLogin.getDetails().get("code_id"), thirdLogin.getSessionId()).user(userId)
                    .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE).assertEvent();

            List<UserSessionRepresentation> offlineUserSessionsAfterThirdLogin =
                    authorizationClient.getOfflineUserSessions(0, 100);
            Assert.assertEquals(1, offlineUserSessionsAfterThirdLogin.size());
        } finally {
            revertFlows(testRealm(), flowAlias);
            authorizationClient.removeDefaultClientScope(offlineScopeId);
            authorizationClient.addOptionalClientScope(offlineScopeId);
        }
    }

    private void performLogin(String clientSecret) throws URISyntaxException {
        loginPage.open();
        loginPage.assertCurrent();
        loginPage.login(USERNAME, PASSWORD);
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        oauth.doAccessTokenRequest(URLEncodedUtils.parse(new URI(driver.getCurrentUrl()), Charset.defaultCharset())
                .stream().filter(p -> "code".equals(p.getName())).findFirst().get().getValue(), clientSecret);
        deleteAllCookiesForRealm(TEST_REALM_NAME);
    }

    /**
     * This flow contains:
     * UsernamePasswordForm REQUIRED
     * UserSessionsLimiterAuthenticator REQUIRED
     *
     * @param newFlowAlias
     * @param limitConfig
     */
    private void configureBrowserFlowWithSessionLimit(String newFlowAlias, Map<String, String> limitConfig) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED,
                                UsernamePasswordFormFactory.PROVIDER_ID)
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED,
                                UserSessionsLimiterAuthenticatorFactory.PROVIDER_ID,
                                config -> config.setConfig(limitConfig)))
                .defineAsBrowserFlow() // Activate this new flow
        );
    }
}
