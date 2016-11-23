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
package org.keycloak.credential;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.TimeBasedOTP;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OTPCredentialProvider implements CredentialProvider, CredentialInputValidator, CredentialInputUpdater, OnUserCache {
    private static final Logger logger = Logger.getLogger(OTPCredentialProvider.class);

    protected KeycloakSession session;

    protected List<CredentialModel> getCachedCredentials(UserModel user, String type) {
        if (!(user instanceof CachedUserModel)) return null;
        CachedUserModel cached = (CachedUserModel)user;
        if (cached.isMarkedForEviction()) return null;
        List<CredentialModel> rtn = (List<CredentialModel>)cached.getCachedWith().get(OTPCredentialProvider.class.getName() + "." + type);
        if (rtn == null) return Collections.EMPTY_LIST;
        return rtn;
    }

    protected UserCredentialStore getCredentialStore() {
        return session.userCredentialManager();
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        List<CredentialModel> creds = getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.TOTP);
        user.getCachedWith().put(OTPCredentialProvider.class.getName() + "." + CredentialModel.TOTP, creds);

    }

    public OTPCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType())) return false;

        if (!(input instanceof UserCredentialModel)) {
            logger.debug("Expected instance of UserCredentialModel for CredentialInput");
            return false;
        }
        UserCredentialModel inputModel = (UserCredentialModel)input;
        CredentialModel model = null;
        if (inputModel.getDevice() != null) {
            model = getCredentialStore().getStoredCredentialByNameAndType(realm, user, inputModel.getDevice(), CredentialModel.TOTP);
            if (model == null) {
                model = getCredentialStore().getStoredCredentialByNameAndType(realm, user, inputModel.getDevice(), CredentialModel.HOTP);
            }
        }
        if (model == null) {
            // delete all existing
            disableCredentialType(realm, user, CredentialModel.OTP);
            model = new CredentialModel();
        }

        OTPPolicy policy = realm.getOTPPolicy();
        model.setDigits(policy.getDigits());
        model.setCounter(policy.getInitialCounter());
        model.setAlgorithm(policy.getAlgorithm());
        model.setType(input.getType());
        model.setValue(inputModel.getValue());
        model.setDevice(inputModel.getDevice());
        model.setPeriod(policy.getPeriod());
        model.setCreatedDate(Time.currentTimeMillis());
        if (model.getId() == null) {
            getCredentialStore().createCredential(realm, user, model);
        } else {
            getCredentialStore().updateCredential(realm, user, model);
        }
        session.userCache().evict(realm, user);
        return true;



    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        boolean disableTOTP = false, disableHOTP = false;
        if (CredentialModel.OTP.equals(credentialType)) {
            disableTOTP = true;
            disableHOTP = true;
        } else if (CredentialModel.HOTP.equals(credentialType)) {
            disableHOTP = true;

        } else if (CredentialModel.TOTP.equals(credentialType)) {
            disableTOTP = true;
        }
        if (disableHOTP) {
            List<CredentialModel> hotp = getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.HOTP);
            for (CredentialModel cred : hotp) {
                getCredentialStore().removeStoredCredential(realm, user, cred.getId());
            }

        }
        if (disableTOTP) {
            List<CredentialModel> totp = getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.TOTP);
            if (!totp.isEmpty()) {
                for (CredentialModel cred : totp) {
                    getCredentialStore().removeStoredCredential(realm, user, cred.getId());
                }
            }

        }
        if (disableTOTP || disableHOTP) {
            session.userCache().evict(realm, user);
        }
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        if (!getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.HOTP).isEmpty()
        || !getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.TOTP).isEmpty()) {
            Set<String> set = new HashSet<>();
            set.add(CredentialModel.OTP);
            return set;
        } else {
            return Collections.EMPTY_SET;
        }
    }


    @Override
    public boolean supportsCredentialType(String credentialType) {
        return CredentialModel.OTP.equals(credentialType)
                || CredentialModel.HOTP.equals(credentialType)
                || CredentialModel.TOTP.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return false;
        if (CredentialModel.OTP.equals(credentialType)) {
            if (realm.getOTPPolicy().getType().equals(CredentialModel.HOTP)) {
                return configuredForHOTP(realm, user);
            } else {
                return configuredForTOTP(realm, user);
            }
        } else if (CredentialModel.HOTP.equals(credentialType)) {
            return configuredForHOTP(realm, user);

        } else if (CredentialModel.TOTP.equals(credentialType)) {
            return configuredForTOTP(realm, user);
        } else {
            return false;
        }

    }

    protected boolean configuredForHOTP(RealmModel realm, UserModel user) {
        return !getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.HOTP).isEmpty();
    }

    protected boolean configuredForTOTP(RealmModel realm, UserModel user) {
        List<CredentialModel> cachedCredentials = getCachedCredentials(user, CredentialModel.TOTP);
        if (cachedCredentials == null) return !getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.TOTP).isEmpty();
        return !cachedCredentials.isEmpty();
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

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (! (input instanceof UserCredentialModel)) {
            logger.debug("Expected instance of UserCredentialModel for CredentialInput");
            return false;

        }
        String token = ((UserCredentialModel)input).getValue();
        if (token == null) {
            return false;
        }

        OTPPolicy policy = realm.getOTPPolicy();
        if (realm.getOTPPolicy().getType().equals(CredentialModel.HOTP)) {
            HmacOTP validator = new HmacOTP(policy.getDigits(), policy.getAlgorithm(), policy.getLookAheadWindow());
            for (CredentialModel cred : getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.HOTP)) {
                int counter = validator.validateHOTP(token, cred.getValue(), cred.getCounter());
                if (counter < 0) continue;
                cred.setCounter(counter);
                getCredentialStore().updateCredential(realm, user, cred);
                return true;
            }
        } else {
            TimeBasedOTP validator = new TimeBasedOTP(policy.getAlgorithm(), policy.getDigits(), policy.getPeriod(), policy.getLookAheadWindow());
            List<CredentialModel> creds = getCachedCredentials(user, CredentialModel.TOTP);
            if (creds == null) {
                creds = getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.TOTP);
            } else {
                logger.debugv("Cache hit for TOTP for user {0}", user.getUsername());
            }
            for (CredentialModel cred : creds) {
                if (validator.validateTOTP(token, cred.getValue().getBytes())) {
                    return true;
                }
            }

        }
        return false;
    }
}
