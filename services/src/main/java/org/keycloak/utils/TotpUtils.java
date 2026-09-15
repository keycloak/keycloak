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

package org.keycloak.utils;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.Base32;
import org.keycloak.theme.TemplatingUtil;
import org.keycloak.theme.Theme;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TotpUtils {

    private static final Logger logger = Logger.getLogger(TotpUtils.class);

    public static String encode(String totpSecret) {
        String encoded = Base32.encode(totpSecret.getBytes());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < encoded.length(); i += 4) {
            sb.append(encoded.substring(i, i + 4 < encoded.length() ? i + 4 : encoded.length()));
            if (i + 4 < encoded.length()) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Generates a QR code using the realm name as issuer. Use when no session is available.
     *
     * @deprecated Use {@link #qrCode(KeycloakSession, String, RealmModel, UserModel)} instead to get locale-aware issuer names.
     */
    @Deprecated(since = "26.7.0")
    public static String qrCode(String totpSecret, RealmModel realm, UserModel user) {
        return qrCode(null, totpSecret, realm, user);
    }

    /**
     * Generates a QR code using a locale-aware realm display name as issuer. Preferred when a session is available.
     */
    public static String qrCode(KeycloakSession session, String totpSecret, RealmModel realm, UserModel user) {
        try {
            String keyUri;
            if (session != null) {
                String issuerName = getIssuerName(session, realm, user);
                keyUri = realm.getOTPPolicy().getKeyURI(issuerName, user.getUsername(), totpSecret);
            } else {
                keyUri = realm.getOTPPolicy().getKeyURI(realm, user, totpSecret);
            }

            int width = 246;
            int height = 246;

            return QRCodeUtils.encodeAsQRString(keyUri, width, height);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getIssuerName(KeycloakSession session, RealmModel realm, UserModel user) {
        String displayName = realm.getDisplayName();
        if (StringUtil.isNullOrEmpty(displayName)) {
            return realm.getName();
        }

        try {
            Locale locale = session.getContext().resolveLocale(user);
            Properties messages = session.theme().getTheme(Theme.Type.LOGIN).getEnhancedMessages(realm, locale);
            return TemplatingUtil.resolveVariables(displayName, messages);
        } catch (IOException e) {
            logger.warn("Failed to load messages to resolve realm display name", e);
            return displayName;
        }
    }

}
