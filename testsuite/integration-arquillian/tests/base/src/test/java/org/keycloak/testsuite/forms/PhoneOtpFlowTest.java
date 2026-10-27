package org.keycloak.testsuite.forms;

import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.PhoneOtpAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.util.PhoneOtpUtils;
import org.keycloak.authentication.requiredactions.VerifyPhoneNumber;
import org.keycloak.common.util.Time;
import org.keycloak.cookie.CookieType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.PhoneOtpCodePage;
import org.keycloak.testsuite.pages.PhoneOtpLoginPage;
import org.keycloak.testsuite.pages.VerifyPhoneNumberCodePage;
import org.keycloak.testsuite.pages.VerifyPhoneNumberPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.RealmRepUtil;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.Cookie;

public class PhoneOtpFlowTest extends AbstractChangeImportedUserPasswordsTest {

    private static final String FLOW_ALIAS = "browser - phone otp";
    private static final String PHONE_NUMBER = "+12025550123";
    private static final String OTP_CODE = "123456";

    private static final String PHONE_OTP_HASH_NOTE = "PHONE_OTP_CODE_HASH";
    private static final String PHONE_OTP_CREATED_NOTE = "PHONE_OTP_CODE_CREATED";
    private static final String PHONE_OTP_ATTEMPTS_NOTE = "PHONE_OTP_CODE_ATTEMPTS";
    private static final String PHONE_OTP_LAST_SENT_NOTE = "PHONE_OTP_CODE_LAST_SENT";

    private static final String VERIFY_PHONE_HASH_NOTE = "VERIFY_PHONE_OTP_HASH";
    private static final String VERIFY_PHONE_CREATED_NOTE = "VERIFY_PHONE_OTP_CREATED";
    private static final String VERIFY_PHONE_ATTEMPTS_NOTE = "VERIFY_PHONE_OTP_ATTEMPTS";
    private static final String VERIFY_PHONE_LAST_SENT_NOTE = "VERIFY_PHONE_OTP_LAST_SENT";
    private static final String VERIFY_PHONE_NUMBER_NOTE = "VERIFY_PHONE_NUMBER_VALUE";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected PhoneOtpLoginPage phoneOtpLoginPage;

    @Page
    protected PhoneOtpCodePage phoneOtpCodePage;

    @Page
    protected VerifyPhoneNumberPage verifyPhoneNumberPage;

