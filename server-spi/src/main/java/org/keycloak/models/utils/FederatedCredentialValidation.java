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
import org.keycloak.hash.PasswordHashManager;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.PasswordToken;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FederatedCredentialValidation {

    private static int hashIterations(RealmModel realm) {
        PasswordPolicy policy = realm.getPasswordPolicy();
        if (policy != null) {
            return policy.getHashIterations();
        }
        return -1;

    }

   /**
     * Will update password if hash iteration policy has changed
     *
     * @param realm
     * @param user
     * @param password
     * @return
     */
    public static boolean validPassword(KeycloakSession session, RealmModel realm, UserModel user, String password, UserCredentialValueModel fedCred) {
        return validateHashedCredential(session, realm, user, password, fedCred);

    }


    public static boolean validateHashedCredential(KeycloakSession session, RealmModel realm, UserModel user, String unhashedCredValue, UserCredentialValueModel credential) {
        if (unhashedCredValue == null || unhashedCredValue.isEmpty()) {
            return false;
        }

        boolean validated = PasswordHashManager.verify(session, realm, unhashedCredValue, credential);

        if (validated) {
            int iterations = hashIterations(realm);
            if (iterations > -1 && iterations != credential.getHashIterations()) {

                UserCredentialValueModel newCred = PasswordHashManager.encode(session, realm, unhashedCredValue);
                session.userFederatedStorage().updateCredential(realm, user, newCred);
            }

        }
        return validated;
    }

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

    public static boolean validHOTP(KeycloakSession session, RealmModel realm, UserModel user, String otp, List<UserCredentialValueModel> fedCreds) {
        UserCredentialValueModel passwordCred = null;
        OTPPolicy policy = realm.getOTPPolicy();
        HmacOTP validator = new HmacOTP(policy.getDigits(), policy.getAlgorithm(), policy.getLookAheadWindow());
        for (UserCredentialValueModel cred : fedCreds) {
            if (cred.getType().equals(UserCredentialModel.HOTP)) {
                int counter = validator.validateHOTP(otp, cred.getValue(), cred.getCounter());
                if (counter < 0) return false;
                cred.setCounter(counter);
                session.userFederatedStorage().updateCredential(realm, user, cred);
                return true;
            }
        }
        return false;

    }

    public static boolean validTOTP(RealmModel realm, UserModel user, String otp, List<UserCredentialValueModel> fedCreds) {
        UserCredentialValueModel passwordCred = null;
        OTPPolicy policy = realm.getOTPPolicy();
        TimeBasedOTP validator = new TimeBasedOTP(policy.getAlgorithm(), policy.getDigits(), policy.getPeriod(), policy.getLookAheadWindow());
        for (UserCredentialValueModel cred : fedCreds) {
            if (validator.validateTOTP(otp, cred.getValue().getBytes())) {
                return true;
            }
        }
        return false;

    }
    public static boolean validSecret(RealmModel realm, UserModel user, String secret, UserCredentialValueModel cred) {
        return cred.getValue().equals(secret);

    }

    public static boolean validCredential(KeycloakSession session, RealmModel realm, UserModel user, UserCredentialModel credential, List<UserCredentialValueModel> fedCreds) {
        if (credential.getType().equals(UserCredentialModel.PASSWORD)) {
            if (!validPassword(session, realm, user, credential.getValue(), fedCreds.get(0))) {
                return false;
            }
        } else if (credential.getType().equals(UserCredentialModel.PASSWORD_TOKEN)) {
            if (!validPasswordToken(realm, user, credential.getValue())) {
                return false;
            }
        } else if (credential.getType().equals(UserCredentialModel.TOTP)) {
            if (!validTOTP(realm, user, credential.getValue(), fedCreds)) {
                return false;
            }
        } else if (credential.getType().equals(UserCredentialModel.HOTP)) {
            if (!validHOTP(session, realm, user, credential.getValue(), fedCreds)) {
                return false;
            }
        } else if (credential.getType().equals(UserCredentialModel.SECRET)) {
            if (!validSecret(realm, user, credential.getValue(), fedCreds.get(0))) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
}
