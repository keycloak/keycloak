package org.keycloak.testsuite.forms;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalRoleAuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalUserAttributeValueFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalUserConfiguredAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.AbstractAuthenticationTest;
import org.keycloak.testsuite.auth.page.login.OneTimeCode;
import org.keycloak.testsuite.authentication.SetUserAttributeAuthenticatorFactory;
import org.keycloak.testsuite.broker.SocialLoginTest;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.URLUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GITHUB;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GITLAB;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GOOGLE;

public class BrowserFlowTest extends AbstractChangeImportedUserPasswordsTest {
    private static final String INVALID_AUTH_CODE = "Invalid authenticator code.";

    private static final String USER_WITH_ONE_OTP_OTP_SECRET = "DJmQfC73VGFhw7D4QJ8A";
    private static final String USER_WITH_TWO_OTPS_OTP1_SECRET = "DJmQfC73VGFhw7D4QJ8A";
    private static final String USER_WITH_TWO_OTPS_OTP2_SECRET = "ABCQfC73VGFhw7D4QJ8A";

    @ArquillianResource
    protected OAuthClient oauth;

    @Drone
    protected WebDriver driver;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginUsernameOnlyPage loginUsernameOnlyPage;

    @Page
    protected PasswordPage passwordPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected OneTimeCode oneTimeCodePage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private void importTestRealm(Consumer<RealmRepresentation> realmUpdater) {
        if (testRealmReps == null) {
            testRealmReps = testContext.getTestRealmReps();
        }
        RealmRepresentation realm = testRealmReps.get(0); // test realm
        if (realmUpdater != null) {
            realmUpdater.accept(realm);
        }
        importRealm(realm);
    }

    private void provideUsernamePassword(String user) {
        // Go to login page
        loginPage.open();
        loginPage.assertCurrent();

        // Login attempt with an invalid password
        loginPage.login(user, getPassword(user) + "invalid");
        loginPage.assertCurrent();

        // Login attempt with a valid password - user with configured OTP
        loginPage.login(user, getPassword(user));
    }

    private String getOtpCode(String key) {
        return new TimeBasedOTP().generateTOTP(key);
    }

    @Test
    public void testUserWithoutAdditionalFactorConnection() {
        provideUsernamePassword("test-user@localhost");
        Assert.assertFalse(loginPage.isCurrent());
        Assert.assertFalse(oneTimeCodePage.isOtpLabelPresent());
        Assert.assertFalse(loginTotpPage.isCurrent());
        loginTotpPage.assertOtpCredentialSelectorAvailability(false);
    }

    @Test
    public void testUserWithOneAdditionalFactorOtpFails() {
        provideUsernamePassword("user-with-one-configured-otp");
        Assert.assertTrue(oneTimeCodePage.isOtpLabelPresent());
        loginTotpPage.assertCurrent();
        loginTotpPage.assertOtpCredentialSelectorAvailability(false);

        // Use 7 digits instead 6 to have 100% probability of failure
        oneTimeCodePage.sendCode("1234567");
        Assert.assertEquals(INVALID_AUTH_CODE, oneTimeCodePage.getInputError());
        Assert.assertTrue(oneTimeCodePage.isOtpLabelPresent());
    }

    @Test
    public void testUserWithOneAdditionalFactorOtpSuccess() {
        provideUsernamePassword("user-with-one-configured-otp");
        Assert.assertTrue(oneTimeCodePage.isOtpLabelPresent());
        loginTotpPage.assertCurrent();
        loginTotpPage.assertOtpCredentialSelectorAvailability(false);

        oneTimeCodePage.sendCode(getOtpCode(USER_WITH_ONE_OTP_OTP_SECRET));
        Assert.assertFalse(loginPage.isCurrent());
        Assert.assertFalse(oneTimeCodePage.isOtpLabelPresent());
    }

    @Test
    public void testUserWithTwoAdditionalFactors() {
        // Provide username and password
        provideUsernamePassword("user-with-two-configured-otp");
        Assert.assertTrue(oneTimeCodePage.isOtpLabelPresent());
        loginTotpPage.assertCurrent();
        loginTotpPage.assertOtpCredentialSelectorAvailability(true);
        loginTotpPage.selectOtpCredential("first");

        // Check that selected credential is "first"
        Assert.assertEquals("first", loginTotpPage.getSelectedOtpCredential());

        // Select "second" factor (which is unnamed as it doesn't have userLabel) but try to connect with the OTP code from the "first" one
        loginTotpPage.selectOtpCredential(OTPFormAuthenticator.UNNAMED);
        loginTotpPage.login(getOtpCode(USER_WITH_TWO_OTPS_OTP1_SECRET));
        Assert.assertEquals(INVALID_AUTH_CODE, oneTimeCodePage.getInputError());

        // Select "first" factor but try to connect with the OTP code from the "second" one
        loginTotpPage.selectOtpCredential("first");
        loginTotpPage.login(getOtpCode(USER_WITH_TWO_OTPS_OTP2_SECRET));
        Assert.assertEquals(INVALID_AUTH_CODE, oneTimeCodePage.getInputError());

        // Select "second" factor and try to connect with its OTP code
        loginTotpPage.selectOtpCredential(OTPFormAuthenticator.UNNAMED);
        loginTotpPage.login(getOtpCode(USER_WITH_TWO_OTPS_OTP2_SECRET));
        Assert.assertFalse(oneTimeCodePage.isOtpLabelPresent());
    }

    private void testCredentialsOrder(String username, List<String> orderedCredentials) {
        // Provide username and password
        provideUsernamePassword(username);
        Assert.assertTrue(oneTimeCodePage.isOtpLabelPresent());
        loginTotpPage.assertCurrent();
        loginTotpPage.assertOtpCredentialSelectorAvailability(true);
        loginTotpPage.selectOtpCredential(orderedCredentials.get(0));

        // Check that preferred credential is selected
        Assert.assertEquals(orderedCredentials.get(0), loginTotpPage.getSelectedOtpCredential());
        // Check credentials order
        List<String> creds = loginTotpPage.getAvailableOtpCredentials();
        Assert.assertEquals(2, creds.size());
        Assert.assertEquals(orderedCredentials, creds);
    }