    @Page
    protected VerifyPhoneNumberCodePage verifyPhoneNumberCodePage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        UserRepresentation user = RealmRepUtil.findUser(testRealm, "test-user@localhost");
        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        } else {
            attributes = new HashMap<>(attributes);
        }
        attributes.put("phoneNumber", List.of(PHONE_NUMBER));
        attributes.put("phoneNumberVerified", List.of("false"));
        user.setAttributes(attributes);
    }

    @After
    public void after() {
        deleteFlowIfPresent(FLOW_ALIAS);
        UserResource user = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), "test-user@localhost");
        UserRepresentation rep = user.toRepresentation();
        rep.setRequiredActions(Collections.emptyList());
        user.update(rep);
    }

    @Test
    public void phoneOtpLoginSuccess() {
        configurePhoneOtpFlow();
        try {
            oauth.openLoginForm();
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

            phoneOtpLoginPage.assertCurrent();
            phoneOtpLoginPage.submitPhoneNumber(PHONE_NUMBER);

            phoneOtpCodePage.assertCurrent();
            setPhoneOtpCodeForCurrentSession(OTP_CODE);
            phoneOtpCodePage.submitCode(OTP_CODE);

            Assertions.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
            EventAssertion.expectLoginSuccess(events.poll());
        } finally {
            deleteFlowIfPresent(FLOW_ALIAS);
        }
    }

    @Test
    public void phoneOtpLoginInvalidCode() {
        configurePhoneOtpFlow();
        try {
            oauth.openLoginForm();
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

            phoneOtpLoginPage.assertCurrent();
            phoneOtpLoginPage.submitPhoneNumber(PHONE_NUMBER);

            phoneOtpCodePage.assertCurrent();
            setPhoneOtpCodeForCurrentSession(OTP_CODE);
            phoneOtpCodePage.submitCode("000000");

            Assertions.assertEquals("Invalid authenticator code.", phoneOtpCodePage.getInputError());
        } finally {
            deleteFlowIfPresent(FLOW_ALIAS);
        }
    }

    @Test
    public void phoneOtpResendCooldown() {
        configurePhoneOtpFlow();
        try {
            oauth.openLoginForm();
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

            phoneOtpLoginPage.assertCurrent();
            phoneOtpLoginPage.submitPhoneNumber(PHONE_NUMBER);

            phoneOtpCodePage.assertCurrent();
            phoneOtpCodePage.resendCode();

            Assertions.assertTrue(phoneOtpCodePage.getAlertError().contains("Please wait"));
        } finally {
            deleteFlowIfPresent(FLOW_ALIAS);
        }
    }

    @Test
    public void verifyPhoneNumberRequiredAction() {
        ensureVerifyPhoneRequiredActionRegistered();
        UserResource user = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), "test-user@localhost");
        UserRepresentation rep = user.toRepresentation();
        if (rep.getAttributes() == null || rep.getAttributes().isEmpty()) {
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("phoneNumber", List.of(PHONE_NUMBER));
            attributes.put("phoneNumberVerified", List.of("false"));
            rep.setAttributes(attributes);
        }
        rep.setRequiredActions(List.of(VerifyPhoneNumber.PROVIDER_ID));
        user.update(rep);

        oauth.openLoginForm();
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        verifyPhoneNumberPage.assertCurrent();
        verifyPhoneNumberPage.submitPhoneNumber(PHONE_NUMBER);

        verifyPhoneNumberCodePage.assertCurrent();
        setVerifyPhoneOtpCodeForCurrentSession(OTP_CODE);
        verifyPhoneNumberCodePage.submitCode(OTP_CODE);

        Assertions.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        assertUserPhoneAttributes(PHONE_NUMBER, "true");
    }

    private void configurePhoneOtpFlow() {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(FLOW_ALIAS));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(FLOW_ALIAS)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, PhoneOtpAuthenticatorFactory.PROVIDER_ID, config -> {
                            config.getConfig().put(PhoneOtpAuthenticatorFactory.CONF_PHONE_ATTRIBUTE, "phoneNumber");
                            config.getConfig().put(PhoneOtpAuthenticatorFactory.CONF_CODE_LENGTH, "6");
                            config.getConfig().put(PhoneOtpAuthenticatorFactory.CONF_CODE_TTL, "300");
                            config.getConfig().put(PhoneOtpAuthenticatorFactory.CONF_MAX_ATTEMPTS, "3");
                            config.getConfig().put(PhoneOtpAuthenticatorFactory.CONF_RESEND_COOLDOWN, "30");
                        })
                )
                .defineAsBrowserFlow()
        );
    }

    private void setPhoneOtpCodeForCurrentSession(String code) {
        setOtpForCurrentSession(code, PHONE_OTP_HASH_NOTE, PHONE_OTP_CREATED_NOTE, PHONE_OTP_ATTEMPTS_NOTE, PHONE_OTP_LAST_SENT_NOTE, null);
    }

    private void setVerifyPhoneOtpCodeForCurrentSession(String code) {
        setOtpForCurrentSession(code, VERIFY_PHONE_HASH_NOTE, VERIFY_PHONE_CREATED_NOTE, VERIFY_PHONE_ATTEMPTS_NOTE, VERIFY_PHONE_LAST_SENT_NOTE, PHONE_NUMBER);
    }

    private void setOtpForCurrentSession(String code, String hashNote, String createdNote, String attemptsNote, String lastSentNote, String phoneNumber) {
        String actionUri = ActionURIUtils.getActionURIFromPageSource(driver.getPageSource());
        Map<String, String> params = ActionURIUtils.parseQueryParamsFromActionURI(actionUri);
        String tabId = params.get(Constants.TAB_ID);
        Cookie authSessionCookie = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        Assertions.assertNotNull(authSessionCookie, "Auth session cookie missing");

        String authSessionId = authSessionCookie.getValue();

        testingClient.server("test").run(session -> {
            RealmModel realm = session.realms().getRealmByName(TEST_REALM_NAME);
            session.getContext().setRealm(realm);

            String decodedAuthSessionId = new AuthenticationSessionManager(session).decodeBase64AndValidateSignature(authSessionId);
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, decodedAuthSessionId);
            if (rootAuthSession == null) {
                throw new IllegalStateException("Root authentication session not found");
            }

            ClientModel client = realm.getClientByClientId("test-app");
            AuthenticationSessionModel authSession = rootAuthSession.getAuthenticationSession(client, tabId);
            if (authSession == null) {
                throw new IllegalStateException("Authentication session not found for tab " + tabId);
            }

            String salt = authSession.getParentSession().getId() + ":" + authSession.getTabId();
            authSession.setAuthNote(hashNote, PhoneOtpUtils.hashCode(code, salt));
            authSession.setAuthNote(createdNote, Integer.toString(Time.currentTime()));
            authSession.setAuthNote(attemptsNote, "0");
            authSession.setAuthNote(lastSentNote, Integer.toString(Time.currentTime()));
            if (phoneNumber != null) {
                authSession.setAuthNote(VERIFY_PHONE_NUMBER_NOTE, phoneNumber);
            }
        });
    }

    private void ensureVerifyPhoneRequiredActionRegistered() {
        AuthenticationManagementResource authMgmt = managedRealm.admin().flows();
        boolean exists = authMgmt.getRequiredActions().stream()
                .anyMatch(action -> VerifyPhoneNumber.PROVIDER_ID.equals(action.getProviderId())
                        || VerifyPhoneNumber.PROVIDER_ID.equals(action.getAlias()));
        if (!exists) {
            RequiredActionProviderSimpleRepresentation rep = new RequiredActionProviderSimpleRepresentation();
            rep.setProviderId(VerifyPhoneNumber.PROVIDER_ID);
            rep.setName("Verify phone number");
            authMgmt.registerRequiredAction(rep);
        }
    }

    private void deleteFlowIfPresent(String flowAlias) {
        RealmResource realm = managedRealm.admin();
        if (realm.flows().getFlows().stream().anyMatch(flow -> flowAlias.equals(flow.getAlias()))) {
            BrowserFlowTest.revertFlows(realm, flowAlias);
        }
    }

    private void assertUserPhoneAttributes(String phoneNumber, String verified) {
        testingClient.server("test").run(session -> {
            RealmModel realm = session.realms().getRealmByName(TEST_REALM_NAME);
            UserModel user = session.users().getUserByUsername(realm, "test-user@localhost");
            if (user == null) {
                throw new IllegalStateException("User not found");
            }
            Assertions.assertEquals(phoneNumber, user.getFirstAttribute("phoneNumber"));
            Assertions.assertEquals(verified, user.getFirstAttribute("phoneNumberVerified"));
        });
    }
}
