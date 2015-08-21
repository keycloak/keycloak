package org.keycloak.models.utils;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultAuthenticationFlows {

    public static final String REGISTRATION_FLOW = "registration";
    public static final String REGISTRATION_FORM_FLOW = "registration form";
    public static final String BROWSER_FLOW = "browser";
    public static final String DIRECT_GRANT_FLOW = "direct grant";
    public static final String RESET_CREDENTIALS_FLOW = "reset credentials";
    public static final String LOGIN_FORMS_FLOW = "forms";

    public static final String CLIENT_AUTHENTICATION_FLOW = "clients";

    public static void addFlows(RealmModel realm) {
        if (realm.getFlowByAlias(BROWSER_FLOW) == null) browserFlow(realm);
        if (realm.getFlowByAlias(DIRECT_GRANT_FLOW) == null) directGrantFlow(realm, false);
        if (realm.getFlowByAlias(REGISTRATION_FLOW) == null) registrationFlow(realm);
        if (realm.getFlowByAlias(RESET_CREDENTIALS_FLOW) == null) resetCredentialsFlow(realm);
        if (realm.getFlowByAlias(CLIENT_AUTHENTICATION_FLOW) == null) clientAuthFlow(realm);
    }
    public static void migrateFlows(RealmModel realm) {
        if (realm.getFlowByAlias(BROWSER_FLOW) == null) browserFlow(realm, true);
        if (realm.getFlowByAlias(DIRECT_GRANT_FLOW) == null) directGrantFlow(realm, true);
        if (realm.getFlowByAlias(REGISTRATION_FLOW) == null) registrationFlow(realm);
        if (realm.getFlowByAlias(RESET_CREDENTIALS_FLOW) == null) resetCredentialsFlow(realm);
        if (realm.getFlowByAlias(CLIENT_AUTHENTICATION_FLOW) == null) clientAuthFlow(realm);
    }

    public static void registrationFlow(RealmModel realm) {
        AuthenticationFlowModel registrationFlow = new AuthenticationFlowModel();
        registrationFlow.setAlias(REGISTRATION_FLOW);
        registrationFlow.setDescription("registration flow");
        registrationFlow.setProviderId("basic-flow");
        registrationFlow.setTopLevel(true);
        registrationFlow.setBuiltIn(true);
        registrationFlow = realm.addAuthenticationFlow(registrationFlow);
        realm.setRegistrationFlow(registrationFlow);

        AuthenticationFlowModel registrationFormFlow = new AuthenticationFlowModel();
        registrationFormFlow.setAlias(REGISTRATION_FORM_FLOW);
        registrationFormFlow.setDescription("registration form");
        registrationFormFlow.setProviderId("form-flow");
        registrationFormFlow.setTopLevel(false);
        registrationFormFlow.setBuiltIn(true);
        registrationFormFlow = realm.addAuthenticationFlow(registrationFormFlow);

        AuthenticationExecutionModel execution;

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(registrationFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("registration-page-form");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(true);
        execution.setFlowId(registrationFormFlow.getId());
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(registrationFormFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("registration-user-creation");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(registrationFormFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("registration-profile-action");
        execution.setPriority(40);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(registrationFormFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("registration-password-action");
        execution.setPriority(50);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        //AuthenticatorConfigModel captchaConfig = new AuthenticatorConfigModel();
        //captchaConfig.setAlias("Recaptcha Config");
        //Map<String, String> config = new HashMap<>();
        //config.put("site.key", "6LcFEAkTAAAAAOaY-5RJk3zIYw4AalNtqfac27Bn");
        //config.put("secret", "6LcFEAkTAAAAAM0SErEs9NlfhYpOTRj_vOVJSAMI");
        //captchaConfig.setConfig(config);
        //captchaConfig = realm.addAuthenticatorConfig(captchaConfig);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(registrationFormFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        execution.setAuthenticator("registration-recaptcha-action");
        execution.setPriority(60);
        execution.setAuthenticatorFlow(false);
        //execution.setAuthenticatorConfig(captchaConfig.getId());
        realm.addAuthenticatorExecution(execution);



    }

    public static void browserFlow(RealmModel realm) {
        browserFlow(realm, false);
    }

    private static boolean hasCredentialType(RealmModel realm, String type) {
        for (RequiredCredentialModel requiredCredentialModel : realm.getRequiredCredentials()) {
            if (type.equals(requiredCredentialModel.getType())) {
                return true;
            }

        }
        return false;
    }

    public static void resetCredentialsFlow(RealmModel realm) {
        AuthenticationFlowModel grant = new AuthenticationFlowModel();
        grant.setAlias(RESET_CREDENTIALS_FLOW);
        grant.setDescription("Reset credentials for a user if they forgot their password or something");
        grant.setProviderId("basic-flow");
        grant.setTopLevel(true);
        grant.setBuiltIn(true);
        grant = realm.addAuthenticationFlow(grant);
        realm.setResetCredentialsFlow(grant);

        // username
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(grant.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("reset-credentials-choose-user");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // send email
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(grant.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("reset-credential-email");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // password
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(grant.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("reset-password");
        execution.setPriority(30);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // otp
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(grant.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.OPTIONAL);
        execution.setAuthenticator("reset-otp");
        execution.setPriority(40);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);
    }

    public static void directGrantFlow(RealmModel realm, boolean migrate) {
        AuthenticationFlowModel grant = new AuthenticationFlowModel();
        grant.setAlias(DIRECT_GRANT_FLOW);
        grant.setDescription("OpenID Connect Resource Owner Grant");
        grant.setProviderId("basic-flow");
        grant.setTopLevel(true);
        grant.setBuiltIn(true);
        grant = realm.addAuthenticationFlow(grant);
        realm.setDirectGrantFlow(grant);

        // username
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(grant.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("direct-grant-validate-username");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // password
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(grant.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        if (migrate && !hasCredentialType(realm, RequiredCredentialModel.PASSWORD.getType())) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        }
        execution.setAuthenticator("direct-grant-validate-password");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // otp
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(grant.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.OPTIONAL);
        if (migrate && hasCredentialType(realm, RequiredCredentialModel.TOTP.getType())) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        }
        execution.setAuthenticator("direct-grant-validate-otp");
        execution.setPriority(30);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);
    }

    public static void browserFlow(RealmModel realm, boolean migrate) {
        AuthenticationFlowModel browser = new AuthenticationFlowModel();
        browser.setAlias(BROWSER_FLOW);
        browser.setDescription("browser based authentication");
        browser.setProviderId("basic-flow");
        browser.setTopLevel(true);
        browser.setBuiltIn(true);
        browser = realm.addAuthenticationFlow(browser);
        realm.setBrowserFlow(browser);

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(browser.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator("auth-cookie");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(browser.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        if (migrate && hasCredentialType(realm, RequiredCredentialModel.KERBEROS.getType())) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);

        }
        execution.setAuthenticator("auth-spnego");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);


        AuthenticationFlowModel forms = new AuthenticationFlowModel();
        forms.setTopLevel(false);
        forms.setBuiltIn(true);
        forms.setAlias(LOGIN_FORMS_FLOW);
        forms.setDescription("Username, password, otp and other auth forms.");
        forms.setProviderId("basic-flow");
        forms = realm.addAuthenticationFlow(forms);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(browser.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setFlowId(forms.getId());
        execution.setPriority(30);
        execution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        // forms
        // Username Password processing
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(forms.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("auth-username-password-form");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // otp processing
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(forms.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.OPTIONAL);
        if (migrate && hasCredentialType(realm, RequiredCredentialModel.TOTP.getType())) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);

        }

        execution.setAuthenticator("auth-otp-form");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);
    }

    public static void clientAuthFlow(RealmModel realm) {
        AuthenticationFlowModel clients = new AuthenticationFlowModel();
        clients.setAlias(CLIENT_AUTHENTICATION_FLOW);
        clients.setDescription("Base authentication for clients");
        clients.setProviderId("client-flow");
        clients.setTopLevel(true);
        clients.setBuiltIn(true);
        clients = realm.addAuthenticationFlow(clients);
        realm.setClientAuthenticationFlow(clients);

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(clients.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator("client-secret");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(clients.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator("client-jwt");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);
    }
}
