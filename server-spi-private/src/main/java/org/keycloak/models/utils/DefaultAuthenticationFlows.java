/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.utils;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    public static final String SAML_ECP_FLOW = "saml ecp";
    public static final String DOCKER_AUTH = "docker auth";

    public static final String CLIENT_AUTHENTICATION_FLOW = "clients";
    public static final String FIRST_BROKER_LOGIN_FLOW = "first broker login";
    public static final String FIRST_BROKER_LOGIN_HANDLE_EXISTING_SUBFLOW = "Handle Existing Account";

    public static final String IDP_REVIEW_PROFILE_CONFIG_ALIAS = "review profile config";
    public static final String IDP_CREATE_UNIQUE_USER_CONFIG_ALIAS = "create unique user config";

    public static void addFlows(RealmModel realm) {
        if (realm.getFlowByAlias(BROWSER_FLOW) == null) browserFlow(realm);
        if (realm.getFlowByAlias(DIRECT_GRANT_FLOW) == null) directGrantFlow(realm, false);
        if (realm.getFlowByAlias(REGISTRATION_FLOW) == null) registrationFlow(realm);
        if (realm.getFlowByAlias(RESET_CREDENTIALS_FLOW) == null) resetCredentialsFlow(realm);
        if (realm.getFlowByAlias(CLIENT_AUTHENTICATION_FLOW) == null) clientAuthFlow(realm);
        if (realm.getFlowByAlias(FIRST_BROKER_LOGIN_FLOW) == null) firstBrokerLoginFlow(realm, false);
        if (realm.getFlowByAlias(SAML_ECP_FLOW) == null) samlEcpProfile(realm);
        if (realm.getFlowByAlias(DOCKER_AUTH) == null) dockerAuthenticationFlow(realm);
    }
    public static void migrateFlows(RealmModel realm) {
        if (realm.getFlowByAlias(BROWSER_FLOW) == null) browserFlow(realm, true);
        if (realm.getFlowByAlias(DIRECT_GRANT_FLOW) == null) directGrantFlow(realm, true);
        if (realm.getFlowByAlias(REGISTRATION_FLOW) == null) registrationFlow(realm);
        if (realm.getFlowByAlias(RESET_CREDENTIALS_FLOW) == null) resetCredentialsFlow(realm);
        if (realm.getFlowByAlias(CLIENT_AUTHENTICATION_FLOW) == null) clientAuthFlow(realm);
        if (realm.getFlowByAlias(FIRST_BROKER_LOGIN_FLOW) == null) firstBrokerLoginFlow(realm, true);
        if (realm.getFlowByAlias(SAML_ECP_FLOW) == null) samlEcpProfile(realm);
        if (realm.getFlowByAlias(DOCKER_AUTH) == null) dockerAuthenticationFlow(realm);
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

        addIdentityProviderAuthenticator(realm, null);

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

    public static void addIdentityProviderAuthenticator(RealmModel realm, String defaultProvider) {
        String browserFlowId = null;
        for (AuthenticationFlowModel f : realm.getAuthenticationFlows()) {
            if (f.getAlias().equals(DefaultAuthenticationFlows.BROWSER_FLOW)) {
                browserFlowId = f.getId();
                break;
            }
        }

        if (browserFlowId != null) {
            for (AuthenticationExecutionModel e : realm.getAuthenticationExecutions(browserFlowId)) {
                if ("identity-provider-redirector".equals(e.getAuthenticator())) {
                    return;
                }
            }

            AuthenticationExecutionModel execution;
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(browserFlowId);
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setAuthenticator("identity-provider-redirector");
            execution.setPriority(25);
            execution.setAuthenticatorFlow(false);

            if (defaultProvider != null) {
                AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();

                Map<String, String> config = new HashMap<>();
                config.put("defaultProvider", defaultProvider);
                configModel.setConfig(config);
                configModel = realm.addAuthenticatorConfig(configModel);

                execution.setAuthenticatorConfig(configModel.getId());
            }

            realm.addAuthenticatorExecution(execution);
        }
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

    public static void firstBrokerLoginFlow(RealmModel realm, boolean migrate) {
        AuthenticationFlowModel firstBrokerLogin = new AuthenticationFlowModel();
        firstBrokerLogin.setAlias(FIRST_BROKER_LOGIN_FLOW);
        firstBrokerLogin.setDescription("Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account");
        firstBrokerLogin.setProviderId("basic-flow");
        firstBrokerLogin.setTopLevel(true);
        firstBrokerLogin.setBuiltIn(true);
        firstBrokerLogin = realm.addAuthenticationFlow(firstBrokerLogin);

        AuthenticatorConfigModel reviewProfileConfig = new AuthenticatorConfigModel();
        reviewProfileConfig.setAlias(IDP_REVIEW_PROFILE_CONFIG_ALIAS);
        Map<String, String> config = new HashMap<>();
        config.put("update.profile.on.first.login", IdentityProviderRepresentation.UPFLM_MISSING);
        reviewProfileConfig.setConfig(config);
        reviewProfileConfig = realm.addAuthenticatorConfig(reviewProfileConfig);

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(firstBrokerLogin.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("idp-review-profile");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        execution.setAuthenticatorConfig(reviewProfileConfig.getId());
        realm.addAuthenticatorExecution(execution);


        AuthenticatorConfigModel createUserIfUniqueConfig = new AuthenticatorConfigModel();
        createUserIfUniqueConfig.setAlias(IDP_CREATE_UNIQUE_USER_CONFIG_ALIAS);
        config = new HashMap<>();
        config.put("require.password.update.after.registration", "false");
        createUserIfUniqueConfig.setConfig(config);
        createUserIfUniqueConfig = realm.addAuthenticatorConfig(createUserIfUniqueConfig);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(firstBrokerLogin.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator("idp-create-user-if-unique");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        execution.setAuthenticatorConfig(createUserIfUniqueConfig.getId());
        realm.addAuthenticatorExecution(execution);


        AuthenticationFlowModel linkExistingAccountFlow = new AuthenticationFlowModel();
        linkExistingAccountFlow.setTopLevel(false);
        linkExistingAccountFlow.setBuiltIn(true);
        linkExistingAccountFlow.setAlias(FIRST_BROKER_LOGIN_HANDLE_EXISTING_SUBFLOW);
        linkExistingAccountFlow.setDescription("Handle what to do if there is existing account with same email/username like authenticated identity provider");
        linkExistingAccountFlow.setProviderId("basic-flow");
        linkExistingAccountFlow = realm.addAuthenticationFlow(linkExistingAccountFlow);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(firstBrokerLogin.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setFlowId(linkExistingAccountFlow.getId());
        execution.setPriority(30);
        execution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(linkExistingAccountFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("idp-confirm-link");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(linkExistingAccountFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator("idp-email-verification");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        AuthenticationFlowModel verifyByReauthenticationAccountFlow = new AuthenticationFlowModel();
        verifyByReauthenticationAccountFlow.setTopLevel(false);
        verifyByReauthenticationAccountFlow.setBuiltIn(true);
        verifyByReauthenticationAccountFlow.setAlias("Verify Existing Account by Re-authentication");
        verifyByReauthenticationAccountFlow.setDescription("Reauthentication of existing account");
        verifyByReauthenticationAccountFlow.setProviderId("basic-flow");
        verifyByReauthenticationAccountFlow = realm.addAuthenticationFlow(verifyByReauthenticationAccountFlow);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(linkExistingAccountFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setFlowId(verifyByReauthenticationAccountFlow.getId());
        execution.setPriority(30);
        execution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        // password + otp
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(verifyByReauthenticationAccountFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("idp-username-password-form");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(verifyByReauthenticationAccountFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.OPTIONAL);

        if (migrate) {
            // Try to read OTP requirement from browser flow
            AuthenticationFlowModel browserFlow = realm.getBrowserFlow();
            if (browserFlow == null) {
                browserFlow = realm.getFlowByAlias(DefaultAuthenticationFlows.BROWSER_FLOW);
            }

            List<AuthenticationExecutionModel> browserExecutions = new LinkedList<>();
            KeycloakModelUtils.deepFindAuthenticationExecutions(realm, browserFlow, browserExecutions);
            for (AuthenticationExecutionModel browserExecution : browserExecutions) {
                if (browserExecution.getAuthenticator().equals("auth-otp-form")) {
                    execution.setRequirement(browserExecution.getRequirement());
                }
            }
        }

        execution.setAuthenticator("auth-otp-form");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);
    }

    public static void samlEcpProfile(RealmModel realm) {
        AuthenticationFlowModel ecpFlow = new AuthenticationFlowModel();

        ecpFlow.setAlias(SAML_ECP_FLOW);
        ecpFlow.setDescription("SAML ECP Profile Authentication Flow");
        ecpFlow.setProviderId("basic-flow");
        ecpFlow.setTopLevel(true);
        ecpFlow.setBuiltIn(true);
        ecpFlow = realm.addAuthenticationFlow(ecpFlow);

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();

        execution.setParentFlow(ecpFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("http-basic-authenticator");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);

        realm.addAuthenticatorExecution(execution);
    }

    public static void dockerAuthenticationFlow(final RealmModel realm) {
        AuthenticationFlowModel dockerAuthFlow = new AuthenticationFlowModel();

        dockerAuthFlow.setAlias(DOCKER_AUTH);
        dockerAuthFlow.setDescription("Used by Docker clients to authenticate against the IDP");
        dockerAuthFlow.setProviderId("basic-flow");
        dockerAuthFlow.setTopLevel(true);
        dockerAuthFlow.setBuiltIn(true);
        dockerAuthFlow = realm.addAuthenticationFlow(dockerAuthFlow);
        realm.setDockerAuthenticationFlow(dockerAuthFlow);

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();

        execution.setParentFlow(dockerAuthFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("docker-http-basic-authenticator");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);

        realm.addAuthenticatorExecution(execution);
    }
}
