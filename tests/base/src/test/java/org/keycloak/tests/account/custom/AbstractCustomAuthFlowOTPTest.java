package org.keycloak.tests.account.custom;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.AbstractLoginPage;
import org.keycloak.testframework.ui.page.LoginConfigTotpPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.models.UserModel.RequiredAction.CONFIGURE_TOTP;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author rmartinc
 */
public abstract class AbstractCustomAuthFlowOTPTest extends AbstractCustomAccountManagementTest {

    private final TimeBasedOTP totp = new TimeBasedOTP();

    @InjectPage
    protected LoginConfigTotpConfigPage loginConfigTotpPage;

    @InjectPage
    protected LoginTotpPage loginTotpPage;

    @BeforeEach
    public void configureUserProfile() {
        UserProfileResource userProfileRes = managedRealm.admin().users().userProfile();
        UserProfileUtil.enableUnmanagedAttributes(userProfileRes);
    }

    protected void configureRequiredActions() {
        //set configure TOTP as required action to test user
        List<String> requiredActions = new ArrayList<>();
        requiredActions.add(CONFIGURE_TOTP.name());
        testUser.setRequiredActions(requiredActions);
        managedRealm.admin().users().get(testUser.getId()).update(testUser);
    }

    protected void configureOTP() {
        //configure OTP for test user
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        String totpSecret = loginConfigTotpPage.getTotpSecret();
        loginConfigTotpPage.configure(totp.generateTOTP(totpSecret));
        AccountHelper.logout(managedRealm.admin(), testUser.getUsername());

        //verify that user has OTP configured
        testUser = managedRealm.admin().users().get(testUser.getId()).toRepresentation();
        assertTrue(testUser.getRequiredActions().isEmpty());
    }

    protected void reuseExistingOtp(boolean allowReusingExistingOtp) {
        RealmRepresentation originalRealm = managedRealm.admin().toRepresentation();
        managedRealm.cleanup().add(realmResource -> realmResource.update(originalRealm));

        RealmRepresentation updatedRealm = org.keycloak.testframework.realm.RepresentationUtils.clone(originalRealm);
        updatedRealm.setBrowserFlow("browser");
        updatedRealm.setOtpPolicyCodeReusable(allowReusingExistingOtp);
        managedRealm.admin().update(updatedRealm);

        updateRequirement("browser", AuthenticationExecutionModel.Requirement.REQUIRED, (authExec) -> authExec.getDisplayName().equals("Browser - Conditional 2FA"));
        updateRequirement("Browser - Conditional 2FA", OTPFormAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.REQUIRED);
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        loginConfigTotpPage.assertCurrent();

        //configure OTP for test user
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        final String totpSecret = loginConfigTotpPage.getTotpSecret();
        assertThat(totpSecret, notNullValue());

        final String generatedOtp = totp.generateTOTP(totpSecret);
        assertThat(generatedOtp, notNullValue());

        loginConfigTotpPage.configure(generatedOtp);
        AccountHelper.logout(managedRealm.admin(), testUser.getUsername());

        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        loginTotpPage.assertCurrent();
        loginTotpPage.login(generatedOtp);
    }

    protected RoleRepresentation getOrCreateOTPRole() {
        try {
            return managedRealm.admin().roles().get("otp_role").toRepresentation();
        } catch (NotFoundException ex) {
            RoleRepresentation role = new RoleRepresentation("otp_role", "", false);
            managedRealm.admin().roles().create(role);
            //obtain id
            return managedRealm.admin().roles().get("otp_role").toRepresentation();
        }
    }

    protected GroupRepresentation getOrCreateOTPRoleInGroup() {
        GroupRepresentation group = new GroupRepresentation();
        group.setName("otp_group");
        RoleRepresentation role  = getOrCreateOTPRole();
        managedRealm.admin().groups().add(group);
        // obtain id
        GroupRepresentation groupRep = managedRealm.admin().groups().groups("otp_group",0,1).get(0);
        managedRealm.admin().groups().group(groupRep.getId()).roles().realmLevel().add(Arrays.asList(role));
        // reread
        return managedRealm.admin().groups().groups("otp_group",0,1).get(0);
    }

