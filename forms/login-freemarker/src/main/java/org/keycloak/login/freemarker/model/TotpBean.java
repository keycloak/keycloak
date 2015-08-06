/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.login.freemarker.model;

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
    private final String realmName;
    private final String keyUri;

    public TotpBean(RealmModel realm, UserModel user, URI baseUri) {
        this.realmName = realm.getName();
        this.enabled = user.isOtpEnabled();
        this.contextUrl = baseUri.getPath();
        
        this.totpSecret = HmacOTP.generateSecret(20);
        this.totpSecretEncoded = Base32.encode(totpSecret.getBytes());
        this.keyUri = realm.getOTPPolicy().getKeyURI(realm, this.totpSecret);
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

