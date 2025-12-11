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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;


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
        if (realm.getFlowByAlias(REGISTRATION_FLOW) == null) registrationFlow(realm, false);
        if (realm.getFlowByAlias(RESET_CREDENTIALS_FLOW) == null) resetCredentialsFlow(realm);
        if (realm.getFlowByAlias(CLIENT_AUTHENTICATION_FLOW) == null) clientAuthFlow(realm);
        if (realm.getFlowByAlias(FIRST_BROKER_LOGIN_FLOW) == null) firstBrokerLoginFlow(realm, false);
        if (realm.getFlowByAlias(SAML_ECP_FLOW) == null) samlEcpProfile(realm);
        if (realm.getFlowByAlias(DOCKER_AUTH) == null) dockerAuthenticationFlow(realm);
    }

    public static void migrateFlows(RealmModel realm) {
        if (realm.getFlowByAlias(BROWSER_FLOW) == null) browserFlow(realm, true);
        if (realm.getFlowByAlias(DIRECT_GRANT_FLOW) == null) directGrantFlow(realm, true);
        if (realm.getFlowByAlias(REGISTRATION_FLOW) == null) registrationFlow(realm, true);
        if (realm.getFlowByAlias(RESET_CREDENTIALS_FLOW) == null) resetCredentialsFlow(realm);
        if (realm.getFlowByAlias(CLIENT_AUTHENTICATION_FLOW) == null) clientAuthFlow(realm);
        if (realm.getFlowByAlias(FIRST_BROKER_LOGIN_FLOW) == null) firstBrokerLoginFlow(realm, true);
        if (realm.getFlowByAlias(SAML_ECP_FLOW) == null) samlEcpProfile(realm);
        if (realm.getFlowByAlias(DOCKER_AUTH) == null) dockerAuthenticationFlow(realm);
    }

    public static void registrationFlow(RealmModel realm, boolean migrate) {
        AuthenticationFlowModel registrationFlow = new AuthenticationFlowModel();
        registrationFlow.setAlias(REGISTRATION_FLOW);
        registrationFlow.setDescription("Registration flow");
        registrationFlow.setProviderId("basic-flow");
        registrationFlow.setTopLevel(true);
        registrationFlow.setBuiltIn(true);
        registrationFlow = realm.addAuthenticationFlow(registrationFlow);
        realm.setRegistrationFlow(registrationFlow);

        AuthenticationFlowModel registrationFormFlow = new AuthenticationFlowModel();
        registrationFormFlow.setAlias(REGISTRATION_FORM_FLOW);
        registrationFormFlow.setDescription("Registration form");
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

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(registrationFormFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        execution.setAuthenticator("registration-terms-and-conditions");
        execution.setPriority(70);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);
    }

    public static void browserFlow(RealmModel realm) {
        browserFlow(realm, false);
    }

    private static boolean hasCredentialType(RealmModel realm, String type) {
        return realm.getRequiredCredentialsStream().anyMatch(r -> Objects.equals(r.getType(), type));
    }

    public static void resetCredentialsFlow(RealmModel realm) {
        AuthenticationFlowModel reset = new AuthenticationFlowModel();
        reset.setAlias(RESET_CREDENTIALS_FLOW);
        reset.setDescription("Reset credentials for a user if they forgot their password or something");
        reset.setProviderId("basic-flow");
        reset.setTopLevel(true);
        reset.setBuiltIn(true);
        reset = realm.addAuthenticationFlow(reset);
        realm.setResetCredentialsFlow(reset);

        // username
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(reset.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("reset-credentials-choose-user");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // send email
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(reset.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("reset-credential-email");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // password
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(reset.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("reset-password");
        execution.setPriority(30);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        AuthenticationFlowModel conditionalOTP = new AuthenticationFlowModel();
        conditionalOTP.setTopLevel(false);
        conditionalOTP.setBuiltIn(true);
        conditionalOTP.setAlias("Reset - Conditional OTP");
        conditionalOTP.setDescription("Flow to determine if the OTP should be reset or not. Set to REQUIRED to force.");
        conditionalOTP.setProviderId("basic-flow");
        conditionalOTP = realm.addAuthenticationFlow(conditionalOTP);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(reset.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
        execution.setFlowId(conditionalOTP.getId());
        execution.setPriority(40);
        execution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(conditionalOTP.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("conditional-user-configured");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(conditionalOTP.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("reset-otp");
        execution.setPriority(20);
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
        AuthenticationFlowModel conditionalOTP = new AuthenticationFlowModel();
        conditionalOTP.setTopLevel(false);
        conditionalOTP.setBuiltIn(true);
        conditionalOTP.setAlias("Direct Grant - Conditional OTP");
        conditionalOTP.setDescription("Flow to determine if the OTP is required for the authentication");
        conditionalOTP.setProviderId("basic-flow");
        conditionalOTP = realm.addAuthenticationFlow(conditionalOTP);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(grant.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
        if (migrate && hasCredentialType(realm, RequiredCredentialModel.TOTP.getType())) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        }
        execution.setFlowId(conditionalOTP.getId());
        execution.setPriority(30);
        execution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(conditionalOTP.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("conditional-user-configured");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(conditionalOTP.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("direct-grant-validate-otp");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);
    }

    public static void browserFlow(RealmModel realm, boolean migrate) {
        AuthenticationFlowModel browser = new AuthenticationFlowModel();
        browser.setAlias(BROWSER_FLOW);
        browser.setDescription("Browser based authentication");
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

        AuthenticationFlowModel conditionalOTP = new AuthenticationFlowModel();
        conditionalOTP.setTopLevel(false);
        conditionalOTP.setBuiltIn(true);
        conditionalOTP.setAlias("Browser - Conditional 2FA");
        conditionalOTP.setDescription("Flow to determine if any 2FA is required for the authentication");
        conditionalOTP.setProviderId("basic-flow");
        conditionalOTP = realm.addAuthenticationFlow(conditionalOTP);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(forms.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
        if (migrate && hasCredentialType(realm, RequiredCredentialModel.TOTP.getType())) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        }
        execution.setFlowId(conditionalOTP.getId());
        execution.setPriority(20);
        execution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(conditionalOTP.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("conditional-user-configured");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        if (Profile.isFeatureEnabled(Profile.Feature.PASSKEYS)) {
            AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
            configModel.setAlias("browser-conditional-credential");
            configModel.setConfig(Map.of("credentials", WebAuthnCredentialModel.TYPE_PASSWORDLESS));
            configModel = realm.addAuthenticatorConfig(configModel);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOTP.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator("conditional-credential");
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);
            execution.setAuthenticatorConfig(configModel.getId());
            realm.addAuthenticatorExecution(execution);
        }

        // otp processing
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(conditionalOTP.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        if (migrate && hasCredentialType(realm, RequiredCredentialModel.TOTP.getType())) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        }
        execution.setAuthenticator("auth-otp-form");
        execution.setPriority(30);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // webauthn as disabled
        if (Profile.isFeatureEnabled(Profile.Feature.WEB_AUTHN)) {
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOTP.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
            execution.setAuthenticator("webauthn-authenticator");
            execution.setPriority(40);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);
        }

        // recovery-codes as disabled
        if (Profile.isFeatureEnabled(Profile.Feature.RECOVERY_CODES)) {
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOTP.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
            execution.setAuthenticator("auth-recovery-authn-code-form");
            execution.setPriority(50);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);
        }

        addOrganizationBrowserFlowStep(realm, browser);
    }

    public static void addIdentityProviderAuthenticator(RealmModel realm, String defaultProvider) {
        String browserFlowId = realm.getAuthenticationFlowsStream()
                .filter(f -> Objects.equals(f.getAlias(), DefaultAuthenticationFlows.BROWSER_FLOW))
                .map(AuthenticationFlowModel::getId)
                .findFirst()
                .orElse(null);

        if (browserFlowId != null) {
            if (realm.getAuthenticationExecutionsStream(browserFlowId)
                    .anyMatch(e -> Objects.equals(e.getAuthenticator(), "identity-provider-redirector")))
                return;

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
                configModel.setAlias(defaultProvider);
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

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(clients.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator("client-secret-jwt");
        execution.setPriority(30);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(clients.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator("client-x509");
        execution.setPriority(40);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        if (Profile.isFeatureEnabled(Feature.CLIENT_AUTH_FEDERATED)) {
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(clients.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setAuthenticator("federated-jwt");
            execution.setPriority(50);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);
        }
    }

    public static void firstBrokerLoginFlow(RealmModel realm, boolean migrate) {
        AuthenticationFlowModel firstBrokerLogin = new AuthenticationFlowModel();
        firstBrokerLogin.setAlias(FIRST_BROKER_LOGIN_FLOW);
        firstBrokerLogin.setDescription("Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account");
        firstBrokerLogin.setProviderId("basic-flow");
        firstBrokerLogin.setTopLevel(true);
        firstBrokerLogin.setBuiltIn(true);
        firstBrokerLogin = realm.addAuthenticationFlow(firstBrokerLogin);
        realm.setFirstBrokerLoginFlow(firstBrokerLogin);

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

        AuthenticationFlowModel uniqueOrExistingFlow = new AuthenticationFlowModel();
        uniqueOrExistingFlow.setTopLevel(false);
        uniqueOrExistingFlow.setBuiltIn(true);
        uniqueOrExistingFlow.setAlias("User creation or linking");
        uniqueOrExistingFlow.setDescription("Flow for the existing/non-existing user alternatives");
        uniqueOrExistingFlow.setProviderId("basic-flow");
        uniqueOrExistingFlow = realm.addAuthenticationFlow(uniqueOrExistingFlow);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(firstBrokerLogin.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setFlowId(uniqueOrExistingFlow.getId());
        execution.setPriority(20);
        execution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        AuthenticatorConfigModel createUserIfUniqueConfig = new AuthenticatorConfigModel();
        createUserIfUniqueConfig.setAlias(IDP_CREATE_UNIQUE_USER_CONFIG_ALIAS);
        config = new HashMap<>();
        config.put("require.password.update.after.registration", "false");
        createUserIfUniqueConfig.setConfig(config);
        createUserIfUniqueConfig = realm.addAuthenticatorConfig(createUserIfUniqueConfig);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(uniqueOrExistingFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator("idp-create-user-if-unique");
        execution.setPriority(10);
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
        execution.setParentFlow(uniqueOrExistingFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setFlowId(linkExistingAccountFlow.getId());
        execution.setPriority(20);
        execution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(linkExistingAccountFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("idp-confirm-link");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        AuthenticationFlowModel accountVerificationOptions = new AuthenticationFlowModel();
        accountVerificationOptions.setTopLevel(false);
        accountVerificationOptions.setBuiltIn(true);
        accountVerificationOptions.setAlias("Account verification options");
        accountVerificationOptions.setDescription("Method with which to verity the existing account");
        accountVerificationOptions.setProviderId("basic-flow");
        accountVerificationOptions = realm.addAuthenticationFlow(accountVerificationOptions);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(linkExistingAccountFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setFlowId(accountVerificationOptions.getId());
        execution.setPriority(20);
        execution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(accountVerificationOptions.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator("idp-email-verification");
        execution.setPriority(10);
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
        execution.setParentFlow(accountVerificationOptions.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setFlowId(verifyByReauthenticationAccountFlow.getId());
        execution.setPriority(20);
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

        AuthenticationFlowModel conditionalOTP = new AuthenticationFlowModel();
        conditionalOTP.setTopLevel(false);
        conditionalOTP.setBuiltIn(true);
        conditionalOTP.setAlias("First broker login - Conditional 2FA");
        conditionalOTP.setDescription("Flow to determine if any 2FA is required for the authentication");
        conditionalOTP.setProviderId("basic-flow");
        conditionalOTP = realm.addAuthenticationFlow(conditionalOTP);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(verifyByReauthenticationAccountFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
        if (migrate) {
            // Try to read OTP requirement from browser flow
            AuthenticationFlowModel browserFlow = realm.getBrowserFlow();
            if (browserFlow == null) {
                browserFlow = realm.getFlowByAlias(DefaultAuthenticationFlows.BROWSER_FLOW);
            }
            if (browserFlow != null) {
                List<AuthenticationExecutionModel> browserExecutions = new LinkedList<>();
                KeycloakModelUtils.deepFindAuthenticationExecutions(realm, browserFlow, browserExecutions);
                for (AuthenticationExecutionModel browserExecution : browserExecutions) {
                    if (browserExecution.isAuthenticatorFlow()){
                        if (realm.getAuthenticationExecutionsStream(browserExecution.getFlowId())
                                .anyMatch(e -> e.getAuthenticator().equals("auth-otp-form"))){
                            execution.setRequirement(browserExecution.getRequirement());
                        }
                    }
                }
            }
        }
        execution.setFlowId(conditionalOTP.getId());
        execution.setPriority(20);
        execution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(conditionalOTP.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("conditional-user-configured");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        if (Profile.isFeatureEnabled(Profile.Feature.PASSKEYS)) {
            AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
            configModel.setAlias("first-broker-login-conditional-credential");
            configModel.setConfig(Map.of("credentials", WebAuthnCredentialModel.TYPE_PASSWORDLESS));
            configModel = realm.addAuthenticatorConfig(configModel);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOTP.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator("conditional-credential");
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);
            execution.setAuthenticatorConfig(configModel.getId());
            realm.addAuthenticatorExecution(execution);
        }

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(conditionalOTP.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        if (migrate && hasCredentialType(realm, RequiredCredentialModel.TOTP.getType())) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        }
        execution.setAuthenticator("auth-otp-form");
        execution.setPriority(30);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // webauthn as disabled
        if (Profile.isFeatureEnabled(Profile.Feature.WEB_AUTHN)) {
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOTP.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
            execution.setAuthenticator("webauthn-authenticator");
            execution.setPriority(40);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);
        }

        // recovery-codes as disabled
        if (Profile.isFeatureEnabled(Profile.Feature.RECOVERY_CODES)) {
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOTP.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
            execution.setAuthenticator("auth-recovery-authn-code-form");
            execution.setPriority(50);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);
        }

        addOrganizationFirstBrokerFlowStep(realm, firstBrokerLogin);
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

    private static void addOrganizationFirstBrokerFlowStep(RealmModel realm, AuthenticationFlowModel flow) {
        if (!Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
            return;
        }
        if (!Config.getAdminRealm().equals(realm.getName())) {
            // do not add the org flows to the master realm for now.
            AuthenticationFlowModel conditionalOrg = new AuthenticationFlowModel();
            conditionalOrg.setTopLevel(false);
            conditionalOrg.setBuiltIn(true);
            conditionalOrg.setAlias("First Broker Login - Conditional Organization");
            conditionalOrg.setDescription("Flow to determine if the authenticator that adds organization members is to be used");
            conditionalOrg.setProviderId("basic-flow");
            conditionalOrg = realm.addAuthenticationFlow(conditionalOrg);
            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(flow.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
            execution.setFlowId(conditionalOrg.getId());
            execution.setPriority(60);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOrg.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator("conditional-user-configured");
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOrg.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator("idp-add-organization-member");
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);
        }
    }

    public static void addOrganizationBrowserFlowStep(RealmModel realm, AuthenticationFlowModel flow) {
        if (!Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
            return;
        }
        if (!Config.getAdminRealm().equals(realm.getName())) {
            // do not add the org flows to the master realm for now.
            AuthenticationFlowModel organizations = new AuthenticationFlowModel();
            organizations.setTopLevel(false);
            organizations.setBuiltIn(true);
            organizations.setAlias("Organization");
            organizations.setProviderId("basic-flow");
            organizations = realm.addAuthenticationFlow(organizations);
            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(flow.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setFlowId(organizations.getId());
            execution.setPriority(26);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);

            AuthenticationFlowModel conditionalOrg = new AuthenticationFlowModel();
            conditionalOrg.setTopLevel(false);
            conditionalOrg.setBuiltIn(true);
            conditionalOrg.setAlias("Browser - Conditional Organization");
            conditionalOrg.setDescription("Flow to determine if the organization identity-first login is to be used");
            conditionalOrg.setProviderId("basic-flow");
            conditionalOrg = realm.addAuthenticationFlow(conditionalOrg);
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(organizations.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
            execution.setFlowId(conditionalOrg.getId());
            execution.setPriority(10);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOrg.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator("conditional-user-configured");
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOrg.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setAuthenticator("organization");
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);
        }
    }
}
