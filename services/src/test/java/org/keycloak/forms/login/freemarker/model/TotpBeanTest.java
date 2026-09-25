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

package org.keycloak.forms.login.freemarker.model;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

public class TotpBeanTest {

    private static final String REALM_NAME = "myrealm";
    private static final String USERNAME = "tester";
    // 20 bytes -> exactly 32 Base32 chars, no padding, so the URI is deterministic
    private static final String SECRET = "12345678901234567890";

    @Test
    public void keyUriIsExposedAndMatchesTheUriEncodedInTheQrCode() {

        OTPPolicy policy = totpPolicy();
        TotpBean bean = new TotpBean(session(), realm(policy, ""), user(), null, SECRET);

        // The exposed key URI must be exactly the otpauth:// URI that TotpUtils encodes into the QR code,
        // i.e. the canonical value produced by OTPPolicy#getKeyURI for the same issuer/account/secret.
        String expected = policy.getKeyURI(REALM_NAME, USERNAME, SECRET);
        assertThat(bean.getTotpSecretKeyUri(), is(expected));
    }

    @Test
    public void keyUriContainsTypeIssuerAccountAndSecret() {

        TotpBean bean = new TotpBean(session(), realm(totpPolicy(), ""), user(), null, SECRET);
        String keyUri = bean.getTotpSecretKeyUri();

        assertThat(keyUri, startsWith("otpauth://totp/"));
        assertThat(keyUri, containsString(REALM_NAME + ":" + USERNAME));
        assertThat(keyUri, containsString("issuer=" + REALM_NAME));
        // Base32 of the secret, no padding for a 20-byte secret
        assertThat(keyUri, containsString("secret=" + org.keycloak.models.utils.Base32.encode(SECRET.getBytes())));
    }

    @Test
    public void keyUriIssuerFallsBackToRealmNameWhenDisplayNameIsBlank() {

        TotpBean bean = new TotpBean(session(), realm(totpPolicy(), ""), user(), null, SECRET);

        assertThat(bean.getTotpSecretKeyUri(), containsString("/" + REALM_NAME + ":"));
        assertThat(bean.getTotpSecretKeyUri(), containsString("issuer=" + REALM_NAME));
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
        return proxy(KeycloakSession.class, (proxy, method, args) -> {
            if (method.getName().equals("getAllProviders")) {
                return Collections.emptySet();
            }
            return null;
        });
    }

    private static RealmModel realm(OTPPolicy policy, String displayName) {
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
        SubjectCredentialManager credentialManager = proxy(SubjectCredentialManager.class, (proxy, method, args) -> {
            if (method.getName().equals("isConfiguredFor")) {
                return Boolean.FALSE;
            }
            return null;
        });

        return proxy(UserModel.class, (proxy, method, args) -> {
            switch (method.getName()) {
                case "credentialManager":
                    return credentialManager;
                case "getUsername":
                    return USERNAME;
                default:
                    return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(TotpBeanTest.class.getClassLoader(), new Class[]{type}, handler);
    }
}
