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

package org.keycloak.tests.admin.authentication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.authentication.forms.RegistrationRecaptcha;
import org.keycloak.authentication.forms.RegistrationRecaptchaEnterprise;
import org.keycloak.representations.idm.AuthenticatorConfigInfoRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.KerberosUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest
public class ProvidersTest extends AbstractAuthenticationTest {

    @Test
    public void testFormProviders() {
        List<Map<String, Object>> result = authMgmtResource.getFormProviders();

        Assertions.assertNotNull(result, "null result");
        Assertions.assertEquals(1, result.size(), "size");
        Map<String, Object> item = result.get(0);

        Assertions.assertEquals("registration-page-form", item.get("id"), "id");
        Assertions.assertEquals("Registration Page", item.get("displayName"), "displayName");
        Assertions.assertEquals("This is the controller for the registration page", item.get("description"), "description");
    }

    @Test
    public void testFormActionProviders() {
        List<Map<String, Object>> result = authMgmtResource.getFormActionProviders();

        List<Map<String, Object>> expected = new LinkedList<>();
        addProviderInfo(expected, RegistrationRecaptcha.PROVIDER_ID, "reCAPTCHA", "Adds Google reCAPTCHA to the form.");
        addProviderInfo(expected, RegistrationRecaptchaEnterprise.PROVIDER_ID, "reCAPTCHA Enterprise", "Adds Google reCAPTCHA Enterprise to the form.");
        addProviderInfo(expected, "registration-password-action", "Password Validation",
                "Validates that password matches password confirmation field.  It also will store password in user's credential store.");
        addProviderInfo(expected, "registration-user-creation", "Registration User Profile Creation",
                "This action must always be first! Validates the username and user profile of the user in validation phase.  " +
                        "In success phase, this will create the user in the database including his user profile.");
        addProviderInfo(expected, "registration-terms-and-conditions", "Terms and conditions",
                "Asks the user to accept terms and conditions before submitting its registration form.");

        compareProviders(expected, result);
    }

    @Test
    public void testClientAuthenticatorProviders() {
        List<Map<String, Object>> result = authMgmtResource.getClientAuthenticatorProviders();

        List<Map<String, Object>> expected = new LinkedList<>();
        addClientAuthenticatorProviderInfo(expected, "client-jwt", "Signed JWT",
                "Validates client based on signed JWT issued by client and signed with the Client private key", false);
        addClientAuthenticatorProviderInfo(expected, "client-secret", "Client Id and Secret", "Validates client based on 'client_id' and " +
                "'client_secret' sent either in request parameters or in 'Authorization: Basic' header", true);
        addClientAuthenticatorProviderInfo(expected, "client-x509", "X509 Certificate",
                "Validates client based on a X509 Certificate", false);
        addClientAuthenticatorProviderInfo(expected, "client-secret-jwt", "Signed JWT with Client Secret",
                "Validates client based on signed JWT issued by client and signed with the Client Secret", true);

        compareProviders(expected, result);
    }

    @Test
    public void testPerClientConfigDescriptions() {
        Map<String, List<ConfigPropertyRepresentation>> configs = authMgmtResource.getPerClientConfigDescription();
        Assertions.assertTrue(configs.containsKey("client-jwt"));
        Assertions.assertTrue(configs.containsKey("client-secret"));
        Assertions.assertTrue(configs.get("client-jwt").isEmpty());
        Assertions.assertTrue(configs.get("client-secret").isEmpty());
    }

    @Test
    public void testAuthenticatorConfigDescription() {
        // Try some not-existent provider
        try {
            authMgmtResource.getAuthenticatorConfigDescription("not-existent");
            Assertions.fail("Don't expected to find provider 'not-existent'");
        } catch (NotFoundException nfe) {
            // Expected
        }

        AuthenticatorConfigInfoRepresentation infoRep = authMgmtResource.getAuthenticatorConfigDescription(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID);
        Assertions.assertEquals("Create User If Unique", infoRep.getName());
        Assertions.assertEquals(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, infoRep.getProviderId());
        Assertions.assertEquals("Detect if there is existing Keycloak account with same email like identity provider. If no, create new user", infoRep.getHelpText());
        Assertions.assertEquals(1, infoRep.getProperties().size());
        Assert.assertProviderConfigProperty(infoRep.getProperties().get(0), "require.password.update.after.registration", "Require Password Update After Registration",
                null, "If this option is true and new user is successfully imported from Identity Provider to Keycloak (there is no duplicated email or username detected in Keycloak DB), then this user is required to update his password",
                "boolean");
    }


