/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.federation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.credential.PasswordUserCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

/**
 * UserStorage implementation created in Keycloak 4.8.3. It is used for backwards compatibility testing. Future Keycloak versions
 * should work fine without a need to change the code of this provider.
 *
 * TODO: Have some good mechanims to make sure that source code of this provider is really compatible with Keycloak 4.8.3
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BackwardsCompatibilityUserStorage implements UserLookupProvider, UserStorageProvider, UserRegistrationProvider,
        CredentialInputUpdater, CredentialInputValidator, UserQueryProvider {

    private static final Logger log = Logger.getLogger(BackwardsCompatibilityUserStorage.class);

    protected final Map<String, MyUser> users;
    protected final ComponentModel model;
    protected final KeycloakSession session;

    public BackwardsCompatibilityUserStorage(KeycloakSession session, ComponentModel model, Map<String, MyUser> users) {
        this.session = session;
        this.model = model;
        this.users = users;
    }

    private static String translateUserName(String userName) {
        return userName == null ? null : userName.toLowerCase();
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        final String username = storageId.getExternalId();
        if (!users.containsKey(translateUserName(username))) return null;

        return createUser(realm, username);
    }

    private UserModel createUser(RealmModel realm, String username) {
        return new AbstractUserAdapterFederatedStorage(session, realm,  model) {
            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public void setUsername(String username1) {
                if (!username1.equals(username)) {
                    throw new RuntimeException("Unsupported to change username");
                }
            }

        };
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        if (CredentialModel.PASSWORD.equals(credentialType)
                || isOTPType(credentialType)) {
            return true;
        } else {
            log.infof("Unsupported credential type: %s", credentialType);
            return false;
        }
    }

    private boolean isOTPType(String credentialType) {
        return CredentialModel.OTP.equals(credentialType)
                || CredentialModel.HOTP.equals(credentialType)
                || CredentialModel.TOTP.equals(credentialType);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) return false;

        if (input.getType().equals(UserCredentialModel.PASSWORD)) {

            // Compatibility with 4.8.3 - Using "legacy" type PasswordUserCredentialModel
            if (!(input instanceof PasswordUserCredentialModel)) {
                log.warn("Input is not PasswordUserCredentialModel");
                return false;
            }

            PasswordUserCredentialModel userCredentialModel = (PasswordUserCredentialModel) input;

            // Those are not supposed to be set when calling this method in Keycloak 4.8.3 for password credential
            assertNull(userCredentialModel.getDevice());
            assertNull(userCredentialModel.getAlgorithm());

            PasswordPolicy policy = session.getContext().getRealm().getPasswordPolicy();
            PasswordHashProvider hashProvider = getHashProvider(policy);

            CredentialModel newPassword = new CredentialModel();
            newPassword.setType(CredentialModel.PASSWORD);
            long createdDate = Time.currentTimeMillis();
            newPassword.setCreatedDate(createdDate);

            // Compatibility with 4.8.3 - Using "legacy" signature of the method on hashProvider
            hashProvider.encode(userCredentialModel.getValue(), policy.getHashIterations(), newPassword);

            // Test expected values of credentialModel
            assertEquals(newPassword.getAlgorithm(), policy.getHashAlgorithm());
            assertNotNull(newPassword.getValue());
            assertNotNull(newPassword.getSalt());

            users.get(translateUserName(user.getUsername())).hashedPassword = newPassword;

            UserCache userCache = session.userCache();
            if (userCache != null) {
                userCache.evict(realm, user);
            }
            return true;
        } else if (isOTPType(input.getType())) {
            UserCredentialModel otpCredential = (UserCredentialModel) input;

            // Those are not supposed to be set when calling this method in Keycloak 4.8.3 for password credential
            assertNull(otpCredential.getDevice());
            assertNull(otpCredential.getAlgorithm());

            OTPPolicy otpPolicy = session.getContext().getRealm().getOTPPolicy();

            CredentialModel newOTP = new CredentialModel();
            newOTP.setType(input.getType());
            long createdDate = Time.currentTimeMillis();
            newOTP.setCreatedDate(createdDate);
            newOTP.setValue(otpCredential.getValue());

            newOTP.setCounter(otpPolicy.getInitialCounter());
            newOTP.setDigits(otpPolicy.getDigits());
            newOTP.setAlgorithm(otpPolicy.getAlgorithm());
            newOTP.setPeriod(otpPolicy.getPeriod());

            users.get(translateUserName(user.getUsername())).otp = newOTP;

            return true;
        } else {
            log.infof("Attempt to update unsupported credential of type: %s", input.getType());
            return false;
        }
    }

    protected PasswordHashProvider getHashProvider(PasswordPolicy policy) {
        PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, policy.getHashAlgorithm());
        if (hash == null) {
            log.warnv("Realm PasswordPolicy PasswordHashProvider {0} not found", policy.getHashAlgorithm());
            return session.getProvider(PasswordHashProvider.class, PasswordPolicy.HASH_ALGORITHM_DEFAULT);
        }
        return hash;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (isOTPType(credentialType)) {
            MyUser myUser = getMyUser(user);
            myUser.otp = null;
        } else {
            log.infof("Unsupported to disable credential of type: %s", credentialType);
        }
    }

    private MyUser getMyUser(UserModel user) {
        return users.get(translateUserName(user.getUsername()));
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        Set<String> types = new HashSet<>();

        MyUser myUser = getMyUser(user);
        if (myUser != null && myUser.otp != null) {
            types.add(CredentialModel.OTP);
        }

        return types;
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        // Always assume that password is supported
        if (CredentialModel.PASSWORD.equals(credentialType)) return true;
        MyUser myUser = getMyUser(user);
        if (myUser == null) return false;

        if (isOTPType(credentialType) && myUser.otp != null) {
            return true;
        } else {
            log.infof("Not supported credentialType '%s' for user '%s'", credentialType, user.getUsername());
            return false;
        }
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        MyUser myUser = users.get(translateUserName(user.getUsername()));
        if (myUser == null) return false;

        if (input.getType().equals(UserCredentialModel.PASSWORD)) {
            if (!(input instanceof PasswordUserCredentialModel)) return false;
            CredentialModel hashedPassword = myUser.hashedPassword;
            if (hashedPassword == null) {
                log.warnf("Password not set for user %s", user.getUsername());
                return false;
            }

            PasswordUserCredentialModel userCredentialModel = (PasswordUserCredentialModel) input;

            // Those are not supposed to be set when calling this method in Keycloak 4.8.3 for password credential
            assertNull(userCredentialModel.getDevice());
            assertNull(userCredentialModel.getAlgorithm());

            PasswordPolicy policy = session.getContext().getRealm().getPasswordPolicy();
            PasswordHashProvider hashProvider = getHashProvider(policy);

            String rawPassword = userCredentialModel.getValue();

            // Compatibility with 4.8.3 - using "legacy" signature of this method
            return hashProvider.verify(rawPassword, hashedPassword);
        } else if (isOTPType(input.getType())) {
            UserCredentialModel otpCredential = (UserCredentialModel) input;

            // Special hardcoded OTP, which is always considered valid
            if ("123456".equals(otpCredential.getValue())) {
                return true;
            }

            CredentialModel storedOTPCredential = myUser.otp;
            if (storedOTPCredential == null) {
                log.warnf("Not found credential for the user %s", user.getUsername());
                return false;
            }

            TimeBasedOTP validator = new TimeBasedOTP(storedOTPCredential.getAlgorithm(), storedOTPCredential.getDigits(),
                    storedOTPCredential.getPeriod(), realm.getOTPPolicy().getLookAheadWindow());
            return validator.validateTOTP(otpCredential.getValue(), storedOTPCredential.getValue().getBytes());
        } else {
            log.infof("Not supported to validate credential of type '%s' for user '%s'", input.getType(), user.getUsername());
            return false;
        }
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        if (!users.containsKey(translateUserName(username))) return null;

        return createUser(realm, username);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        users.put(translateUserName(username), new MyUser(username));
        return createUser(realm, username);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return users.remove(translateUserName(user.getUsername())) != null;
    }


    // UserQueryProvider methods

    @Override
    public int getUsersCount(RealmModel realm) {
        return users.size();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, -1, -1);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return users.values()
                .stream()
                .skip(firstResult).limit(maxResults)
                .map(myUser -> createUser(realm, myUser.username))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        UserModel user = getUserByUsername(realm, search);
        return user == null ? Collections.emptyList() : Arrays.asList(user);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        // Assume that this is not supported
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        // Assume that this is not supported
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        // Assume that this is not supported
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        // Assume that this is not supported
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        // Assume that this is not supported
        return Collections.emptyList();
    }

    @Override
    public void close() {
    }


    static class MyUser {

        private String username;
        private CredentialModel hashedPassword;
        private CredentialModel otp;

        private MyUser(String username) {
            this.username = username;
        }

        public CredentialModel getOtp() {
            return otp;
        }
    }


    private void assertNull(Object obj) {
        if (obj != null) {
            throw new AssertionError("Object wasn't null");
        }
    }

    private void assertNotNull(Object obj) {
        if (obj == null) {
            throw new AssertionError("Object was null");
        }
    }

    private void assertEquals(Object obj1, Object obj2) {
        if (!(obj1.equals(obj2))) {
            throw new AssertionError("Objects not equals");
        }
    }

}