    @Test
    public void testCredentialsOrder() {
        String username = "user-with-two-configured-otp";
        int idxFirst = 0; // Credentials order is: first, password, second

        // Priority tells: first then second
        testCredentialsOrder(username, Arrays.asList("first", OTPFormAuthenticator.UNNAMED));

        try {
            // Move first credential in last position
            importTestRealm(realmRep -> {
                UserRepresentation user = realmRep.getUsers().stream().filter(u -> username.equals(u.getUsername())).findFirst().get();
                // Move first OTP after second while priority are not used for import
                user.getCredentials().add(user.getCredentials().remove(idxFirst));
            });
            changePasswords(username);

            // Priority tells: second then first
            testCredentialsOrder(username, Arrays.asList(OTPFormAuthenticator.UNNAMED, "first"));
        } finally {
            // Restore default testrealm.json
            testRealmReps = null;
            importTestRealms();
        }
    }


    // In a form waiting for a username only, provides a username and check if password is requested in the following execution of the flow
    private boolean needsPassword(String username) {
        // provides username
        loginUsernameOnlyPage.open();
        loginUsernameOnlyPage.login(username);

        return passwordPage.isCurrent();
    }

    // A conditional flow without conditional authenticator should automatically be disabled
    @Test

    public void testFlowDisabledWhenConditionalAuthenticatorIsMissing() {
        try {
            configureBrowserFlowWithConditionalSubFlowHavingConditionalAuthenticator("browser - non missing conditional authenticator", true);
            Assert.assertTrue(needsPassword("user-with-two-configured-otp"));

            configureBrowserFlowWithConditionalSubFlowHavingConditionalAuthenticator("browser - missing conditional authenticator", false);
            // Flow is conditional but it is missing a conditional authentication executor
            // The whole flow is disabled
            Assert.assertFalse(needsPassword("user-with-two-configured-otp"));
        } finally {
            revertFlows("browser - non missing conditional authenticator");
        }
    }

