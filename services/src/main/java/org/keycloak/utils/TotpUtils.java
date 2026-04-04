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

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.Base32;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TotpUtils {

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

    public static String qrCode(String totpSecret, RealmModel realm, UserModel user) {
        try {
            String keyUri = realm.getOTPPolicy().getKeyURI(realm, user, totpSecret);

            int width = 246;
            int height = 246;

            return QRCodeUtils.encodeAsQRString(keyUri, width, height);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
