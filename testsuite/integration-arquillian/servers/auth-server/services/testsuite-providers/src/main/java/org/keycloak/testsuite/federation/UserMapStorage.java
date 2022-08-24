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
package org.keycloak.testsuite.federation;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.PasswordUserCredentialModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.federated.UserGroupMembershipFederatedStorage;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserMapStorage implements UserLookupProvider.Streams, UserStorageProvider, UserRegistrationProvider, CredentialInputUpdater.Streams,
        CredentialInputValidator, UserGroupMembershipFederatedStorage.Streams, UserQueryProvider.Streams, ImportedUserValidation {

    private static final Logger log = Logger.getLogger(UserMapStorage.class);
    
    protected final Map<String, String> userPasswords;
    protected final ConcurrentMap<String, Set<String>> userGroups;
    protected ComponentModel model;
    protected KeycloakSession session;
    protected EditMode editMode;
    private transient Boolean importEnabled;

    public static final AtomicInteger allocations = new AtomicInteger(0);
    public static final AtomicInteger closings = new AtomicInteger(0);
    public static final AtomicInteger realmRemovals = new AtomicInteger(0);
    public static final AtomicInteger groupRemovals = new AtomicInteger(0);
    public static final AtomicInteger roleRemovals = new AtomicInteger(0);

    public UserMapStorage(KeycloakSession session, ComponentModel model, Map<String, String> userPasswords, ConcurrentMap<String, Set<String>> userGroups) {
        this.session = session;
        this.model = model;
        this.userPasswords = userPasswords;
        this.userGroups = userGroups;
        allocations.incrementAndGet();

        String editModeString = model.getConfig().getFirst(LDAPConstants.EDIT_MODE);
        if (editModeString == null) {
            this.editMode = EditMode.UNSYNCED;
        } else {
            this.editMode = EditMode.valueOf(editModeString);
        }
    }

    private static String getUserIdInMap(RealmModel realm, String userId) {
        return realm.getId() + "/" + userId;
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        final String username = storageId.getExternalId();
        if (!userPasswords.containsKey(translateUserName(username))) {
            return null;
        }

        return createUser(realm, username);
    }

    public Set<String> getUsernames() {
        return userPasswords.keySet();
    }

    private UserModel createUser(RealmModel realm, String username) {
        UserModel user;
        if (isImportEnabled()) {
            user = UserStoragePrivateUtil.userLocalStorage(session).addUser(realm, username);
            user.setEnabled(true);
            user.setFederationLink(model.getId());
        } else {
            user = new AbstractUserAdapterFederatedStorage.Streams(session, realm, model) {
                @Override
                public String getUsername() {
                    return username.toLowerCase();
                }

                @Override
                public void setUsername(String innerUsername) {
                    if (! Objects.equals(innerUsername, username.toLowerCase())) {
                        throw new RuntimeException("Unsupported");
                    }
                }

                @Override
                public void leaveGroup(GroupModel group) {
                    UserMapStorage.this.leaveGroup(realm, getUsername(), group);
                }

                @Override
                public void joinGroup(GroupModel group) {
                    UserMapStorage.this.joinGroup(realm, getUsername(), group);
                }

                @Override
                public String getFederationLink() {
                    return model.getId();
                }
            };
        }

        return user;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (editMode == EditMode.READ_ONLY) {
            throw new ReadOnlyException("Federated storage is not writable");
        }
        if (!(input instanceof UserCredentialModel)) {
            return false;
        }
        if (input.getType().equals(PasswordCredentialModel.TYPE)) {
            userPasswords.put(translateUserName(user.getUsername()), input.getChallengeResponse());
            return true;

        } else {
            return false;
        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        // Test "instanceof PasswordUserCredentialModel" on purpose. We want to test that the backwards compatibility
        if (!(input instanceof PasswordUserCredentialModel)) {
            return false;
        }
        if (input.getType().equals(PasswordCredentialModel.TYPE)) {
            String pw = userPasswords.get(translateUserName(user.getUsername()));

            // Using "getValue" on purpose here, to test that backwards compatibility works as expected
            return pw != null && pw.equals(((UserCredentialModel) input).getValue());
        } else {
            return false;
        }
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        if (!userPasswords.containsKey(translateUserName(username))) {
            return null;
        }

        return createUser(realm, username);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        if (editMode == EditMode.READ_ONLY) {
            throw new ReadOnlyException("Federated storage is not writable");
        }

        userPasswords.put(translateUserName(username), "");
        return createUser(realm, username);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        if (editMode == EditMode.READ_ONLY || editMode == EditMode.UNSYNCED) {
            log.warnf("User '%s' can't be deleted in LDAP as editMode is '%s'. Deleting user just from Keycloak DB, but he will be re-imported from LDAP again once searched in Keycloak", user.getUsername(), editMode.toString());
            userPasswords.remove(translateUserName(user.getUsername()));
            return true;
        }

        return userPasswords.remove(translateUserName(user.getUsername())) != null;
    }

    public boolean removeUserByName(String userName) {
        if (editMode == EditMode.READ_ONLY || editMode == EditMode.UNSYNCED) {
            log.warnf("User '%s' can't be deleted in LDAP as editMode is '%s'. Deleting user just from Keycloak DB, but he will be re-imported from LDAP again once searched in Keycloak", userName, editMode.toString());
            userPasswords.remove(translateUserName(userName));
            return true;
        }

        return userPasswords.remove(translateUserName(userName)) != null;
    }

    public boolean isImportEnabled() {
        if (importEnabled == null) {
            String val = model.getConfig().getFirst(IMPORT_ENABLED);
            if (val == null) {
                importEnabled = true;
            } else {
                importEnabled = Boolean.valueOf(val);
            }
        }
        return importEnabled;

    }

    public void setImportEnabled(boolean flag) {
        importEnabled = flag;
        model.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(flag));
    }

    @Override
    public void preRemove(RealmModel realm) {
        log.infof("preRemove: realm=%s", realm.getName());
        realmRemovals.incrementAndGet();
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        log.infof("preRemove: realm=%s, group=%s", realm.getName(), group.getName());
        groupRemovals.incrementAndGet();
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        log.infof("preRemove: realm=%s, role=%s", realm.getName(), role.getName());
        roleRemovals.incrementAndGet();
    }

    @Override
    public void close() {
        closings.incrementAndGet();
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return userPasswords.size();
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm) {
        return userPasswords.keySet().stream()
          .map(userName -> createUser(realm, userName));
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        Stream<String> userStream = userPasswords.keySet().stream().sorted();

        return paginatedStream(userStream, firstResult, maxResults).map(userName -> createUser(realm, userName));
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search) {
        String tSearch = translateUserName(search);
        return userPasswords.keySet().stream()
          .sorted()
          .filter(userName -> translateUserName(userName).contains(tSearch))
          .map(userName -> createUser(realm, userName));
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        String tSearch = translateUserName(search);
        Stream<String> userStream = userPasswords.keySet().stream()
                .sorted()
                .filter(userName -> translateUserName(userName).contains(search));

        return paginatedStream(userStream, firstResult, maxResults).map(userName -> createUser(realm, userName));
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        Stream<String> userStream = userPasswords.keySet().stream()
          .sorted();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null) {
                continue;
            }

            switch (key) {
                case UserModel.USERNAME:
                case UserModel.SEARCH:
                    if (Boolean.valueOf(params.getOrDefault(UserModel.EXACT, Boolean.FALSE.toString()))) {
                        userStream = userStream.filter(s -> s.toLowerCase().equals(value.toLowerCase()));           
                    } else {
                        userStream = userStream.filter(s -> s.toLowerCase().contains(value.toLowerCase()));
                    }
            }
        }

        return paginatedStream(userStream, firstResult, maxResults).map(userName -> createUser(realm, userName));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return getMembershipStream(realm, group, firstResult == null ? -1 : firstResult, maxResults == null ? -1 : maxResults)
          .map(userName -> createUser(realm, userName));
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        if (isImportEnabled()) {
            return UserStoragePrivateUtil.userLocalStorage(session).searchForUserByUserAttributeStream(realm, attrName, attrValue);
        } else {
            return UserStorageUtil.userFederatedStorage(session).getUsersByUserAttributeStream(realm, attrName, attrValue)
              .map(userName -> createUser(realm, userName));
        }
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, String userId) {
        Set<String> set = userGroups.get(getUserIdInMap(realm, userId));
        if (set == null) {
            return Stream.empty();
        }
        return set.stream().map(realm::getGroupById);
    }

    @Override
    public void joinGroup(RealmModel realm, String userId, GroupModel group) {
        Set<String> groups = userGroups.computeIfAbsent(getUserIdInMap(realm, userId), userName -> new ConcurrentSkipListSet());
        groups.add(group.getId());
    }

    @Override
    public void leaveGroup(RealmModel realm, String userId, GroupModel group) {
        Set<String> set = userGroups.get(getUserIdInMap(realm, userId));
        if (set != null) {
            set.remove(group.getId());
        }
    }

    @Override
    public Stream<String> getMembershipStream(RealmModel realm, GroupModel group, Integer firstResult, Integer max) {
        Stream<String> userStream = paginatedStream(userGroups.entrySet().stream(), firstResult, max)
          .filter(me -> me.getValue().contains(group.getId()))
          .map(Map.Entry::getKey)
          .filter(realmUser -> realmUser.startsWith(realm.getId()))
          .map(realmUser -> realmUser.substring(realmUser.indexOf("/") + 1));

        return userStream;
    }

    @Override
    public UserModel validate(RealmModel realm, UserModel local) {
        final boolean userExists = userPasswords.containsKey(translateUserName(local.getUsername()));
        if (! userExists) {
            userGroups.remove(getUserIdInMap(realm, local.getUsername()));
        }
        return userExists ? local : null;
    }

    private static String translateUserName(String userName) {
        return userName == null ? null : userName.toLowerCase();
    }

}