    private void configureBrowserFlowWithConditionalSubFlowHavingConditionalAuthenticator(String newFlowAlias, boolean conditionFlowHasConditionalAuthenticator) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                            if (conditionFlowHasConditionalAuthenticator) {
                                // Add authenticators to this flow: 1 conditional authenticator and a basic authenticator executions
                                subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalUserConfiguredAuthenticatorFactory.PROVIDER_ID);
                            }
                            // Update the browser forms only with a UsernameForm
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID);
                        }))
                .defineAsBrowserFlow()
        );
    }

    // A conditional flow with disabled conditional authenticator should automatically be disabled
    @Test

    public void testFlowDisabledWhenConditionalAuthenticatorIsDisabled() {
        try {
            configureBrowserFlowWithConditionalSubFlowHavingDisabledConditionalAuthenticator("browser - disabled conditional authenticator");
            // Flow is conditional but it is missing a conditional authentication executor
            // The whole flow is disabled
            Assert.assertFalse(needsPassword("user-with-two-configured-otp"));
        } finally {
            revertFlows("browser - disabled conditional authenticator");
        }
    }

    private void configureBrowserFlowWithConditionalSubFlowHavingDisabledConditionalAuthenticator(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                            // Add authenticators to this flow: 1 conditional authenticator and a basic authenticator executions
                            subFlow.addAuthenticatorExecution(Requirement.DISABLED, ConditionalUserConfiguredAuthenticatorFactory.PROVIDER_ID);

                            // Update the browser forms only with a UsernameForm
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID);
                        }))
                .defineAsBrowserFlow()
        );
    }

    // Configure a conditional authenticator in a non-conditional sub-flow
    // In such case, the flow is evaluated and the conditional authenticator is considered as disabled
    @Test

    public void testConditionalAuthenticatorInNonConditionalFlow() {
        try {
            configureBrowserFlowWithConditionalAuthenticatorInNonConditionalFlow();

            // provides username
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.login("user-with-two-configured-otp");

            // if flow was conditional, the conditional authenticator would disable the flow because no user have the expected role
            // Here, the password form is shown: it shows that the executor of the conditional bloc has been disabled. Other
            // executors of this flow are executed anyway
            passwordPage.assertCurrent();
        } finally {
            revertFlows("browser - nonconditional");
        }
    }

    private void configureBrowserFlowWithConditionalAuthenticatorInNonConditionalFlow() {
        String newFlowAlias = "browser - nonconditional";
        String requiredRole = "non-existing-role";
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.REQUIRED, subFlow -> subFlow
                                // Add authenticators to this flow: 1 conditional authenticator and a basic authenticator executions
                                .addAuthenticatorExecution(Requirement.REQUIRED, ConditionalRoleAuthenticatorFactory.PROVIDER_ID,
                                        config -> config.getConfig().put("condUserRole", requiredRole))
                                .addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID)
                        )
                )
                .defineAsBrowserFlow()
        );
    }

    // Check the ConditionalRoleAuthenticator
    // Configure a conditional subflow with the required role "user" and an OTP authenticator
    // user-with-two-configured-otp has the "user" role and should be asked for an OTP code
    // user-with-one-configured-otp does not have the role. He should not be asked for an OTP code
    @Test

    public void testConditionalRoleAuthenticator() {
        String requiredRole = "user";
        // A browser flow is configured with an OTPForm for users having the role "user"
        configureBrowserFlowOTPNeedsRole(requiredRole);

        try {
            // user-with-two-configured-otp has been configured with role "user". He should be asked for an OTP code
            provideUsernamePassword("user-with-two-configured-otp");
            Assert.assertTrue(oneTimeCodePage.isOtpLabelPresent());
            loginTotpPage.assertCurrent();
            loginTotpPage.assertOtpCredentialSelectorAvailability(true);

            // user-with-one-configured-otp has not configured role. He should not be asked for an OTP code
            provideUsernamePassword("user-with-one-configured-otp");
            Assert.assertFalse(oneTimeCodePage.isOtpLabelPresent());
            Assert.assertFalse(loginTotpPage.isCurrent());
        } finally {
            revertFlows("browser - rule");
        }
    }

    // Check the ConditionalRoleAuthenticator
    // Configure a conditional subflow with the required realm role "child-realm-role-1" and an OTP authenticator
    // "child-realm-role-1" is a realm role included in realm composite role "composite-realm-role-1"
    // user-with-two-configured-otp has the "composite-realm-role-1" role and should be asked for an OTP code
    // user-with-one-configured-otp does not have the role. He should not be asked for an OTP code
    @Test

    public void testConditionalRoleAuthenticatorWithRealmRoleIncludedInCompositeRealmRole() {

        // Create composite-realm-role-1
        String compositeRealmRoleName = "composite-realm-role-1";
        testRealm().roles().create(RoleBuilder.create().name(compositeRealmRoleName).build());

        // Create child-realm-role-1
        String childRealmRoleName = "child-realm-role-1";
        testRealm().roles().create(RoleBuilder.create().name(childRealmRoleName).build());

        // Make child-realm-role-1 a member of composite-realm-role-1
        testRealm().roles().get(compositeRealmRoleName)
                .addComposites(Collections.singletonList(testRealm().roles().get(childRealmRoleName).toRepresentation()));

        // Add composite-realm-role-1 to user "user-with-two-configured-otp"
        UserResource userResource = ApiUtil.findUserByUsernameId(testRealm(), "user-with-two-configured-otp");
        userResource.roles().realmLevel().add(Collections.singletonList(testRealm().roles().get(compositeRealmRoleName).toRepresentation()));

        // A browser flow is configured with an OTPForm for users having the role "child-realm-role-1"
        configureBrowserFlowOTPNeedsRole(childRealmRoleName);

        try {
            // user-with-two-configured-otp has been configured with role "composite-realm-role-1".
            // He should be asked for an OTP code
            provideUsernamePassword("user-with-two-configured-otp");
            Assert.assertTrue(oneTimeCodePage.isOtpLabelPresent());
            loginTotpPage.assertCurrent();
            loginTotpPage.assertOtpCredentialSelectorAvailability(true);

            // user-with-one-configured-otp doesn't have the role. He should not be asked for an OTP code
            provideUsernamePassword("user-with-one-configured-otp");
            Assert.assertFalse(oneTimeCodePage.isOtpLabelPresent());
            Assert.assertFalse(loginTotpPage.isCurrent());
        } finally {
            testRealm().roles().deleteRole(childRealmRoleName);
            testRealm().roles().deleteRole(compositeRealmRoleName);
            revertFlows("browser - rule");
        }
    }

    // Check the ConditionalRoleAuthenticator
    // Configure a conditional subflow with the required client role "child-client-role-1" from "test-app" client and an OTP authenticator
    // "child-client-role-1" is a client role included in composite client role "composite-client-role-1"
    // user-with-two-configured-otp has the "composite-client-role-1" role and should be asked for an OTP code
    // user-with-one-configured-otp does not have the role. He should not be asked for an OTP code
    @Test

    public void testConditionalRoleAuthenticatorWithClientRoleIncludedInCompositeClientRole() {

        String clientName = "test-app";
        ClientRepresentation testClient = testRealm().clients().findByClientId(clientName).get(0);

        // Create composite-client-role-1
        String compositeClientRoleName = "composite-client-role-1";
        testRealm().clients().get(testClient.getId()).roles().create(RoleBuilder.create().name(compositeClientRoleName).build());

        // Create child-client-role-1
        String childClientRoleName = "child-client-role-1";
        testRealm().clients().get(testClient.getId()).roles().create(RoleBuilder.create().name(childClientRoleName).build());

        // Make child-client-role-1 a member of composite-client-role-1
        testRealm().clients().get(testClient.getId()).roles().get(compositeClientRoleName)
                .addComposites(Collections.singletonList(testRealm().clients().get(testClient.getId()).roles().get(childClientRoleName).toRepresentation()));

        // Add composite-client-role-1 to user "user-with-two-configured-otp"
        UserResource userResource = ApiUtil.findUserByUsernameId(testRealm(), "user-with-two-configured-otp") ;
        userResource.roles().clientLevel(testClient.getId())
                .add(Collections.singletonList(testRealm().clients().get(testClient.getId()).roles().get(compositeClientRoleName).toRepresentation()));

        // A browser flow is configured with an OTPForm for users having the role "test-app.child-client-role-1"
        configureBrowserFlowOTPNeedsRole(clientName + "." + childClientRoleName);

        try {
            // user-with-two-configured-otp has been configured with role "test-app.child-client-role-1". He should be asked for an OTP code
            provideUsernamePassword("user-with-two-configured-otp");
            Assert.assertTrue(oneTimeCodePage.isOtpLabelPresent());
            loginTotpPage.assertCurrent();
            loginTotpPage.assertOtpCredentialSelectorAvailability(true);

            // user-with-one-configured-otp doesn't have the role. He should not be asked for an OTP code
            provideUsernamePassword("user-with-one-configured-otp");
            Assert.assertFalse(oneTimeCodePage.isOtpLabelPresent());
            Assert.assertFalse(loginTotpPage.isCurrent());
        } finally {
            testRealm().clients().get(testClient.getId()).roles().deleteRole(childClientRoleName);
            testRealm().clients().get(testClient.getId()).roles().deleteRole(compositeClientRoleName);
            revertFlows("browser - rule");
        }
    }

    private void configureBrowserFlowWithConditionalSubFlowWithChangingConditionWhileFlowEvaluation() {
        final String newFlowAlias = "browser - changing condition";
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                            // Add authenticators to this flow: 1 conditional authenticator and a basic authenticator executions
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalUserAttributeValueFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalUserAttributeValueFactory.CONF_ATTRIBUTE_NAME, "attribute");
                                        config.getConfig().put(ConditionalUserAttributeValueFactory.CONF_ATTRIBUTE_EXPECTED_VALUE, "value");
                                        config.getConfig().put(ConditionalUserAttributeValueFactory.CONF_NOT, Boolean.toString(true));
                                    });

                            // Set the attribute value
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, SetUserAttributeAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(SetUserAttributeAuthenticatorFactory.CONF_ATTR_NAME, "attribute");
                                        config.getConfig().put(SetUserAttributeAuthenticatorFactory.CONF_ATTR_VALUE, "value");
                                    });


                            // Requires Password
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID);

                            // Requires TOTP
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID);
                        }))
                .defineAsBrowserFlow()
        );
    }

    // Configure a conditional authenticator with a condition which change while the flow evaluation
    // In such case, all the required authenticator inside the subflow should be evaluated even if the condition has changed
    @Test

    public void testConditionalAuthenticatorWithConditionalSubFlowWithChangingConditionWhileFlowEvaluation() {
        try {
            configureBrowserFlowWithConditionalSubFlowWithChangingConditionWhileFlowEvaluation();

            // provides username
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.login("user-with-two-configured-otp");

            // The conditional sub flow is executed only if a specific user attribute is not set.
            // This sub flow will set the user attribute and displays password form.
            passwordPage.assertCurrent();
            passwordPage.login(getPassword("user-with-two-configured-otp"));

            Assert.assertTrue(oneTimeCodePage.isOtpLabelPresent());
        } finally {
            revertFlows("browser - changing condition");
        }
    }

    @Test
    public void testAlternativeNonInteractiveExecutorInSubflow() {
        final String newFlowAlias = "browser - alternative non-interactive executor";
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.REQUIRED, reqSubFlow -> reqSubFlow
                                .addAuthenticatorExecution(Requirement.ALTERNATIVE, PassThroughAuthenticator.PROVIDER_ID)
                        )
                )
                .defineAsBrowserFlow()
        );

        try {
            // provides username
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.login("test-user@localhost");

            // Check that Keycloak is redirecting us to the Keycloak account management page
            WebElement aHref = driver.findElement(By.tagName("a"));
            driver.get(aHref.getAttribute("href"));
            // Waiting for account redirection from app page
            waitForPage(driver, "Account Management", true);
            assertThat(driver.getTitle(), containsString("Account Management"));
        } finally {
            revertFlows("browser - alternative non-interactive executor");
        }
    }


    // Configure a flow with a conditional sub flow with a condition where a specific role is required
    private void configureBrowserFlowOTPNeedsRole(String requiredRole) {
        final String newFlowAlias = "browser - rule";
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        // Update the browser forms with a UsernamePasswordForm
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> subFlow
                                .addAuthenticatorExecution(Requirement.REQUIRED, ConditionalRoleAuthenticatorFactory.PROVIDER_ID,
                                        config -> config.getConfig().put("condUserRole", requiredRole))
                                .addAuthenticatorExecution(Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID)
                        )
                )
                .defineAsBrowserFlow()
        );
    }

    @Test

    public void testSwitchExecutionNotAllowedWithRequiredPasswordAndAlternativeOTP() {
        String newFlowAlias = "browser - copy 1";
        configureBrowserFlowWithRequiredPasswordFormAndAlternativeOTP(newFlowAlias);

        try {
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login("user-with-one-configured-otp");

            // Assert on password page now
            passwordPage.assertCurrent();

            String otpAuthenticatorExecutionId = realmsResouce().realm("test").flows().getExecutions(newFlowAlias)
                    .stream()
                    .filter(execution -> OTPFormAuthenticatorFactory.PROVIDER_ID.equals(execution.getProviderId()))
                    .findFirst()
                    .get()
                    .getId();

            // Manually run request to switch execution to OTP. It shouldn't be allowed and error should be thrown
            String actionURL = ActionURIUtils.getActionURIFromPageSource(driver.getPageSource());
            String formParameters = Constants.AUTHENTICATION_EXECUTION + "=" + otpAuthenticatorExecutionId + "&"
                    + Constants.CREDENTIAL_ID + "=";

            URLUtils.sendPOSTRequestWithWebDriver(actionURL, formParameters);

            errorPage.assertCurrent();

        } finally {
            revertFlows("browser - copy 1");
        }
    }


    @Test

    public void testSocialProvidersPresentOnLoginUsernameOnlyPageIfConfigured() {
        String testRealm = "test";
        // Test setup - Configure the testing Keycloak instance with UsernameForm & PasswordForm (both REQUIRED) and OTPFormAuthenticator (ALTERNATIVE)
        configureBrowserFlowWithRequiredPasswordFormAndAlternativeOTP("browser - copy 1");

        try {
            SocialLoginTest socialLoginTest = new SocialLoginTest();

            // Add some sample dummy GitHub, Gitlab & Google social providers to the testing realm. Dummy because they won't be fully
            // functional (won't have proper Client ID & Client Secret defined). But that doesn't matter for this particular test. What
            // matters is if they are visible (clickable) on the LoginUsernameOnlyPage once the page is loaded
            for (SocialLoginTest.Provider provider : Arrays.asList(GITHUB, GITLAB, GOOGLE)) {
                adminClient.realm(testRealm).identityProviders().create(socialLoginTest.buildIdp(provider));

                loginUsernameOnlyPage.open();
                loginUsernameOnlyPage.assertCurrent();
                // For each of the testing social providers, check the particular social provider button is present on the UsernameForm
                // Test succeeded if NoSuchElementException is thrown for none of them
                loginUsernameOnlyPage.findSocialButton(provider.id());
            }

            // Test cleanup - Return back to the initial state
        } finally {
            // Drop the testing social providers previously created within the test
            for (IdentityProviderRepresentation providerRepresentation : adminClient.realm(testRealm).identityProviders().findAll()) {
                adminClient.realm(testRealm).identityProviders().get(providerRepresentation.getInternalId()).remove();
            }

            revertFlows("browser - copy 1");
        }
    }

    // Configure the browser flow with those 3 authenticators at same level as subflows of the "Form":
    // UsernameForm: REQUIRED
    // PasswordForm: REQUIRED
    // OTPFormAuthenticator: ALTERNATIVE
    // In reality, the configuration of the flow like this doesn't have much sense, but nothing prevents administrator to configure it at this moment
    private void configureBrowserFlowWithRequiredPasswordFormAndAlternativeOTP(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        // Add REQUIRED UsernameForm Authenticator as first
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        // Add REQUIRED PasswordForm Authenticator as second
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID)
                        // Add OTPForm ALTERNATIVE execution as third
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, OTPFormAuthenticatorFactory.PROVIDER_ID)
                )
                // Activate this new flow
                .defineAsBrowserFlow()
        );
    }

    @Test

    public void testConditionalFlowWithConditionalAuthenticatorEvaluatingToFalseActsAsDisabled(){
        String newFlowAlias = "browser - copy 1";
        configureBrowserFlowWithConditionalFlowWithOTP(newFlowAlias);

        try {
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login("test-user@localhost");

            // Assert that the login evaluates to an error, as all required elements to not validate to successful
            errorPage.assertCurrent();

        } finally {
            revertFlows("browser - copy 1");
        }
    }

    @Test

    public void testConditionalFlowWithConditionalAuthenticatorEvaluatingToTrueActsAsRequired(){
        String newFlowAlias = "browser - copy 1";
        configureBrowserFlowWithConditionalFlowWithOTP(newFlowAlias);

        try {
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login("user-with-one-configured-otp");

            // Assert on password page now
            Assert.assertTrue(oneTimeCodePage.isOtpLabelPresent());
            loginTotpPage.assertCurrent();
            loginTotpPage.assertOtpCredentialSelectorAvailability(false);

            loginTotpPage.login(getOtpCode(USER_WITH_ONE_OTP_OTP_SECRET));
            Assert.assertFalse(loginTotpPage.isCurrent());
            events.expectLogin().user(testRealm().users().search("user-with-one-configured-otp").get(0).getId())
                    .detail(Details.USERNAME, "user-with-one-configured-otp").assertEvent();

        } finally {
            revertFlows("browser - copy 1");
        }
    }

    /**
     * Configure the browser flow with a simple flow that contains:
     * UsernameForm REQUIRED
     * Subflow REQUIRED
     * ** Sub-subflow CONDITIONAL
     * ***** ConditionalUserConfiguredAuthenticator REQUIRED
     * ***** OTPFormAuthenticator REQUIRED
     *
     * The expected behaviour is to prevent login if the user doesn't have an OTP credential, and to be able to login with an
     * OTP if the user has one. This demonstrates that conditional branches act as disabled when their conditional authenticator evaluates to false
     *
     * @param newFlowAlias
     */
    private void configureBrowserFlowWithConditionalFlowWithOTP(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.REQUIRED, sf -> sf
                                .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> subFlow
                                        .addAuthenticatorExecution(Requirement.REQUIRED, ConditionalUserConfiguredAuthenticatorFactory.PROVIDER_ID)
                                        .addAuthenticatorExecution(Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID)
                                )
                        )

                )
                // Activate this new flow
                .defineAsBrowserFlow()
        );
    }

    /**
     * In this test the user is expected to have to log in with OTP
     */
    @Test

    public void testConditionalFlowWithMultipleConditionalAuthenticatorsWithUserWithRoleAndOTP() {
        String newFlowAlias = "browser - copy 1";
        configureBrowserFlowWithConditionalFlowWithMultipleConditionalAuthenticators(newFlowAlias);

        try {
            String userId = testRealm().users().search("user-with-two-configured-otp").get(0).getId();
            provideUsernamePassword("user-with-two-configured-otp");
            events.expectLogin().user(userId).session((String) null)
                    .error("invalid_user_credentials")
                    .detail(Details.USERNAME, "user-with-two-configured-otp")
                    .removeDetail(Details.CONSENT)
                    .assertEvent();

            // Assert on otp page now
            Assert.assertTrue(oneTimeCodePage.isOtpLabelPresent());
            loginTotpPage.assertCurrent();
            loginTotpPage.assertOtpCredentialSelectorAvailability(true);

            loginTotpPage.login(getOtpCode(USER_WITH_TWO_OTPS_OTP1_SECRET));
            Assert.assertFalse(loginTotpPage.isCurrent());
            events.expectLogin().user(userId).detail(Details.USERNAME, "user-with-two-configured-otp").assertEvent();
        } finally {
            revertFlows("browser - copy 1");
        }
    }

    /**
     * In this test, the user is expected to have to login with username and password only, as the conditional branch evaluates to false, and is therefore DISABLED
     */
    @Test

    public void testConditionalFlowWithMultipleConditionalAuthenticatorsWithUserWithRoleButNotOTP() {
        String newFlowAlias = "browser - copy 1";
        configureBrowserFlowWithConditionalFlowWithMultipleConditionalAuthenticators(newFlowAlias);

        try {
            String userId = testRealm().users().search("user-with-one-configured-otp").get(0).getId();
            provideUsernamePassword("user-with-one-configured-otp");
            events.expectLogin().user(userId).session((String) null)
                    .error("invalid_user_credentials")
                    .detail(Details.USERNAME, "user-with-one-configured-otp")
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
            // Assert not on otp page now
            Assert.assertFalse(oneTimeCodePage.isOtpLabelPresent());
            Assert.assertFalse(loginTotpPage.isCurrent());
            events.expectLogin().user(userId).detail(Details.USERNAME, "user-with-one-configured-otp").assertEvent();

        } finally {
            revertFlows("browser - copy 1");
        }
    }

    /**
     * Configure the browser flow with a flow that contains:
     * UsernamePasswordForm REQUIRED
     * Subflow CONDITIONAL
     * ** ConditionalUserConfiguredAuthenticator REQUIRED
     * ** ConditionalRoleAuthenticator REQUIRED
     * ** OTPFormAuthenticatorFactory ALTERNATIVE
     * ** sub-subflow ALERNATIVE
     * **** OTPFormAuthenticatorFactory DISABLED
     *
     * The expected behaviour is the following:
     * - If the user is in the "user" group and has an OTP credential -> he sees the OTP form
     * - Otherwise the user logs in directly
     * This is important, because the ConditionalRoleAuthenticator must not count towards the check from the ConditionalUserConfiguredAuthenticator
     * The sub-subflow is present in the conditional flow to show that it is ignored by the ConditionalUserConfiguredAuthenticator (as it would raise an exception otherwise)
     * @param newFlowAlias
     */
    private void configureBrowserFlowWithConditionalFlowWithMultipleConditionalAuthenticators(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                                .clear()
                                .addAuthenticatorExecution(Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                                .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> subFlow
                                        .addAuthenticatorExecution(Requirement.REQUIRED, ConditionalUserConfiguredAuthenticatorFactory.PROVIDER_ID)
                                        .addAuthenticatorExecution(Requirement.REQUIRED, ConditionalRoleAuthenticatorFactory.PROVIDER_ID,
                                                config -> config.getConfig().put("condUserRole", "user"))
                                        .addAuthenticatorExecution(Requirement.ALTERNATIVE, OTPFormAuthenticatorFactory.PROVIDER_ID)
                                        .addSubFlowExecution(Requirement.ALTERNATIVE, sf -> sf
                                                .addAuthenticatorExecution(Requirement.DISABLED, OTPFormAuthenticatorFactory.PROVIDER_ID))
                                )
                        // Activate this new flow
                ).defineAsBrowserFlow()
        );
    }

    /**
     * This test checks that if a REQUIRED authentication execution which has isUserSetupAllowed -> true
     * has its requiredActionProvider in a not registered state, then it will not try to create the required action,
     * and will instead raise an credential setup required error.
     */
    @Test

    public void testLoginWithWithNoOTPCredentialAndNoRequiredActionProviderRegistered(){
        String newFlowAlias = "browser - copy 1";
        configureBrowserFlowWithRequiredOTP(newFlowAlias);
        RequiredActionProviderRepresentation otpRequiredAction = testRealm().flows().getRequiredAction("CONFIGURE_TOTP");
        testRealm().flows().removeRequiredAction("CONFIGURE_TOTP");
        try {
            provideUsernamePassword("test-user@localhost");

            // Assert that the login evaluates to an error, as all required elements to not validate to successful
            errorPage.assertCurrent();

        } finally {
            revertFlows("browser - copy 1");
            RequiredActionProviderSimpleRepresentation simpleRepresentation = new RequiredActionProviderSimpleRepresentation();
            simpleRepresentation.setProviderId("CONFIGURE_TOTP");
            simpleRepresentation.setName(otpRequiredAction.getName());
            testRealm().flows().registerRequiredAction(simpleRepresentation);
        }
    }

    /**
     * This test checks that if a REQUIRED authentication execution which has isUserSetupAllowed -> true
     * has its requiredActionProvider disabled, then it will not try to create the required action,
     * and will instead raise an credential setup required error.
     */
    @Test

    public void testLoginWithWithNoOTPCredentialAndRequiredActionProviderDisabled(){
        String newFlowAlias = "browser - copy 1";
        configureBrowserFlowWithRequiredOTP(newFlowAlias);
        RequiredActionProviderRepresentation otpRequiredAction = testRealm().flows().getRequiredAction("CONFIGURE_TOTP");
        otpRequiredAction.setEnabled(false);
        testRealm().flows().updateRequiredAction("CONFIGURE_TOTP", otpRequiredAction);
        try {
            provideUsernamePassword("test-user@localhost");

            // Assert that the login evaluates to an error, as all required elements to not validate to successful
            errorPage.assertCurrent();

        } finally {
            revertFlows("browser - copy 1");
            otpRequiredAction.setEnabled(true);
            testRealm().flows().updateRequiredAction("CONFIGURE_TOTP", otpRequiredAction);
        }
    }

    /**
     * This test checks that if a REQUIRED authentication execution which has isUserSetupAllowed -> true
     * has its requiredActionProvider enabled, than it will login and show the otpSetup page.
     */
    @Test

    public void testLoginWithWithNoOTPCredential(){
        String newFlowAlias = "browser - copy 1";
        configureBrowserFlowWithRequiredOTP(newFlowAlias);;
        try {
            provideUsernamePassword("test-user@localhost");

            // Assert that in this case you arrive to an OTP setup
            Assert.assertTrue(driver.getCurrentUrl().contains("required-action?execution=CONFIGURE_TOTP"));

        } finally {
            revertFlows("browser - copy 1");
            UserRepresentation user = testRealm().users().search("test-user@localhost").get(0);
            user.setRequiredActions(Collections.emptyList());
            testRealm().users().get(user.getId()).update(user);
        }
    }

    /**
     * This flow contains:
     * UsernamePasswordForm REQUIRED
     * OTPForm REQUIRED
     *
     * @param newFlowAlias
     */
    private void configureBrowserFlowWithRequiredOTP(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                                .clear()
                                .addAuthenticatorExecution(Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                                .addAuthenticatorExecution(Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID)
                ).defineAsBrowserFlow() // Activate this new flow
        );
    }

    /**
     * This test checks that if a REQUIRED authentication execution which has isUserSetupAllowed -> true
     * has its requiredActionProvider disabled, then it will not try to create the required action,
     * and will instead raise an credential setup required error.
     */
    @Test

    public void testLoginWithWithNoWebAuthnCredentialAndRequiredActionProviderDisabled(){
        String newFlowAlias = "browser - copy 1";
        configureBrowserFlowWithRequiredWebAuthn(newFlowAlias);
        RequiredActionProviderRepresentation rapr = testRealm().flows().getRequiredAction(WebAuthnRegisterFactory.PROVIDER_ID);
        rapr.setEnabled(false);
        testRealm().flows().updateRequiredAction(WebAuthnRegisterFactory.PROVIDER_ID, rapr);
        try {
            provideUsernamePassword("test-user@localhost");

            // Assert that the login evaluates to an error, as all required elements to not validate to successful
            errorPage.assertCurrent();

        } finally {
            revertFlows("browser - copy 1");
        }
    }

    /**
     * This test checks that if a REQUIRED authentication execution which has isUserSetupAllowed -> true
     * has its requiredActionProvider enabled, then it will login and show the WebAuthn registration page.
     */
    @Test

    public void testLoginWithWithNoWebAuthnCredential(){
        String newFlowAlias = "browser - copy 1";
        configureBrowserFlowWithRequiredWebAuthn(newFlowAlias);

        try {
            provideUsernamePassword("test-user@localhost");

            // Assert that in this case you arrive to an webauthn setup
            Assert.assertTrue(driver.getCurrentUrl().contains("required-action?execution=" + WebAuthnRegisterFactory.PROVIDER_ID));
        } finally {
            revertFlows("browser - copy 1");
            UserRepresentation user = testRealm().users().search("test-user@localhost").get(0);
            user.setRequiredActions(Collections.emptyList());
            testRealm().users().get(user.getId()).update(user);
        }
    }

    /**
     * This flow contains:
     * UsernamePasswordForm REQUIRED
     * WebAuthn REQUIRED
     *
     * @param newFlowAlias
     */
    private void configureBrowserFlowWithRequiredWebAuthn(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                        .addAuthenticatorExecution(Requirement.REQUIRED, WebAuthnAuthenticatorFactory.PROVIDER_ID)
                ).defineAsBrowserFlow() // Activate this new flow
        );
    }


    /**
     * This test checks that if a alternative authentication execution which has no credential, and the alternative is a flow,
     * then the selection mechanism will see that there's no viable alternative, and move on to the next execution (in this case the flow)
     */
    @Test

    public void testLoginWithWithNoOTPCredentialAndAlternativeActionProvider(){
        String newFlowAlias = "browser - copy 1";
        configureBrowserFlowWithAlternativeOTPAndPassword(newFlowAlias);
        try {
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login("test-user@localhost");

            // Assert that the login skipped the OTP authenticator and moved to the password
            passwordPage.assertCurrent();

        } finally {
            revertFlows("browser - copy 1");
        }
    }

    /**
     * This test checks the error messages, when the credentials are invalid and UsernameForm and PasswordForm are separated.
     */
    @Test
    public void testLoginWithWrongCredentialsMessage() {
        UserRepresentation user = testRealm().users().search("test-user@localhost").get(0);
        Assert.assertNotNull(user);

        loginPage.open();
        loginPage.assertCurrent();
        loginPage.login(user.getUsername(), getPassword("test-user@localhost") + "wrong_password");

        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());
        events.clear();

        loginPage.assertCurrent();
        loginPage.login(user.getUsername(), getPassword("test-user@localhost"));

        Assert.assertFalse(loginPage.isCurrent());
        events.expectLogin()
                .user(user)
                .detail(Details.USERNAME, "test-user@localhost")
                .assertEvent();
    }

    /**
     * This test checks the error messages, when the credentials are invalid and UsernameForm and PasswordForm are separated.
     */
    @Test

    public void testLoginMultiFactorWithWrongCredentialsMessage() {
        UserRepresentation user = testRealm().users().search("test-user@localhost").get(0);
        Assert.assertNotNull(user);

        MultiFactorAuthenticationTest.configureBrowserFlowWithAlternativeCredentials(testingClient);
        try {
            RealmRepresentation realm = testRealm().toRepresentation();
            realm.setLoginWithEmailAllowed(false);
            testRealm().update(realm);

            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login("non_existing_user");
            Assert.assertEquals("Invalid username.", loginUsernameOnlyPage.getUsernameError());
            Assert.assertEquals("Invalid username.", loginUsernameOnlyPage.getUsernameError());

            realm.setLoginWithEmailAllowed(true);
            testRealm().update(realm);
            loginUsernameOnlyPage.login("non_existing_user");
            Assert.assertEquals("Invalid username or email.", loginUsernameOnlyPage.getUsernameError());
            Assert.assertEquals("Invalid username or email.", loginUsernameOnlyPage.getUsernameError());

            loginUsernameOnlyPage.login(user.getUsername());

            passwordPage.assertCurrent();
            passwordPage.login("wrong_password");
            Assert.assertEquals("Invalid password.", passwordPage.getPasswordError());
            Assert.assertEquals("Invalid password.", passwordPage.getPasswordError());

            passwordPage.assertCurrent();
            events.clear();
            passwordPage.login(getPassword(user.getUsername()));

            Assert.assertFalse(loginUsernameOnlyPage.isCurrent());
            Assert.assertFalse(passwordPage.isCurrent());

            events.expectLogin()
                    .user(user)
                    .detail(Details.USERNAME, "test-user@localhost")
                    .assertEvent();
        } finally {
            revertFlows("browser - alternative");
        }
    }

    /**
     * This flow contains:
     * UsernameForm REQUIRED
     * Subflow REQUIRED
     * ** OTPForm ALTERNATIVE
     * ** sub-subflow ALTERNATIVE
     * **** PasswordForm ALTERNATIVE
     *
     * The passwordform is in a sub-subflow, because otherwise credential preference mechanisms would take over and any
     * way go into the password form
     *
     * @param newFlowAlias
     */
    private void configureBrowserFlowWithAlternativeOTPAndPassword(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.REQUIRED, subflow -> subflow
                                .addAuthenticatorExecution(Requirement.ALTERNATIVE, OTPFormAuthenticatorFactory.PROVIDER_ID)
                                .addSubFlowExecution(Requirement.ALTERNATIVE, sf -> sf
                                        .addAuthenticatorExecution(Requirement.ALTERNATIVE, PasswordFormFactory.PROVIDER_ID))
                                )
                ).defineAsBrowserFlow() // Activate this new flow
        );
    }


    /**
     * This test checks that if a alternative authentication execution which has isUserSetupAllowed -> true for
     * but is not a CredentialValidator (and therefore will not be removed by the selection mechanism),
     * then it will not try to create the required action, and will instead move to the next alternative
     */
    @Test

    public void testLoginWithWithNoWebAuthnCredentialAndAlternativeActionProvider(){
        String newFlowAlias = "browser - copy 1";
        configureBrowserFlowWithAlternativeWebAuthnAndPassword(newFlowAlias);
        try {
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login("test-user@localhost");

            // Assert that the login skipped the OTP authenticator and moved to the password
            passwordPage.assertCurrent();

        } finally {
            revertFlows("browser - copy 1");
        }
    }

    /**
     * Test for KEYCLOAK-12858
     *
     * Flow is configured, so that once user provides username, there are 2 alternatives:
     * - OTP
     * - Subflow1, which contains another conditional subflow2, which requires user to authenticate with Password if he has password configured
     *
     * After login with password and fulfill the conditional subflow2, the subflow1 should be considered successful as well and the OTP authentication should not be needed
     */
    @Test

    public void testLoginWithAlternativeOTPAndConditionalPassword(){
        String newFlowAlias = "browser - copy 2";
        configureBrowserFlowWithAlternativeOTPAndConditionalPassword(newFlowAlias);
        try {

            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login("user-with-one-configured-otp");

            // Assert that the login skipped the OTP authenticator and moved to the password
            passwordPage.assertCurrent();
            passwordPage.assertTryAnotherWayLinkAvailability(true);
            passwordPage.login(getPassword("user-with-one-configured-otp"));

            Assert.assertFalse(loginPage.isCurrent());
            Assert.assertFalse(oneTimeCodePage.isOtpLabelPresent());
            events.expectLogin().user(testRealm().users().search("user-with-one-configured-otp").get(0).getId())
                    .detail(Details.USERNAME, "user-with-one-configured-otp").assertEvent();
        } finally {
            revertFlows(newFlowAlias);
        }
    }

    /**
     * This flow contains:
     * UsernameForm REQUIRED
     * Subflow REQUIRED
     * ** WebAuthn ALTERNATIVE
     * ** sub-subflow ALTERNATIVE
     * **** PasswordForm ALTERNATIVE
     *
     * The password form is in a sub-subflow, because otherwise credential preference mechanisms would take over and any
     * way go into the password form. Note that this flow only works for the test because WebAuthn is a isUserSetupAllowed
     * flow . When this changes, this flow will have to be modified to use another appropriate
     * authenticator.
     *
     * @param newFlowAlias
     */
    private void configureBrowserFlowWithAlternativeWebAuthnAndPassword(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.REQUIRED, subflow -> subflow
                                .addAuthenticatorExecution(Requirement.ALTERNATIVE, WebAuthnAuthenticatorFactory.PROVIDER_ID)
                                .addSubFlowExecution(Requirement.ALTERNATIVE, sf -> sf
                                        .addAuthenticatorExecution(Requirement.ALTERNATIVE, PasswordFormFactory.PROVIDER_ID))
                        )
                ).defineAsBrowserFlow() // Activate this new flow
        );
    }

    /**
     * This flow contains:
     * UsernameForm REQUIRED
     * Subflow REQUIRED
     * ** OTP ALTERNATIVE
     * ** password-subflow ALTERNATIVE
     * **** PasswordConditional subflow CONDITIONAL
     * ****** ConditionalUserConfiguredOTP REQUIRED
     * ****** Password REQUIRED
     *
     * @param newFlowAlias
     */
    private void configureBrowserFlowWithAlternativeOTPAndConditionalPassword(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.REQUIRED, subflow -> subflow
                                .addAuthenticatorExecution(Requirement.ALTERNATIVE, OTPFormAuthenticatorFactory.PROVIDER_ID)
                                .addSubFlowExecution(Requirement.ALTERNATIVE, sf -> sf
                                        .addSubFlowExecution(Requirement.CONDITIONAL, sf2 -> sf2
                                                .addAuthenticatorExecution(Requirement.REQUIRED, ConditionalUserConfiguredAuthenticatorFactory.PROVIDER_ID)
                                                .addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID)
                                        )
                                )
                        )
                ).defineAsBrowserFlow() // Activate this new flow
        );
    }


    private void revertFlows(String flowToDeleteAlias) {
        revertFlows(testRealm(), flowToDeleteAlias);
    }

    public static void revertFlows(RealmResource realmResource, String flowToDeleteAlias) {
        List<AuthenticationFlowRepresentation> flows = realmResource.flows().getFlows();

        // Set default browser flow
        RealmRepresentation realm = realmResource.toRepresentation();
        realm.setBrowserFlow(DefaultAuthenticationFlows.BROWSER_FLOW);
        realmResource.update(realm);

        AuthenticationFlowRepresentation flowRepresentation = AbstractAuthenticationTest.findFlowByAlias(flowToDeleteAlias, flows);

        // Throw error if flow doesn't exist to ensure we did not accidentally use different alias of non-existing flow when
        // calling this method
        if (flowRepresentation == null) {
            throw new IllegalArgumentException("The flow with alias " + flowToDeleteAlias + " did not exist");
        }

        realmResource.flows().deleteFlow(flowRepresentation.getId());
    }

}
