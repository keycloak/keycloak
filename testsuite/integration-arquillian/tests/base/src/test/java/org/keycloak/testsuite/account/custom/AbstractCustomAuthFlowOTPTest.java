package org.keycloak.testsuite.account.custom;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.admin.Users;
import org.keycloak.testsuite.auth.page.login.OneTimeCode;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.models.UserModel.RequiredAction.CONFIGURE_TOTP;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

/**
 * Base Class for all Custom Auth Flow OTP Tests providing basic setup and test funcitonality.
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public abstract class AbstractCustomAuthFlowOTPTest extends AbstractCustomAccountManagementTest {
    private final TimeBasedOTP totp = new TimeBasedOTP();
    @Page
    protected OneTimeCode testLoginOneTimeCodePage;
    @Page
    protected LoginConfigTotpPage loginConfigTotpPage;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testLoginOneTimeCodePage.setAuthRealm(testRealmPage);
    }

    @Before
    @Override
    public void beforeTest() {
        super.beforeTest();
    }

    protected void configureRequiredActions() {
        //set configure TOTP as required action to test user
        List<String> requiredActions = new ArrayList<>();
        requiredActions.add(CONFIGURE_TOTP.name());
        testUser.setRequiredActions(requiredActions);
        testRealmResource().users().get(testUser.getId()).update(testUser);
    }

    protected void configureOTP() {
        //configure OTP for test user
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        String totpSecret = testRealmLoginPage.form().totpForm().getTotpSecret();
        testRealmLoginPage.form().totpForm().setTotp(totp.generateTOTP(totpSecret));
        testRealmLoginPage.form().totpForm().submit();
        testRealmAccountManagementPage.signOut();

        //verify that user has OTP configured
        testUser = testRealmResource().users().get(testUser.getId()).toRepresentation();
        Users.setPasswordFor(testUser, PASSWORD);
        assertTrue(testUser.getRequiredActions().isEmpty());
    }

    protected RoleRepresentation getOrCreateOTPRole() {
        try {
            return testRealmResource().roles().get("otp_role").toRepresentation();
        } catch (NotFoundException ex) {
            RoleRepresentation role = new RoleRepresentation("otp_role", "", false);
            testRealmResource().roles().create(role);
            //obtain id
            return testRealmResource().roles().get("otp_role").toRepresentation();
        }
    }

    protected void setConditionalOTPForm(Map<String, String> config) {
        List<AuthenticationFlowRepresentation> authFlows = getAuthMgmtResource().getFlows();
        for (AuthenticationFlowRepresentation flow : authFlows) {
            if ("ConditionalOTPFlow".equals(flow.getAlias())) {
                //update realm browser flow
                RealmRepresentation realm = testRealmResource().toRepresentation();
                realm.setBrowserFlow(DefaultAuthenticationFlows.BROWSER_FLOW);
                testRealmResource().update(realm);

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

        Response response = getAuthMgmtResource().createFlow(flow);
        assertEquals(flowAlias + " create success", 201, response.getStatus());
        response.close();

        //add execution - username-password form
        Map<String, String> data = new HashMap<>();
        data.put("provider", "auth-username-password-form");
        getAuthMgmtResource().addExecution(flowAlias, data);

        //set username-password requirement to required
        updateRequirement(flowAlias, "auth-username-password-form", AuthenticationExecutionModel.Requirement.REQUIRED);

        //add execution - conditional OTP
        data.clear();
        data.put("provider", provider);
        getAuthMgmtResource().addExecution(flowAlias, data);

        //set Conditional OTP requirement to required
        updateRequirement(flowAlias, provider, AuthenticationExecutionModel.Requirement.REQUIRED);

        //update realm browser flow
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setBrowserFlow(flowAlias);
        testRealmResource().update(realm);

        //get executionId
        String executionId = getExecution(flowAlias, provider).getId();

        //prepare auth config
        AuthenticatorConfigRepresentation authConfig = new AuthenticatorConfigRepresentation();
        authConfig.setAlias("Config alias");
        authConfig.setConfig(config);

        //add auth config to the execution
        response = getAuthMgmtResource().newExecutionConfig(executionId, authConfig);
        assertEquals("new execution success", 201, response.getStatus());
        getCleanup().addAuthenticationConfigId(ApiUtil.getCreatedId(response));
        response.close();
    }
}