    @Test
    public void testInitialAuthenticationProviders() {
        List<Map<String, Object>> providers = authMgmtResource.getAuthenticatorProviders();
        compareProviders(expectedAuthProviders(), providers);
    }

    private List<Map<String, Object>> expectedAuthProviders() {
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        addProviderInfo(result, "auth-conditional-otp-form", "Conditional OTP Form",
                "Validates a OTP on a separate OTP form. Only shown if required based on the configured conditions.");
        addProviderInfo(result, "auth-cookie", "Cookie", "Validates the SSO cookie set by the auth server.");
        addProviderInfo(result, "auth-otp-form", "OTP Form", "Validates a OTP on a separate OTP form.");
        String kerberosHelpMessage = (KerberosUtils.isKerberosSupportExpected())
                ? "Initiates the SPNEGO protocol.  Most often used with Kerberos."
                : "DISABLED. Please enable Kerberos feature and make sure Kerberos available in your platform. Initiates the SPNEGO protocol. Most often used with Kerberos.";
        addProviderInfo(result, "auth-spnego", "Kerberos", kerberosHelpMessage);
        addProviderInfo(result, "auth-username-password-form", "Username Password Form",
                "Validates a username and password from login form.");
        addProviderInfo(result, "auth-x509-client-username-form", "X509/Validate Username Form",
                "Validates username and password from X509 client certificate received as a part of mutual SSL handshake.");
        addProviderInfo(result, "direct-grant-auth-x509-username", "X509/Validate Username",
                "Validates username and password from X509 client certificate received as a part of mutual SSL handshake.");
        addProviderInfo(result, "direct-grant-validate-otp", "OTP", "Validates the one time password supplied as a 'totp' form parameter in direct grant request");
        addProviderInfo(result, "direct-grant-validate-password", "Password",
                "Validates the password supplied as a 'password' form parameter in direct grant request");
        addProviderInfo(result, "direct-grant-validate-username", "Username Validation",
                "Validates the username supplied as a 'username' form parameter in direct grant request");
        addProviderInfo(result, "docker-http-basic-authenticator", "Docker Authenticator", "Uses HTTP Basic authentication to validate docker users, returning a docker error token on auth failure");
        addProviderInfo(result, "http-basic-authenticator", "HTTP Basic Authentication", "Validates username and password from Authorization HTTP header");
        addProviderInfo(result, "identity-provider-redirector", "Identity Provider Redirector", "Redirects to default Identity Provider or Identity Provider specified with kc_idp_hint query parameter");
        addProviderInfo(result, "idp-auto-link", "Automatically set existing user", "Automatically set existing user to authentication context without any verification");
        addProviderInfo(result, "idp-confirm-link", "Confirm link existing account", "Show the form where user confirms if he wants " +
                "to link identity provider with existing account or rather edit user profile data retrieved from identity provider to avoid conflict");
        addProviderInfo(result, "idp-confirm-override-link", "Confirm override existing link", "Confirm override the link if there is an existing broker user linked to the account.");
        addProviderInfo(result, "idp-create-user-if-unique", "Create User If Unique", "Detect if there is existing Keycloak account " +
                "with same email like identity provider. If no, create new user");
        addProviderInfo(result, "idp-email-verification", "Verify existing account by Email", "Email verification of existing Keycloak " +
                "user, that wants to link his user account with identity provider");
        addProviderInfo(result, "idp-review-profile", "Review Profile",
                "User reviews and updates profile data retrieved from Identity Provider in the displayed form");
        addProviderInfo(result, "idp-username-password-form", "Username Password Form for identity provider reauthentication",
                "Validates a password from login form. Username may be already known from identity provider authentication");
        addProviderInfo(result, "reset-credential-email", "Send Reset Email", "Send email to user and wait for response.");
        addProviderInfo(result, "reset-credentials-choose-user", "Choose User", "Choose a user to reset credentials for");
        addProviderInfo(result, "reset-otp", "Reset OTP", "Removes existing OTP configurations (if chosen) and sets the 'Configure OTP' required action.");
        addProviderInfo(result, "reset-password", "Reset Password", "Sets the Update Password required action if execution is REQUIRED.  " +
                "Will also set it if execution is OPTIONAL and the password is currently configured for it.");
        addProviderInfo(result, "webauthn-authenticator", "WebAuthn Authenticator", "Authenticator for WebAuthn. Usually used for WebAuthn two-factor authentication");
        addProviderInfo(result, "webauthn-authenticator-passwordless", "WebAuthn Passwordless Authenticator", "Authenticator for Passwordless WebAuthn authentication");
        addProviderInfo(result, "auth-recovery-authn-code-form", "Recovery Authentication Code Form", "Validates a Recovery Authentication Code");

        addProviderInfo(result, "auth-username-form", "Username Form",
                "Selects a user from his username.");
        addProviderInfo(result, "auth-password-form", "Password Form",
                "Validates a password from login form.");
        addProviderInfo(result, "conditional-user-role", "Condition - user role",
                "Flow is executed only if user has the given role.");
        addProviderInfo(result, "conditional-user-configured", "Condition - user configured",
                "Executes the current flow only if authenticators are configured");
        addProviderInfo(result, "conditional-user-attribute", "Condition - user attribute",
                "Flow is executed only if the user attribute exists and has the expected value");
        addProviderInfo(result, "idp-detect-existing-broker-user", "Detect existing broker user",
                "Detect if there is an existing Keycloak account with same email like identity provider. If no, throw an error.");

        addProviderInfo(result, "deny-access-authenticator", "Deny access",
                "Access will be always denied. Useful for example in the conditional flows to be used after satisfying the previous conditions");
        addProviderInfo(result, "allow-access-authenticator", "Allow access",
                "Authenticator will always successfully authenticate. Useful for example in the conditional flows to be used after satisfying the previous conditions");

        addProviderInfo(result, "conditional-level-of-authentication", "Condition - Level of Authentication",
                "Flow is executed only if the configured LOA or a higher one has been requested but not yet satisfied. After the flow is successfully finished, the LOA in the session will be updated to value prescribed by this condition.");

        addProviderInfo(result, "user-session-limits", "User session count limiter",
                "Configures how many concurrent sessions a single user is allowed to create for this realm and/or client");

        addProviderInfo(result, "idp-add-organization-member", "Organization Member Onboard", "Adds a federated user as a member of an organization");
        addProviderInfo(result, "organization", "Organization Identity-First Login", "If organizations are enabled, automatically redirects users to the corresponding identity provider.");
        addProviderInfo(result, "conditional-sub-flow-executed", "Condition - sub-flow executed", "Condition to evaluate if a sub-flow was executed successfully during the authentication process");
        addProviderInfo(result, "conditional-client-scope", "Condition - client scope", "Condition to evaluate if a configured client scope is present as a client scope of the client requesting authentication");
        addProviderInfo(result, "conditional-credential", "Condition - credential", "Condition to evaluate if a specific credential type has been used (or not used) by the user during the authentication process");

        return result;
    }

