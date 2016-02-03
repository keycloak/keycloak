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
package org.keycloak.forms.login.freemarker.model;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.HmacOTP;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TotpBean {

    private final String totpSecret;
    private final String totpSecretEncoded;
    private final boolean enabled;
    private final String contextUrl;
    private final String keyUri;

    public TotpBean(RealmModel realm, UserModel user, URI baseUri) {
        this.enabled = user.isOtpEnabled();
        this.contextUrl = baseUri.getPath();
        
        this.totpSecret = HmacOTP.generateSecret(20);
        this.totpSecretEncoded = Base32.encode(totpSecret.getBytes());
        this.keyUri = realm.getOTPPolicy().getKeyURI(realm, user, this.totpSecret);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public String getTotpSecretEncoded() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totpSecretEncoded.length(); i += 4) {
            sb.append(totpSecretEncoded.substring(i, i + 4 < totpSecretEncoded.length() ? i + 4 : totpSecretEncoded.length()));
            if (i + 4 < totpSecretEncoded.length()) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public String getTotpSecretQrCodeUrl() throws UnsupportedEncodingException {
        String contents = URLEncoder.encode(keyUri, "utf-8");
        return contextUrl + "qrcode" + "?size=246x246&contents=" + contents;
    }

}

