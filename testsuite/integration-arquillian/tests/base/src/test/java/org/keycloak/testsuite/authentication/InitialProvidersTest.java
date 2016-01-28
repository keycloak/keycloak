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

package org.keycloak.testsuite.authentication;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class InitialProvidersTest extends AbstractAuthenticationTest {

    @Test
    public void testAuthenticationProvidersList() {

        List<Map<String, Object>> providers = authMgmtResource.getAuthenticatorProviders();
        providers = sortProviders(providers);

        compareProviders(expectedAuthProviders(), providers);
    }

    private void compareProviders(List<Map<String, Object>> expected, List<Map<String, Object>> actual) {

        Assert.assertEquals("Providers count", expected.size(), actual.size());

        Iterator<Map<String, Object>> it1 = expected.iterator();
        Iterator<Map<String, Object>> it2 = actual.iterator();

        while (it1.hasNext()) {
            Assert.assertEquals("Provider", it1.next(), it2.next());
        }
    }

    private List<Map<String, Object>> expectedAuthProviders() {
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        result.add(newClientProvider("auth-conditional-otp-form", "Conditional OTP Form", "Validates a OTP on a separate OTP form. Only shown if required based on the configured conditions."));
        result.add(newClientProvider("auth-cookie", "Cookie", "Validates the SSO cookie set by the auth server."));
        result.add(newClientProvider("auth-otp-form", "OTP Form", "Validates a OTP on a separate OTP form."));
        result.add(newClientProvider("auth-spnego", "Kerberos", "Initiates the SPNEGO protocol.  Most often used with Kerberos."));
        result.add(newClientProvider("auth-username-password-form", "Username Password Form", "Validates a username and password from login form."));
        result.add(newClientProvider("direct-grant-validate-otp", "OTP", "Validates the one time password supplied as a 'totp' form parameter in direct grant request"));
        result.add(newClientProvider("direct-grant-validate-password", "Password", "Validates the password supplied as a 'password' form parameter in direct grant request"));
        result.add(newClientProvider("direct-grant-validate-username", "Username Validation", "Validates the username supplied as a 'username' form parameter in direct grant request"));
        result.add(newClientProvider("http-basic-authenticator", null, null));
        result.add(newClientProvider("idp-confirm-link", "Confirm link existing account", "Show the form where user confirms if he wants to link identity provider with existing account or rather edit user profile data retrieved from identity provider to avoid conflict"));
        result.add(newClientProvider("idp-create-user-if-unique", "Create User If Unique", "Detect if there is existing Keycloak account with same email like identity provider. If no, create new user"));
        result.add(newClientProvider("idp-email-verification", "Verify existing account by Email", "Email verification of existing Keycloak user, that wants to link his user account with identity provider"));
        result.add(newClientProvider("idp-review-profile", "Review Profile", "User reviews and updates profile data retrieved from Identity Provider in the displayed form"));
        result.add(newClientProvider("idp-username-password-form", "Username Password Form for identity provider reauthentication", "Validates a password from login form. Username is already known from identity provider authentication"));
        result.add(newClientProvider("reset-credential-email", "Send Reset Email", "Send email to user and wait for response."));
        result.add(newClientProvider("reset-credentials-choose-user", "Choose User", "Choose a user to reset credentials for"));
        result.add(newClientProvider("reset-otp", "Reset OTP", "Sets the Configure OTP required action if execution is REQUIRED.  Will also set it if execution is OPTIONAL and the OTP is currently configured for it."));
        result.add(newClientProvider("reset-password", "Reset Password", "Sets the Update Password required action if execution is REQUIRED.  Will also set it if execution is OPTIONAL and the password is currently configured for it."));
        return result;
    }

    private Map<String, Object> newClientProvider(String id, String displayName, String description) {
        Map<String, Object> obj = new HashMap<>();
        obj.put("id", id);
        obj.put("displayName", displayName);
        obj.put("description", description);
        return obj;
    }

    private List<Map<String, Object>> sortProviders(List<Map<String, Object>> providers) {
        ArrayList<Map<String, Object>> sorted = new ArrayList<>(providers);
        Collections.sort(sorted, new ProviderComparator());
        return sorted;
    }

    private static class ProviderComparator implements Comparator<Map<String, Object>> {
        @Override
        public int compare(Map<String, Object> o1, Map<String, Object> o2) {
            return String.valueOf(o1.get("id")).compareTo(String.valueOf(o2.get("id")));
        }
    }

}
