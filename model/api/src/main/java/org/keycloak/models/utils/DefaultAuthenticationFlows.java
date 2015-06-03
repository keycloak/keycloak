package org.keycloak.models.utils;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultAuthenticationFlows {

    public static final String BROWSER_FLOW = "browser";
    public static final String FORMS_FLOW = "forms";

    public static void addFlows(RealmModel realm) {
        AuthenticatorModel model = new AuthenticatorModel();
        model.setProviderId("auth-cookie");
        model.setAlias("Cookie");
        AuthenticatorModel cookieAuth = realm.addAuthenticator(model);
        model = new AuthenticatorModel();
        model.setProviderId("auth-login-form-otp");
        model.setAlias("Login Form OTP");
        AuthenticatorModel loginFormOtp = realm.addAuthenticator(model);
        model = new AuthenticatorModel();
        model.setProviderId("auth-login-form-password");
        model.setAlias("Login Form Password");
        AuthenticatorModel password = realm.addAuthenticator(model);
        model = new AuthenticatorModel();
        model.setProviderId("auth-login-form-username");
        model.setAlias("Login Form Username");
        AuthenticatorModel username = realm.addAuthenticator(model);
        model = new AuthenticatorModel();
        model.setProviderId("auth-otp-form");
        model.setAlias("Single OTP Form");
        AuthenticatorModel otp = realm.addAuthenticator(model);
        model = new AuthenticatorModel();
        model.setProviderId("auth-spnego");
        model.setAlias("Kerberos");
        AuthenticatorModel kerberos = realm.addAuthenticator(model);

        AuthenticationFlowModel browser = new AuthenticationFlowModel();
        browser.setAlias(BROWSER_FLOW);
        browser.setDescription("browser based authentication");
        browser = realm.addAuthenticationFlow(browser);
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(browser.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator(cookieAuth.getId());
        execution.setPriority(0);
        execution.setUserSetupAllowed(false);
        execution.setAutheticatorFlow(false);
        realm.addAuthenticatorExecution(execution);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(browser.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        execution.setAuthenticator(kerberos.getId());
        execution.setPriority(1);
        execution.setUserSetupAllowed(false);
        execution.setAutheticatorFlow(false);
        realm.addAuthenticatorExecution(execution);
        AuthenticationFlowModel forms = new AuthenticationFlowModel();
        forms.setAlias(FORMS_FLOW);
        forms.setDescription("Username, password, otp and other auth forms.");
        forms = realm.addAuthenticationFlow(forms);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(browser.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator(forms.getId());
        execution.setPriority(2);
        execution.setUserSetupAllowed(false);
        execution.setAutheticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        // forms
        // Username processing
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(forms.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator(username.getId());
        execution.setPriority(10);
        execution.setUserSetupAllowed(false);
        execution.setAutheticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // password processing
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(forms.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator(password.getId());
        execution.setPriority(11);
        execution.setUserSetupAllowed(false);
        execution.setAutheticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // otp processing
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(forms.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.OPTIONAL);
        execution.setAuthenticator(otp.getId());
        execution.setPriority(12);
        execution.setUserSetupAllowed(true);
        execution.setAutheticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

    }
}
