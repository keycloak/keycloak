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

package org.keycloak.models.cache.infinispan;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.util.CollectionUtil;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.Type;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.infinispan.entities.CachedUser;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAdapter implements CachedUserModel {

    private final Supplier<UserModel> modelSupplier;
    protected final CachedUser cached;
    protected final UserCacheSession userProviderCache;
    protected final KeycloakSession keycloakSession;
    protected final RealmModel realm;
    protected volatile UserModel updated;

    public UserAdapter(CachedUser cached, UserCacheSession userProvider, KeycloakSession keycloakSession, RealmModel realm) {
        this.cached = cached;
        this.userProviderCache = userProvider;
        this.keycloakSession = keycloakSession;
        this.realm = realm;
        this.modelSupplier = new LazyModel<>(this::getUserModel);
    }

    @Override
    public String getFirstName() {
        if (updated != null) return updated.getFirstName();
        return getFirstAttribute(FIRST_NAME);
    }

    @Override
    public void setFirstName(String firstName) {
        setSingleAttribute(FIRST_NAME, firstName);
    }

    @Override
    public String getLastName() {
        if (updated != null) return updated.getLastName();
        return getFirstAttribute(LAST_NAME);
    }

    @Override
    public void setLastName(String lastName) {
        setSingleAttribute(LAST_NAME, lastName);
    }

    @Override
    public String getEmail() {
        if (updated != null) return updated.getEmail();
        return getFirstAttribute(EMAIL);
    }

    @Override
    public void setEmail(String email) {
        email = email == null ? null : email.toLowerCase();
        setSingleAttribute(EMAIL, email);
    }

    @Override
    public UserModel getDelegateForUpdate() {
        if (updated == null) {
            userProviderCache.registerUserInvalidation(cached);
            updated = modelSupplier.get();
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
        return updated;
    }

    @Override
    public boolean isMarkedForEviction() {
        return updated != null;
    }

    @Override
    public void invalidate() {
        try {
            getDelegateForUpdate();
        } catch (IllegalStateException ex) {    // Not found in database, thus "invalidated" in the underlying model by definition
            // ignore
        }
    }

    @Override
    public long getCacheTimestamp() {
        return cached.getCacheTimestamp();
    }

    @Override
    public ConcurrentHashMap getCachedWith() {
        return cached.getCachedWith();
    }

    @Override
    public String getId() {
        if (updated != null) return updated.getId();
        return cached.getId();
    }

    @Override
    public String getUsername() {
        if (updated != null) return updated.getUsername();
        return getFirstAttribute(UserModel.USERNAME);
    }

    @Override
    public void setUsername(String username) {
        username = username==null ? null : username.toLowerCase();
        setSingleAttribute(UserModel.USERNAME, username);
    }

    @Override
    public Long getCreatedTimestamp() {
        // get from cached always as it is immutable
        return cached.getCreatedTimestamp();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        // nothing to do as this value is immutable
    }

    @Override
    public boolean isEnabled() {
        if (updated != null) return updated.isEnabled();
        return cached.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (updated == null && cached.isEnabled() == enabled) {
            return;
        }
        getDelegateForUpdate();
        updated.setEnabled(enabled);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        if (UserModel.USERNAME.equals(name) || UserModel.EMAIL.equals(name)) {
            value = KeycloakModelUtils.toLowerCaseSafe(value);
        }
        if (updated == null) {
            Set<String> oldEntries = getAttributeStream(name).collect(Collectors.toSet());
            Set<String> newEntries = value != null ? Set.of(value) : Collections.emptySet();
            if (CollectionUtil.collectionEquals(oldEntries, newEntries)) {
                return;
            }
        }
        getDelegateForUpdate();
        updated.setSingleAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (UserModel.USERNAME.equals(name) || UserModel.EMAIL.equals(name)) {
            String lowerCasedFirstValue = KeycloakModelUtils.toLowerCaseSafe((values != null && values.size() > 0) ? values.get(0) : null);
            if (lowerCasedFirstValue != null) values = Collections.singletonList(lowerCasedFirstValue);
        }
        if (updated == null) {
            Set<String> oldEntries = getAttributeStream(name).collect(Collectors.toSet());
            Set<String> newEntries;
            if (values == null) {
                newEntries = new HashSet<>();
            } else {
                newEntries = new HashSet<>(values);
            }
            if (CollectionUtil.collectionEquals(oldEntries, newEntries)) {
                return;
            }
        }
        getDelegateForUpdate();
        updated.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        if (updated == null && getFirstAttribute(name) == null) {
            return;
        }
        getDelegateForUpdate();
        updated.removeAttribute(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        if (updated != null) return updated.getFirstAttribute(name);
        return cached.getFirstAttribute(keycloakSession, name, modelSupplier);
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        if (updated != null) return updated.getAttributeStream(name);
        List<String> result = cached.getAttributes(keycloakSession, modelSupplier).get(name);
        return (result == null) ? Stream.empty() : result.stream();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (updated != null) return updated.getAttributes();
        return cached.getAttributes(keycloakSession, modelSupplier);
    }

    @Override
    public Stream<String> getRequiredActionsStream() {
        if (updated != null) return updated.getRequiredActionsStream();
        return cached.getRequiredActions(keycloakSession, modelSupplier).stream();
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        if (action == null || updated == null && getCachedRequiredActions().contains(action.name())) {
            return;
        }
        getDelegateForUpdate();
        updated.addRequiredAction(action);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        if (action == null || updated == null && !getCachedRequiredActions().contains(action.name())) {
            return;
        }
        getDelegateForUpdate();
        updated.removeRequiredAction(action);
    }

    @Override
    public void addRequiredAction(String action) {
        if (updated == null && getCachedRequiredActions().contains(action)) {
            return;
        }
        getDelegateForUpdate();
        updated.addRequiredAction(action);
    }

    @Override
    public void removeRequiredAction(String action) {
        if (updated == null && !getCachedRequiredActions().contains(action)) {
            return;
        }
        getDelegateForUpdate();
        updated.removeRequiredAction(action);
    }

    private Set<String> getCachedRequiredActions() {
        return cached.getRequiredActions(keycloakSession, modelSupplier);
    }

    @Override
    public boolean isEmailVerified() {
        if (updated != null) return updated.isEmailVerified();
        return cached.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        if (updated == null && cached.isEmailVerified() == verified) {
            return;
        }
        getDelegateForUpdate();
        updated.setEmailVerified(verified);
    }

    @Override
    public String getFederationLink() {
        if (updated != null) return updated.getFederationLink();
        return cached.getFederationLink();
    }

    @Override
    public void setFederationLink(String link) {
        if (updated == null && Objects.equals(cached.getFederationLink(), link)) {
            return;
        }
        getDelegateForUpdate();
        updated.setFederationLink(link);
    }

    @Override
    public String getServiceAccountClientLink() {
        if (updated != null) return updated.getServiceAccountClientLink();
        return cached.getServiceAccountClientLink();
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        if (updated == null && Objects.equals(cached.getServiceAccountClientLink(), clientInternalId)) {
            return;
        }
        getDelegateForUpdate();
        updated.setServiceAccountClientLink(clientInternalId);
    }

    @Override
    public SubjectCredentialManager credentialManager() {
        // Instantiate a new LegacyUserCredentialManager that points to the instance that is wrapped by the cache
        // this way it the cache will know if any of the credentials are modified during validation of CredentialInputs.
        // This assumes that each implementation in the legacy world implements the LegacyUserCredentialManager and not something else.
        return new SubjectCredentialManagerCacheAdapter(keycloakSession, realm, this) {
            @Override
            public CredentialModel getStoredCredentialById(String id) {
                if (updated == null) {
                    return cached.getStoredCredentials(keycloakSession, modelSupplier).stream().filter(credential ->
                                    Objects.equals(id, credential.getId()))
                            .findFirst().orElse(null);
                }
                return super.getStoredCredentialById(id);
            }

            @Override
            public Stream<CredentialModel> getStoredCredentialsStream() {
                if (updated == null) {
                    return cached.getStoredCredentials(keycloakSession, modelSupplier).stream();
                }
                return super.getStoredCredentialsStream();
            }

            @Override
            public Stream<CredentialModel> getStoredCredentialsByTypeStream(String type) {
                if (updated == null) {
                    return cached.getStoredCredentials(keycloakSession, modelSupplier).stream().filter(credential -> Objects.equals(type, credential.getType()));
                }
                return super.getStoredCredentialsByTypeStream(type);
            }

            @Override
            public CredentialModel getStoredCredentialByNameAndType(String name, String type) {
                if (updated == null) {
                    return cached.getStoredCredentials(keycloakSession, modelSupplier).stream().filter(credential ->
                            Objects.equals(type, credential.getType()) && Objects.equals(name, credential.getUserLabel()))
                            .findFirst().orElse(null);
                }
                return super.getStoredCredentialByNameAndType(name, type);
            }

            @Override
            public void invalidateCacheForEntity() {
                // This implies invalidation of the cached entry,
                // and all future calls in this session for the user will go to the store instead of the cache.
                getDelegateForUpdate();
            }
        };
    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        if (updated != null) return updated.getRealmRoleMappingsStream();
        return getRoleMappingsStream().filter(r -> RoleUtils.isRealmRole(r, realm));
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        if (updated != null) return updated.getClientRoleMappingsStream(app);
        return getRoleMappingsStream().filter(r -> RoleUtils.isClientRole(r, app));
    }

    @Override
    public boolean hasDirectRole(RoleModel role) {
        if (updated != null) return updated.hasDirectRole(role);
        return cached.getRoleMappings(keycloakSession, modelSupplier).contains(role.getId());
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (updated != null) return updated.hasRole(role);
        return cached.getRoleMappings(keycloakSession, modelSupplier).contains(role.getId()) ||
                getRoleMappingsStream().anyMatch(r -> r.hasRole(role)) ||
                RoleUtils.hasRoleFromGroup(getGroupsStream(), role, true);
    }

    @Override
    public void grantRole(RoleModel role) {
        if (updated == null && cached.getRoleMappings(keycloakSession, modelSupplier).contains(role.getId())) {
            return;
        }
        getDelegateForUpdate();
        updated.grantRole(role);
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        if (updated != null) return updated.getRoleMappingsStream();
        Set<RoleModel> roles = new HashSet<>();
        for (String id : cached.getRoleMappings(keycloakSession, modelSupplier)) {
            RoleModel roleById = keycloakSession.roles().getRoleById(realm, id);
            if (roleById == null) {
                // chance that role was removed, so just delete to persistence and get user invalidated
                getDelegateForUpdate();
                return updated.getRoleMappingsStream();
            }
            roles.add(roleById);

        }
        return roles.stream();
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        if (updated == null && !cached.getRoleMappings(keycloakSession, modelSupplier).contains(role.getId())) {
            return;
        }
        getDelegateForUpdate();
        updated.deleteRoleMapping(role);
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        Stream<GroupModel> result = Stream.empty();

        if (updated != null) {
            result = updated.getGroupsStream();
        } else {
            Set<GroupModel> groups = null;
            for (String id : cached.getGroups(keycloakSession, modelSupplier)) {
                GroupModel groupModel = keycloakSession.groups().getGroupById(realm, id);
                if (groupModel == null) {
                    // chance that role was removed, so just delegate to persistence and get user invalidated
                    getDelegateForUpdate();
                    result = updated.getGroupsStream();
                    break;
                } else {
                    if (groups == null) {
                        groups = new HashSet<>();
                    }
                    groups.add(groupModel);
                }
            }

            if (groups != null) {
                result = groups.stream();
            }
        }

        return result.filter(g -> Type.REALM.equals(g.getType())).sorted(Comparator.comparing(GroupModel::getName));
    }

    @Override
    public long getGroupsCountByNameContaining(String search) {
        if (updated != null) return updated.getGroupsCountByNameContaining(search);
        return modelSupplier.get().getGroupsCountByNameContaining(search);
    }

    @Override
    public void joinGroup(GroupModel group) {
        if (group.getType() == Type.REALM && cached.getGroups(keycloakSession, modelSupplier).contains(group.getId())) {
            return;
        }
        getDelegateForUpdate();
        updated.joinGroup(group);

    }

    @Override
    public void leaveGroup(GroupModel group) {
        if (group.getType() == Type.REALM && updated == null && !cached.getGroups(keycloakSession, modelSupplier).contains(group.getId())) {
            return;
        }
        getDelegateForUpdate();
        updated.leaveGroup(group);
    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        if (updated != null) return updated.isMemberOf(group);
        return cached.getGroups(keycloakSession, modelSupplier).contains(group.getId()) || RoleUtils.isMember(getGroupsStream(), group);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserModel)) return false;

        UserModel that = (UserModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    private UserModel getUserModel() {
        return userProviderCache.getDelegate().getUserById(realm, cached.getId());
    }

}
