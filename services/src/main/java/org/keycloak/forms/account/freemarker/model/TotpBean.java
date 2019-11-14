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

package org.keycloak.forms.account.freemarker.model;

import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.utils.TotpUtils;

import javax.ws.rs.core.UriBuilder;
import java.util.Collections;
import java.util.List;


/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TotpBean {

    private final RealmModel realm;
    private final String totpSecret;
    private final String totpSecretEncoded;
    private final String totpSecretQrCode;
    private final boolean enabled;
    private final UriBuilder uriBuilder;
    private final List<CredentialModel> otpCredentials;

    public TotpBean(KeycloakSession session, RealmModel realm, UserModel user, UriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
        this.enabled = ((OTPCredentialProvider)session.getProvider(CredentialProvider.class, "keycloak-otp")).isConfiguredFor(realm, user);
        if (enabled) {
            otpCredentials = session.userCredentialManager().getStoredCredentialsByType(realm, user, OTPCredentialModel.TYPE);
        } else {
            otpCredentials = Collections.EMPTY_LIST;
        }

        this.realm = realm;
        this.totpSecret = HmacOTP.generateSecret(20);
        this.totpSecretEncoded = TotpUtils.encode(totpSecret);
        this.totpSecretQrCode = TotpUtils.qrCode(totpSecret, realm, user);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public String getTotpSecretEncoded() {
        return totpSecretEncoded;
    }

    public String getTotpSecretQrCode() {
        return totpSecretQrCode;
    }

    public String getManualUrl() {
        return uriBuilder.replaceQueryParam("mode", "manual").build().toString();
    }

    public String getQrUrl() {
        return uriBuilder.replaceQueryParam("mode", "qr").build().toString();
    }

    public OTPPolicy getPolicy() {
        return realm.getOTPPolicy();
    }

    public List<CredentialModel> getOtpCredentials() {
        return otpCredentials;
    }

}

