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

package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.infinispan.DefaultLazyLoader;
import org.keycloak.models.cache.infinispan.LazyLoader;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedUser extends AbstractExtendableRevisioned implements InRealm  {

    private final String realm;
    private final Long createdTimestamp;
    private final boolean emailVerified;
    private final boolean enabled;
    private final String federationLink;
    private final String serviceAccountClientLink;
    private final int notBefore;
    private final LazyLoader<UserModel, Set<String>> requiredActions;
    private final LazyLoader<UserModel, MultivaluedHashMap<String, String>> lazyLoadedAttributes;
    private final MultivaluedHashMap<String,String> eagerLoadedAttributes;
    private final LazyLoader<UserModel, Set<String>> roleMappings;
    private final LazyLoader<UserModel, Set<String>> groups;
    private final LazyLoader<UserModel, List<CredentialModel>> storedCredentials;

    public CachedUser(Long revision, RealmModel realm, UserModel user, int notBefore) {
        super(revision, user.getId());
        this.realm = realm.getId();
        this.createdTimestamp = user.getCreatedTimestamp();
        this.emailVerified = user.isEmailVerified();
        this.enabled = user.isEnabled();
        this.federationLink = user.getFederationLink();
        this.serviceAccountClientLink = user.getServiceAccountClientLink();
        this.notBefore = notBefore;
        this.eagerLoadedAttributes = new MultivaluedHashMap<>();
        this.eagerLoadedAttributes.putSingle(UserModel.USERNAME,user.getUsername());
        this.eagerLoadedAttributes.putSingle(UserModel.FIRST_NAME,user.getFirstName());
        this.eagerLoadedAttributes.putSingle(UserModel.LAST_NAME,user.getLastName());
        this.eagerLoadedAttributes.putSingle(UserModel.EMAIL,user.getEmail());
        this.lazyLoadedAttributes = new DefaultLazyLoader<>(userModel -> new MultivaluedHashMap<>(userModel.getAttributes()), MultivaluedHashMap::new);
        this.requiredActions = new DefaultLazyLoader<>(userModel -> userModel.getRequiredActionsStream().collect(Collectors.toSet()), Collections::emptySet);
        this.roleMappings = new DefaultLazyLoader<>(userModel -> userModel.getRoleMappingsStream().map(RoleModel::getId).collect(Collectors.toSet()), Collections::emptySet);
        this.groups = new DefaultLazyLoader<>(userModel -> userModel.getGroupsStream().map(GroupModel::getId).collect(Collectors.toCollection(LinkedHashSet::new)), LinkedHashSet::new);
        this.storedCredentials = new DefaultLazyLoader<>(userModel -> userModel.credentialManager().getStoredCredentialsStream().collect(Collectors.toCollection(LinkedList::new)), LinkedList::new);
    }

    public String getRealm() {
        return realm;
    }

    public String getUsername() {
        return eagerLoadedAttributes.getFirst(UserModel.USERNAME);
    }

    public String getFirstAttribute(String name, Supplier<UserModel> userModel) {
        if(eagerLoadedAttributes.containsKey(name))
            return eagerLoadedAttributes.getFirst(name);
        else
            return this.lazyLoadedAttributes.get(userModel).getFirst(name);
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public String getEmail() {
        return eagerLoadedAttributes.getFirst(UserModel.EMAIL);
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public MultivaluedHashMap<String, String> getAttributes(Supplier<UserModel> userModel) {
        return lazyLoadedAttributes.get(userModel);
    }

    public Set<String> getRequiredActions(Supplier<UserModel> userModel) {
        return this.requiredActions.get(userModel);
    }

    public Set<String> getRoleMappings(Supplier<UserModel> userModel) {
        return roleMappings.get(userModel);
    }

    public String getFederationLink() {
        return federationLink;
    }

    public String getServiceAccountClientLink() {
        return serviceAccountClientLink;
    }

    public Set<String> getGroups(Supplier<UserModel> userModel) {
        return groups.get(userModel);
    }

    public int getNotBefore() {
        return notBefore;
    }

    public List<CredentialModel> getStoredCredentials(Supplier<UserModel> userModel) {
        // clone the credential model before returning it, so that modifications don't pollute the cache
        return storedCredentials.get(userModel).stream().map(CredentialModel::shallowClone).collect(Collectors.toList());
    }

}