    protected String authServerPort() {
        int port = URI.create(managedRealm.getBaseUrl()).getPort();
        return port < 0 ? "80" : String.valueOf(port);
    }

    protected void setConditionalOTPForm(Map<String, String> config) {
        List<AuthenticationFlowRepresentation> authFlows = getAuthMgmtResource().getFlows();
        for (AuthenticationFlowRepresentation flow : authFlows) {
            if ("ConditionalOTPFlow".equals(flow.getAlias())) {
                //update realm browser flow
                RealmRepresentation realm = managedRealm.admin().toRepresentation();
                realm.setBrowserFlow(DefaultAuthenticationFlows.BROWSER_FLOW);
                managedRealm.admin().update(realm);

                getAuthMgmtResource().deleteFlow(flow.getId());
                break;
            }
        }

        String flowAlias = "ConditionalOTPFlow";
        String provider = "auth-conditional-otp-form";

        //create flow
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(flowAlias);
        flow.setDescription("");
        flow.setProviderId("basic-flow");
        flow.setTopLevel(true);
        flow.setBuiltIn(false);

        try (Response response = getAuthMgmtResource().createFlow(flow)) {
            assertEquals(201, response.getStatus(), flowAlias + " create success");
        }

        //add execution - username-password form
        Map<String, Object> data = new HashMap<>();
        data.put("provider", "auth-username-password-form");
        getAuthMgmtResource().addExecution(flowAlias, data);

        //set username-password requirement to required
        updateRequirement(flowAlias, "auth-username-password-form", AuthenticationExecutionModel.Requirement.REQUIRED);

        //add execution - conditional OTP
        data.clear();
        data.put("provider", provider);
        getAuthMgmtResource().addExecution(flowAlias, data);

        //set Conditional 2FA requirement to required
        updateRequirement(flowAlias, provider, AuthenticationExecutionModel.Requirement.REQUIRED);

        //update realm browser flow
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setBrowserFlow(flowAlias);
        managedRealm.admin().update(realm);

        if (config != null) {
            //get executionId
            String executionId = getExecution(flowAlias, provider).getId();

            //prepare auth config
            AuthenticatorConfigRepresentation authConfig = new AuthenticatorConfigRepresentation();
            authConfig.setAlias("Config alias");
            authConfig.setConfig(config);

            //add auth config to the execution
            try (Response response = getAuthMgmtResource().newExecutionConfig(executionId, authConfig)) {
                assertEquals(201, response.getStatus(), "new execution success");
                String configId = ApiUtil.getCreatedId(response);
                managedRealm.cleanup().add(realmResource -> realmResource.flows().removeAuthenticatorConfig(configId));
            }
        }
    }

    public static class LoginConfigTotpConfigPage extends LoginConfigTotpPage {
        public LoginConfigTotpConfigPage(ManagedWebDriver driver) {
            super(driver);
        }

        public void configure(String totp) {
            WebElement totpInput = driver.findElement(org.openqa.selenium.By.id("totp"));
            totpInput.clear();
            totpInput.sendKeys(totp);

            driver.findElement(org.openqa.selenium.By.cssSelector("input[type=\"submit\"], #saveTOTPBtn")).click();
        }
    }

    public static class LoginTotpPage extends AbstractLoginPage {
        @FindBy(id = "otp")
        private WebElement otpInput;

        @FindBy(css = "[type=\"submit\"]")
        private WebElement submitButton;

        public LoginTotpPage(ManagedWebDriver driver) {
            super(driver);
        }

        public void login(String totp) {
            otpInput.clear();
            if (totp != null) {
                otpInput.sendKeys(totp);
            }
            submitButton.click();
        }

        @Override
        public String getExpectedPageId() {
            return "login-login-otp";
        }
    }
}
