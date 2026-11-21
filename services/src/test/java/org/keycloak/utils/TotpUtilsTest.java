/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

public class TotpUtilsTest {

    private static final String REALM_NAME = "myrealm";
    private static final String USERNAME = "tester";
    // 20 bytes -> exactly 32 Base32 chars, no padding, so the URI is deterministic
    private static final String SECRET = "12345678901234567890";

    @Test
    public void keyUriWithoutSessionUsesRealmDisplayNameAsIssuer() {

        String keyUri = TotpUtils.keyUri(null, SECRET, realm("My Realm"), user());

        assertThat(keyUri, startsWith("otpauth://totp/"));
        // space is encoded as %20 in both label and issuer parameter
        assertThat(keyUri, containsString("My%20Realm:" + USERNAME));
        assertThat(keyUri, containsString("issuer=My%20Realm"));
    }

    @Test
    public void keyUriWithoutSessionFallsBackToRealmNameWhenDisplayNameBlank() {

        String keyUri = TotpUtils.keyUri(null, SECRET, realm(""), user());

        assertThat(keyUri, containsString("/" + REALM_NAME + ":" + USERNAME));
        assertThat(keyUri, containsString("issuer=" + REALM_NAME));
    }

    @Test
    public void keyUriWithSessionFallsBackToRealmNameWhenDisplayNameBlank() {

        // With a blank display name the issuer resolves to the realm name without touching the session,
        // so the result must match the no-session path exactly.
        String withSession = TotpUtils.keyUri(session(), SECRET, realm(""), user());
        String withoutSession = TotpUtils.keyUri(null, SECRET, realm(""), user());

        assertThat(withSession, is(withoutSession));
        assertThat(withSession, containsString("issuer=" + REALM_NAME));
    }

    private static OTPPolicy totpPolicy() {
        OTPPolicy policy = new OTPPolicy();
        policy.setAlgorithm(HmacOTP.HMAC_SHA1);
        policy.setDigits(6);
        policy.setType(OTPCredentialModel.TOTP);
        policy.setPeriod(30);
        return policy;
    }

    private static KeycloakSession session() {
        // A blank realm display name short-circuits issuer resolution, so no session method is invoked.
        return proxy(KeycloakSession.class, (proxy, method, args) -> null);
    }

    private static RealmModel realm(String displayName) {
        OTPPolicy policy = totpPolicy();
        return proxy(RealmModel.class, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getOTPPolicy":
                    return policy;
                case "getName":
                    return REALM_NAME;
                case "getDisplayName":
                    return displayName;
                default:
                    return null;
            }
        });
    }

    private static UserModel user() {
        return proxy(UserModel.class, (proxy, method, args) -> {
            if (method.getName().equals("getUsername")) {
                return USERNAME;
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(TotpUtilsTest.class.getClassLoader(), new Class[]{type}, handler);
    }
}
