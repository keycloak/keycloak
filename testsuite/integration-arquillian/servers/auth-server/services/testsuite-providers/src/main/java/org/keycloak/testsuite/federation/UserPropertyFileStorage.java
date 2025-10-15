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

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.UserCredentialManager;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserPropertyFileStorage implements UserLookupProvider, UserStorageProvider, UserQueryProvider, CredentialInputValidator {

    public static final String SEARCH_METHOD = "searchForUserStream(RealmMode, Map, Integer, Integer)";
    public static final String COUNT_SEARCH_METHOD = "getUsersCount(RealmModel, Map)";

    protected Properties userPasswords;
    protected ComponentModel model;
    protected KeycloakSession session;
    protected boolean federatedStorageEnabled;

    public static Map<String, List<UserPropertyFileStorageCall>> storageCalls = new HashMap<>();

    public static class UserPropertyFileStorageCall implements Serializable {
        private final String method;
        private final Integer first;
        private final Integer max;

        public UserPropertyFileStorageCall(String method, Integer first, Integer max) {
            this.method = method;
            this.first = first;
            this.max = max;
        }

        public String getMethod() {
            return method;
        }

        public Integer getFirst() {
            return first;
        }

        public Integer getMax() {
            return max;
        }
    }

    public UserPropertyFileStorage(KeycloakSession session, ComponentModel model, Properties userPasswords) {
        this.session = session;
        this.model = model;
        this.userPasswords = userPasswords;
        this.federatedStorageEnabled = model.getConfig().containsKey("federatedStorage") && Boolean.valueOf(model.getConfig().getFirst("federatedStorage")).booleanValue();
    }

    private void addCall(String method, Integer first, Integer max) {
        storageCalls.merge(model.getId(), new LinkedList<>(Collections.singletonList(new UserPropertyFileStorageCall(method, first, max))), (a, b) -> {
            a.addAll(b);
            return a;
        });
    }

    private void addCall(String method) {
        addCall(method, null, null);
    }

    @Override
    public int getUsersCount(RealmModel realm, Map<String, String> params) {
        addCall(COUNT_SEARCH_METHOD);

        String search = params.get(UserModel.SEARCH);
        return (int) searchForUser(realm, search, null, null, username -> search == null || username.contains(search), null).count();
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();
        if ("uppercase".equalsIgnoreCase(username)) {
            username = username.toLowerCase();
        }
        if (!userPasswords.containsKey(username)) return null;

        return createUser(realm, username);
    }

    private UserModel createUser(RealmModel realm, String username) {
        if (federatedStorageEnabled) {
            return new AbstractUserAdapterFederatedStorage.Streams(session, realm,  model) {
                @Override
                public String getUsername() {
                    if ("uppercase".equalsIgnoreCase(username)) {
                        return username.toUpperCase();
                    }
                    return username;
                }

                @Override
                public void setUsername(String username) {
                    throw new RuntimeException("Unsupported");
                }
            };
        } else {
            return new AbstractUserAdapter.Streams(session, realm, model) {
                @Override
                public String getUsername() {
                    if ("uppercase".equalsIgnoreCase(username)) {
                        return username.toUpperCase();
                    }
                    return username;
                }

                @Override
                public SubjectCredentialManager credentialManager() {
                    return new UserCredentialManager(session, realm, this);
                }
            };
        }
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        if (!userPasswords.containsKey(username)) return null;

        return createUser(realm, username);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE) && userPasswords.get(user.getUsername()) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) return false;
        if (input.getType().equals(PasswordCredentialModel.TYPE)) {
            String username = user.getUsername();
            if ("uppercase".equalsIgnoreCase(username)) {
                username = user.getUsername().toLowerCase();
            }
            String pw = (String)userPasswords.get(username);
            return pw != null && pw.equals(input.getChallengeResponse());
        } else {
            return false;
        }
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return userPasswords.size();
    }

    @Override
    public int getUsersCount(RealmModel realm, Set<String> groupIds) {
        return 0;
    }

//    @Override
//    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
//        addCall(SEARCH_METHOD, firstResult, maxResults);
//        return searchForUser(realm, search, firstResult, maxResults, username -> username.contains(search));
//    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults, String sortBy) {
        addCall(SEARCH_METHOD, firstResult, maxResults);
        String search = Optional.ofNullable(attributes.get(UserModel.USERNAME))
                .orElseGet(()-> attributes.get(UserModel.SEARCH));
        Predicate<String> p;
        if (search == null) {
            p = x -> true;
        } else {
            p = Boolean.parseBoolean(attributes.getOrDefault(UserModel.EXACT, Boolean.FALSE.toString()))
                    ? username -> username.equals(search)
                    : username -> username.contains(search);
        }
        return searchForUser(realm, search, firstResult, maxResults, p, sortBy);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty();
    }

    @Override
    public void close() {

    }

    private Stream<UserModel> searchForUser(RealmModel realm, String search, Integer firstResult, Integer maxResults, Predicate<String> matcher, String sortBy) {
        if (maxResults != null && maxResults == 0) return Stream.empty();
        return paginatedStream(userPasswords.keySet().stream(), firstResult, maxResults)
                .map(String.class::cast)
                .filter(matcher)
                .map(username -> createUser(realm, username))
                .sorted((u1, u2) -> {
                    if (sortBy == null) {
                        return 0;
                    }
                    String p1 = u1.getFirstAttribute(sortBy);
                    String p2 = u2.getFirstAttribute(sortBy);
                    return p1.compareTo(p2);
                });
    }
}
