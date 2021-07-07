/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.user;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;


public abstract class MapUserAdapter<K> extends AbstractUserModel<MapUserEntity<K>> {
    public MapUserAdapter(KeycloakSession session, RealmModel realm, MapUserEntity<K> entity) {
        super(session, realm, entity);
    }

    @Override
    public String getUsername() {
        return entity.getUsername();
    }

    @Override
    public void setUsername(String username) {
        username = KeycloakModelUtils.toLowerCaseSafe(username);
        // Do not continue if current username of entity is the requested username
        if (username != null && username.equals(entity.getUsername())) return;

        if (checkUsernameUniqueness(realm, username)) {
            throw new ModelDuplicateException("A user with username " + username + " already exists");
        }
        
        entity.setUsername(username);
    }

    @Override
    public Long getCreatedTimestamp() {
        return entity.getCreatedTimestamp();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        entity.setCreatedTimestamp(timestamp);
    }

    @Override
    public boolean isEnabled() {
        return entity.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
    }

    private Optional<String> getSpecialAttributeValue(String name) {
        if (UserModel.FIRST_NAME.equals(name)) {
            return Optional.ofNullable(entity.getFirstName());
        } else if (UserModel.LAST_NAME.equals(name)) {
            return Optional.ofNullable(entity.getLastName());
        } else if (UserModel.EMAIL.equals(name)) {
            return Optional.ofNullable(entity.getEmail());
        } else if (UserModel.USERNAME.equals(name)) {
            return Optional.ofNullable(entity.getUsername());
        }

        return Optional.empty();
    }

    private boolean setSpecialAttributeValue(String name, String value) {
        if (UserModel.FIRST_NAME.equals(name)) {
            entity.setFirstName(value);
            return true;
        } else if (UserModel.LAST_NAME.equals(name)) {
            entity.setLastName(value);
            return true;
        } else if (UserModel.EMAIL.equals(name)) {
            setEmail(value);
            return true;
        } else if (UserModel.USERNAME.equals(name)) {
            setUsername(value);
            return true;
        }

        return false;
    }
    
    @Override
    public void setSingleAttribute(String name, String value) {
        if (setSpecialAttributeValue(name, value)) return;
        if (value == null) {
            entity.removeAttribute(name);
            return;
        }
        entity.setAttribute(name, Collections.singletonList(value));
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        String valueToSet = (values != null && values.size() > 0) ? values.get(0) : null;
        if (setSpecialAttributeValue(name, valueToSet)) return;

        entity.removeAttribute(name);
        if (valueToSet == null) {
            return;
        }

        entity.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        entity.removeAttribute(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        return getSpecialAttributeValue(name)
                .orElseGet(() -> entity.getAttribute(name).stream().findFirst()
                .orElse(null));
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return getSpecialAttributeValue(name).map(Collections::singletonList)
                .orElseGet(() -> entity.getAttribute(name)).stream();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> result = new MultivaluedHashMap<>(entity.getAttributes());
        result.add(UserModel.FIRST_NAME, entity.getFirstName());
        result.add(UserModel.LAST_NAME, entity.getLastName());
        result.add(UserModel.EMAIL, entity.getEmail());
        result.add(UserModel.USERNAME, entity.getUsername());

        return result;
    }

    @Override
    public Stream<String> getRequiredActionsStream() {
        return entity.getRequiredActions().stream();
    }

    @Override
    public void addRequiredAction(String action) {
        entity.addRequiredAction(action);
    }

    @Override
    public void removeRequiredAction(String action) {
        entity.removeRequiredAction(action);
    }

    @Override
    public String getFirstName() {
        return entity.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        entity.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        return entity.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        entity.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        return entity.getEmail();
    }

    @Override
    public void setEmail(String email) {
        email = KeycloakModelUtils.toLowerCaseSafe(email);
        if (email != null) {
            if (email.equals(entity.getEmail())) {
                return;
            }
            if (ObjectUtil.isBlank(email)) {
                email = null;
            }
        }
        boolean duplicatesAllowed = realm.isDuplicateEmailsAllowed();

        if (!duplicatesAllowed && email != null && checkEmailUniqueness(realm, email)) {
            throw new ModelDuplicateException("A user with email " + email + " already exists");
        }

        entity.setEmail(email, duplicatesAllowed);
    }
    
    public abstract boolean checkEmailUniqueness(RealmModel realm, String email);
    public abstract boolean checkUsernameUniqueness(RealmModel realm, String username);

    @Override
    public boolean isEmailVerified() {
        return entity.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        entity.setEmailVerified(verified);
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return session.groups().getGroupsStream(realm, entity.getGroupsMembership().stream());
    }

    @Override
    public void joinGroup(GroupModel group) {
        entity.addGroupsMembership(group.getId());
    }

    @Override
    public void leaveGroup(GroupModel group) {
        entity.removeGroupsMembership(group.getId());
    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        return entity.getGroupsMembership().contains(group.getId());
    }

    @Override
    public String getFederationLink() {
        return entity.getFederationLink();
    }

    @Override
    public void setFederationLink(String link) {
        entity.setFederationLink(link);
    }

    @Override
    public String getServiceAccountClientLink() {
        return entity.getServiceAccountClientLink();
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        entity.setServiceAccountClientLink(clientInternalId);
    }


    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        return getRoleMappingsStream().filter(RoleUtils::isRealmRole);
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        return getRoleMappingsStream().filter(r -> RoleUtils.isClientRole(r, app));
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return entity.getRolesMembership().contains(role.getId());
    }

    @Override
    public void grantRole(RoleModel role) {
        entity.addRolesMembership(role.getId());
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        return entity.getRolesMembership().stream().map(realm::getRoleById);
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        entity.removeRolesMembership(role.getId());
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }
}
