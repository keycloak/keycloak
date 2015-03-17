/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.models.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import org.keycloak.models.file.adapter.UserAdapter;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.keycloak.connections.file.FileConnectionProvider;
import org.keycloak.connections.file.InMemoryModel;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.entities.FederatedIdentityEntity;
import org.keycloak.models.entities.UserEntity;
import org.keycloak.models.utils.CredentialValidation;

/**
 * UserProvider for JSON persistence.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class FileUserProvider implements UserProvider {

    private final KeycloakSession session;
    private FileConnectionProvider fcProvider;
    private final InMemoryModel inMemoryModel;

    public FileUserProvider(KeycloakSession session, FileConnectionProvider fcProvider) {
        this.session = session;
        this.fcProvider = fcProvider;
        session.enlistForClose(this);
        this.inMemoryModel = fcProvider.getModel();
    }

    @Override
    public void close() {
        fcProvider.sessionClosed(session);
    }

    @Override
    public UserModel getUserById(String userId, RealmModel realm) {
        return inMemoryModel.getUser(realm.getId(), userId);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        for (UserModel user : inMemoryModel.getUsers(realm.getId())) {
            if (user.getUsername() == null) continue;
            if (user.getUsername().equals(username.toLowerCase())) return user;
        }

        return null;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        for (UserModel user : inMemoryModel.getUsers(realm.getId())) {
            if (user.getEmail() == null) continue;
            if (user.getEmail().equals(email.toLowerCase())) return user;
        }

        return null;
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        for (UserModel user : inMemoryModel.getUsers(realm.getId())) {
            Set<FederatedIdentityModel> identities = this.getFederatedIdentities(user, realm);
            for (FederatedIdentityModel idModel : identities) {
                if (idModel.getUserId().equals(socialLink.getUserId())) return user;
            }
        }

        return null;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, -1, -1);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return inMemoryModel.getUsers(realm.getId()).size();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        List users = new ArrayList(inMemoryModel.getUsers(realm.getId()));
        List<UserModel> sortedList = sortedSubList(users, firstResult, maxResults);
        return sortedList;
    }

    protected List<UserModel> sortedSubList(List list, int firstResult, int maxResults) {
        if (list.isEmpty()) return list;

        Collections.sort(list);
        int first = (firstResult <= 0) ? 0 : firstResult;
        int last = first + maxResults; // could be int overflow
        if ((maxResults > list.size() - first) || (last > list.size())) { // int overflow or regular overflow
            last = list.size();
        }

        if (maxResults <= 0) {
            last = list.size();
        }

        return list.subList(first, last);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        search = search.trim();
        Pattern caseInsensitivePattern = Pattern.compile("(?i:.*" + search + ".*)", Pattern.CASE_INSENSITIVE);

        int spaceInd = search.lastIndexOf(" ");
        boolean isFirstAndLastSearch = spaceInd != -1;
        Pattern firstNamePattern = null;
        Pattern lastNamePattern = null;
        if (isFirstAndLastSearch) {
            String firstNamePatternString = search.substring(0, spaceInd);
            String lastNamePatternString = search.substring(spaceInd + 1);
            firstNamePattern = Pattern.compile("(?i:.*" + firstNamePatternString + ".*$)", Pattern.CASE_INSENSITIVE);
            lastNamePattern = Pattern.compile("(?i:^.*" + lastNamePatternString + ".*)", Pattern.CASE_INSENSITIVE);
        }

        List<UserModel> found = new ArrayList<UserModel>();

        for (UserModel user : inMemoryModel.getUsers(realm.getId())) {
            String firstName = user.getFirstName();
            String lastName = user.getLastName();
            // Case when we have search string like "ohn Bow". Then firstName must end with "ohn" AND lastName must start with "bow" (everything case-insensitive)
            if (isFirstAndLastSearch) {
                if (isAMatch(firstNamePattern, firstName) &&
                    isAMatch(lastNamePattern, lastName)) {
                    found.add(user);
                    continue;
                }
            }

            if (isAMatch(caseInsensitivePattern, firstName) ||
                isAMatch(caseInsensitivePattern, lastName) ||
                isAMatch(caseInsensitivePattern, user.getUsername()) ||
                isAMatch(caseInsensitivePattern, user.getEmail())) {
                found.add(user);
            }
        }

        return sortedSubList(found, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return searchForUserByAttributes(attributes, realm, -1, -1);
    }

    protected boolean isAMatch(Pattern pattern, String value) {
        return (value != null) && (pattern != null) && pattern.matcher(value).matches();
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        Pattern usernamePattern = null;
        Pattern firstNamePattern = null;
        Pattern lastNamePattern = null;
        Pattern emailPattern = null;
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(UserModel.USERNAME)) {
                usernamePattern = Pattern.compile(".*" + entry.getValue() + ".*", Pattern.CASE_INSENSITIVE);
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                firstNamePattern = Pattern.compile(".*" + entry.getValue() + ".*", Pattern.CASE_INSENSITIVE);
            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                lastNamePattern = Pattern.compile(".*" + entry.getValue() + ".*", Pattern.CASE_INSENSITIVE);
            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                emailPattern = Pattern.compile(".*" + entry.getValue() + ".*", Pattern.CASE_INSENSITIVE);
            }
        }

        List<UserModel> found = new ArrayList<UserModel>();
        for (UserModel user : inMemoryModel.getUsers(realm.getId())) {
            if (isAMatch(usernamePattern, user.getUsername()) ||
                isAMatch(firstNamePattern, user.getFirstName()) ||
                isAMatch(lastNamePattern, user.getLastName()) ||
                isAMatch(emailPattern, user.getEmail())) {
                found.add(user);
            }
        }

        return sortedSubList(found, firstResult, maxResults);
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel userModel, RealmModel realm) {
        UserEntity userEntity = ((UserAdapter)getUserById(userModel.getId(), realm)).getUserEntity();
        List<FederatedIdentityEntity> linkEntities = userEntity.getFederatedIdentities();

        if (linkEntities == null) {
            return Collections.EMPTY_SET;
        }

        Set<FederatedIdentityModel> result = new HashSet<FederatedIdentityModel>();
        for (FederatedIdentityEntity federatedIdentityEntity : linkEntities) {
            FederatedIdentityModel model = new FederatedIdentityModel(federatedIdentityEntity.getIdentityProvider(),
                    federatedIdentityEntity.getUserId(), federatedIdentityEntity.getUserName());
            result.add(model);
        }
        return result;
    }

    private FederatedIdentityEntity findSocialLink(UserModel userModel, String socialProvider, RealmModel realm) {
        UserModel user = getUserById(userModel.getId(), realm);
        UserEntity userEntity = ((UserAdapter)getUserById(userModel.getId(), realm)).getUserEntity();
        List<FederatedIdentityEntity> linkEntities = userEntity.getFederatedIdentities();
        if (linkEntities == null) {
            return null;
        }

        for (FederatedIdentityEntity federatedIdentityEntity : linkEntities) {
            if (federatedIdentityEntity.getIdentityProvider().equals(socialProvider)) {
                return federatedIdentityEntity;
            }
        }
        return null;
    }


    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        FederatedIdentityEntity federatedIdentityEntity = findSocialLink(user, socialProvider, realm);
        return federatedIdentityEntity != null ? new FederatedIdentityModel(federatedIdentityEntity.getIdentityProvider(), federatedIdentityEntity.getUserId(), federatedIdentityEntity.getUserName()) : null;
    }

    @Override
    public UserAdapter addUser(RealmModel realm, String id, String username, boolean addDefaultRoles) {
        if (inMemoryModel.hasUserWithUsername(realm.getId(), username.toLowerCase()))
            throw new ModelDuplicateException("User with username " + username + " already exists in realm.");

        UserAdapter userModel = addUserEntity(realm, id, username.toLowerCase());

        if (addDefaultRoles) {
            for (String r : realm.getDefaultRoles()) {
                userModel.grantRole(realm.getRole(r));
            }

            for (ApplicationModel application : realm.getApplications()) {
                for (String r : application.getDefaultRoles()) {
                    userModel.grantRole(application.getRole(r));
                }
            }
        }

        return userModel;
    }

    protected UserAdapter addUserEntity(RealmModel realm, String userId, String username) {
        if (realm == null) throw new NullPointerException("realm == null");
        if (username == null) throw new NullPointerException("username == null");

        if (userId == null) userId = KeycloakModelUtils.generateId();

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setUsername(username);
        // Compatibility with JPA model, which has user disabled by default
        // userEntity.setEnabled(true);
        userEntity.setRealmId(realm.getId());

        UserAdapter user = new UserAdapter(realm, userEntity, inMemoryModel);
        inMemoryModel.putUser(realm.getId(), userId, user);

        return user;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return inMemoryModel.removeUser(realm.getId(), user.getId());
    }


    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        UserAdapter userAdapter = (UserAdapter)getUserById(user.getId(), realm);
        UserEntity userEntity = userAdapter.getUserEntity();
        FederatedIdentityEntity federatedIdentityEntity = new FederatedIdentityEntity();
        federatedIdentityEntity.setIdentityProvider(socialLink.getIdentityProvider());
        federatedIdentityEntity.setUserId(socialLink.getUserId());
        federatedIdentityEntity.setUserName(socialLink.getUserName().toLowerCase());

        //check if it already exitsts - do I need to do this?
        for (FederatedIdentityEntity fedIdent : userEntity.getFederatedIdentities()) {
            if (fedIdent.equals(federatedIdentityEntity)) return;
        }

        userEntity.getFederatedIdentities().add(federatedIdentityEntity);
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel userModel, String socialProvider) {
        UserModel user = getUserById(userModel.getId(), realm);
        UserEntity userEntity = ((UserAdapter) user).getUserEntity();
        FederatedIdentityEntity federatedIdentityEntity = findSocialLink(userEntity, socialProvider);
        if (federatedIdentityEntity == null) {
            return false;
        }

        userEntity.getFederatedIdentities().remove(federatedIdentityEntity);
        return true;
    }

    private FederatedIdentityEntity findSocialLink(UserEntity userEntity, String socialProvider) {
        List<FederatedIdentityEntity> linkEntities = userEntity.getFederatedIdentities();
        if (linkEntities == null) {
            return null;
        }

        for (FederatedIdentityEntity federatedIdentityEntity : linkEntities) {
            if (federatedIdentityEntity.getIdentityProvider().equals(socialProvider)) {
                return federatedIdentityEntity;
            }
        }
        return null;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return this.addUser(realm, KeycloakModelUtils.generateId(), username.toLowerCase(), true);
    }

    @Override
    public void preRemove(RealmModel realm) {
        // Nothing to do here?  Federation links are attached to users, which are removed by InMemoryModel
    }

    @Override
    public void preRemove(RealmModel realm, UserFederationProviderModel link) {
        Set<UserModel> toBeRemoved = new HashSet<UserModel>();
        for (UserModel user : inMemoryModel.getUsers(realm.getId())) {
            String fedLink = user.getFederationLink();
            if (fedLink == null) continue;
            if (fedLink.equals(link.getId())) toBeRemoved.add(user);
        }

        for (UserModel user : toBeRemoved) {
            inMemoryModel.removeUser(realm.getId(), user.getId());
        }
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        // todo not sure what to do for this
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        return CredentialValidation.validCredentials(realm, user, input);
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
        return CredentialValidation.validCredentials(realm, user, input);
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        federatedUser = getUserById(federatedUser.getId(), realm);
        UserEntity userEntity = ((UserAdapter) federatedUser).getUserEntity();
        FederatedIdentityEntity federatedIdentityEntity = findFederatedIdentityLink(userEntity, federatedIdentityModel.getIdentityProvider());

        federatedIdentityEntity.setToken(federatedIdentityModel.getToken());
    }

    private FederatedIdentityEntity findFederatedIdentityLink(UserEntity userEntity, String identityProvider) {
        List<FederatedIdentityEntity> linkEntities = userEntity.getFederatedIdentities();
        if (linkEntities == null) {
            return null;
        }

        for (FederatedIdentityEntity federatedIdentityEntity : linkEntities) {
            if (federatedIdentityEntity.getIdentityProvider().equals(identityProvider)) {
                return federatedIdentityEntity;
            }
        }
        return null;
    }

    @Override
    public CredentialValidationOutput validCredentials(RealmModel realm, UserCredentialModel... input) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return null; // not supported yet
    }

}
