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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FederatedCredentials {
    public static void updateCredential(KeycloakSession session, UserFederatedStorageProvider provider, RealmModel realm, UserModel user, UserCredentialModel cred) {
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            updatePasswordCredential(session, provider,realm, user, cred);
        } else if (UserCredentialModel.isOtp(cred.getType())) {
            updateOtpCredential(session, provider, realm, user, cred);
        } else {
            UserCredentialValueModel fedCred = getCredentialByType(provider, realm, user, cred.getType());
            if (fedCred == null) {
                fedCred.setCreatedDate(Time.toMillis(Time.currentTime()));
                fedCred.setType(cred.getType());
                fedCred.setDevice(cred.getDevice());
                fedCred.setValue(cred.getValue());

            } else {
                fedCred.setValue(cred.getValue());
            }
            provider.updateCredential(realm, user, fedCred);
        }
    }

    public static UserCredentialValueModel getCredentialByType(UserFederatedStorageProvider provider, RealmModel realm, UserModel user, String type) {
        List<UserCredentialValueModel> creds = provider.getCredentials(realm, user);
        for (UserCredentialValueModel cred : creds) {
            if (cred.getType().equals(type)) return cred;
        }
        return null;
    }

    public static LinkedList<UserCredentialValueModel> getCredentialsByType(UserFederatedStorageProvider provider, RealmModel realm, UserModel user, String type) {
        List<UserCredentialValueModel> creds = provider.getCredentials(realm, user);
        LinkedList<UserCredentialValueModel> newCreds = new LinkedList<>();
        for (UserCredentialValueModel cred : creds) {
            if (cred.getType().equals(type)) newCreds.add(cred);
        }
        return newCreds;
    }

    public static void updatePasswordCredential(KeycloakSession session, UserFederatedStorageProvider provider, RealmModel realm, UserModel user, UserCredentialModel cred) {
        UserCredentialValueModel fedCred = getCredentialByType(provider, realm, user, cred.getType());
        if (fedCred == null) {
            UserCredentialValueModel newCred = PasswordHashManager.encode(session, realm, cred.getValue());
            newCred.setCreatedDate(Time.toMillis(Time.currentTime()));
            newCred.setType(cred.getType());
            newCred.setDevice(cred.getDevice());
            provider.updateCredential(realm, user, newCred);
        } else {
            int expiredPasswordsPolicyValue = -1;
            PasswordPolicy policy = realm.getPasswordPolicy();
            if(policy != null) {
                expiredPasswordsPolicyValue = policy.getExpiredPasswords();
            }

            if (expiredPasswordsPolicyValue != -1) {
                fedCred.setType(UserCredentialModel.PASSWORD_HISTORY);

                LinkedList<UserCredentialValueModel> credentialEntities = getCredentialsByType(provider, realm, user, UserCredentialModel.PASSWORD_HISTORY);
                if (credentialEntities.size() > expiredPasswordsPolicyValue - 1) {
                    Collections.sort(credentialEntities, new Comparator<UserCredentialValueModel>() {
                        @Override
                        public int compare(UserCredentialValueModel o1, UserCredentialValueModel o2) {
                            if (o1.getCreatedDate().equals(o2.getCreatedDate())) return 0;
                            return o1.getCreatedDate() < o2.getCreatedDate() ? -1 : 1;
                        }
                    });
                    while (credentialEntities.size() > expiredPasswordsPolicyValue - 1) {
                        UserCredentialValueModel model = credentialEntities.removeFirst();
                        provider.removeCredential(realm, user, model);
                    }

                }
                provider.updateCredential(realm, user, fedCred);
                fedCred = PasswordHashManager.encode(session, realm, cred.getValue());
                fedCred.setCreatedDate(Time.toMillis(Time.currentTime()));
                fedCred.setType(cred.getType());
                fedCred.setDevice(cred.getDevice());
                provider.updateCredential(realm, user, fedCred);
            } else {
                // clear password history as it is not required anymore
                for (UserCredentialValueModel model : getCredentialsByType(provider, realm, user, UserCredentialModel.PASSWORD_HISTORY)) {
                    provider.removeCredential(realm, user, model);
                }
                UserCredentialValueModel newCred = PasswordHashManager.encode(session, realm, cred.getValue());
                newCred.setCreatedDate(Time.toMillis(Time.currentTime()));
                newCred.setType(cred.getType());
                newCred.setDevice(cred.getDevice());
                newCred.setId(fedCred.getId());
                provider.updateCredential(realm, user, newCred);
            }


        }


    }

    public static  void updateOtpCredential(KeycloakSession session, UserFederatedStorageProvider provider, RealmModel realm, UserModel user, UserCredentialModel cred) {
        LinkedList<UserCredentialValueModel> credentialEntities = getCredentialsByType(provider, realm, user, UserCredentialModel.PASSWORD_HISTORY);

        if (credentialEntities.isEmpty()) {
            UserCredentialValueModel fedCred = new UserCredentialValueModel();
            fedCred.setCreatedDate(Time.toMillis(Time.currentTime()));
            fedCred.setType(cred.getType());
            fedCred.setDevice(cred.getDevice());
            fedCred.setValue(cred.getValue());
            OTPPolicy otpPolicy = realm.getOTPPolicy();
            fedCred.setAlgorithm(otpPolicy.getAlgorithm());
            fedCred.setDigits(otpPolicy.getDigits());
            fedCred.setCounter(otpPolicy.getInitialCounter());
            fedCred.setPeriod(otpPolicy.getPeriod());
            provider.updateCredential(realm, user, fedCred);
        } else {
            OTPPolicy policy = realm.getOTPPolicy();
            if (cred.getDevice() == null) {
                for (UserCredentialValueModel model : credentialEntities) provider.removeCredential(realm, user, model);
                UserCredentialValueModel fedCred = new UserCredentialValueModel();
                fedCred.setCreatedDate(Time.toMillis(Time.currentTime()));
                fedCred.setType(cred.getType());
                fedCred.setDevice(cred.getDevice());
                fedCred.setDigits(policy.getDigits());
                fedCred.setCounter(policy.getInitialCounter());
                fedCred.setAlgorithm(policy.getAlgorithm());
                fedCred.setValue(cred.getValue());
                fedCred.setPeriod(policy.getPeriod());
                provider.updateCredential(realm, user, fedCred);
            } else {
                UserCredentialValueModel fedCred = new UserCredentialValueModel();
                for (UserCredentialValueModel model : credentialEntities) {
                    if (cred.getDevice().equals(model.getDevice())) {
                        fedCred = model;
                        break;
                    }
                }
                fedCred.setCreatedDate(Time.toMillis(Time.currentTime()));
                fedCred.setType(cred.getType());
                fedCred.setDevice(cred.getDevice());
                fedCred.setDigits(policy.getDigits());
                fedCred.setCounter(policy.getInitialCounter());
                fedCred.setAlgorithm(policy.getAlgorithm());
                fedCred.setValue(cred.getValue());
                fedCred.setPeriod(policy.getPeriod());
                provider.updateCredential(realm, user, fedCred);
            }
        }
    }


}
