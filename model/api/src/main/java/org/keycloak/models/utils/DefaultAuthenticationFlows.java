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
        model.setProviderId("auth-username-password-form");
        model.setAlias("Username Password Form");
        AuthenticatorModel usernamePasswordForm = realm.addAuthenticator(model);

        model = new AuthenticatorModel();
        model.setProviderId("auth-otp-form");
        model.setAlias("Single OTP Form");
        AuthenticatorModel otpForm = realm.addAuthenticator(model);

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
        execution.setPriority(10);
        execution.setUserSetupAllowed(false);
        execution.setAutheticatorFlow(false);
        realm.addAuthenticatorExecution(execution);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(browser.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        execution.setAuthenticator(kerberos.getId());
        execution.setPriority(20);
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
        execution.setPriority(30);
        execution.setUserSetupAllowed(false);
        execution.setAutheticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        // forms
        // Username Password processing
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(forms.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator(usernamePasswordForm.getId());
        execution.setPriority(10);
        execution.setUserSetupAllowed(false);
        execution.setAutheticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // otp processing
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(forms.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.OPTIONAL);
        execution.setAuthenticator(otpForm.getId());
        execution.setPriority(20);
        execution.setUserSetupAllowed(true);
        execution.setAutheticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        //

    }
}
