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

import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.PasswordToken;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CredentialValidation {


    public static boolean validPasswordToken(RealmModel realm, UserModel user, String encodedPasswordToken) {
        try {
            JWSInput jws = new JWSInput(encodedPasswordToken);
            if (!RSAProvider.verify(jws, realm.getPublicKey())) {
                return false;
            }
            PasswordToken passwordToken = jws.readJsonContent(PasswordToken.class);
            if (!passwordToken.getRealm().equals(realm.getName())) {
                return false;
            }
            if (!passwordToken.getUser().equals(user.getId())) {
                return false;
            }
            if (Time.currentTime() - passwordToken.getTimestamp() > realm.getAccessCodeLifespanUserAction()) {
                return false;
            }
            return true;
        } catch (JWSInputException e) {
            return false;
        }
    }

    public static boolean validOTP(RealmModel realm, String token, String secret) {
        OTPPolicy policy = realm.getOTPPolicy();
        if (policy.getType().equals(UserCredentialModel.TOTP)) {
            TimeBasedOTP validator = new TimeBasedOTP(policy.getAlgorithm(), policy.getDigits(), policy.getPeriod(), policy.getLookAheadWindow());
            return validator.validateTOTP(token, secret.getBytes());
        } else {
            HmacOTP validator = new HmacOTP(policy.getDigits(), policy.getAlgorithm(), policy.getLookAheadWindow());
            int c = validator.validateHOTP(token, secret, policy.getInitialCounter());
            return c > -1;
        }

    }


}