    private List<Map<String, Object>> sortProviders(List<Map<String, Object>> providers) {
        ArrayList<Map<String, Object>> sorted = new ArrayList<>(providers);
        Collections.sort(sorted, new ProviderComparator());
        return sorted;
    }

    private void compareProviders(List<Map<String, Object>> expected, List<Map<String, Object>> actual) {
        Assertions.assertEquals(expected.size(), actual.size(), "Providers count");
        // compare ignoring list and map impl types
        assertThat(normalizeResults(actual), is(normalizeResults(expected)));
    }

    private List<Map<String, Object>> normalizeResults(List<Map<String, Object>> list) {
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item: list) {
            result.add(new HashMap<>(item));
        }
        return sortProviders(result);
    }

    private void addProviderInfo(List<Map<String, Object>> list, String id, String displayName, String description) {
        HashMap<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("displayName", displayName);
        item.put("description", description);
        list.add(item);
    }

    private void addClientAuthenticatorProviderInfo(List<Map<String, Object>> list, String id, String displayName, String description, boolean supportsSecret) {
        HashMap<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("displayName", displayName);
        item.put("description", description);
        item.put("supportsSecret", supportsSecret);
        list.add(item);
    }

    private static class ProviderComparator implements Comparator<Map<String, Object>> {
        @Override
        public int compare(Map<String, Object> o1, Map<String, Object> o2) {
            return String.valueOf(o1.get("id")).compareTo(String.valueOf(o2.get("id")));
        }

    }
}
