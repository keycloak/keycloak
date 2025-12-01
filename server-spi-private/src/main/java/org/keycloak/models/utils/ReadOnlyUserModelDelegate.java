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

import java.util.List;
import java.util.function.Function;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ReadOnlyException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ReadOnlyUserModelDelegate extends UserModelDelegate {

    private final Function<String, RuntimeException> exceptionCreator;
    private Boolean enabled;

    public ReadOnlyUserModelDelegate(UserModel delegate) {
        this(delegate, ReadOnlyException::new);
    }

    public ReadOnlyUserModelDelegate(UserModel delegate, boolean enabled) {
        this(delegate, ReadOnlyException::new);
        this.enabled = enabled;
    }

    public ReadOnlyUserModelDelegate(UserModel delegate, Function<String, RuntimeException> exceptionCreator) {
        super(delegate);
        this.exceptionCreator = exceptionCreator;
    }

    public ReadOnlyUserModelDelegate(UserModel delegate, boolean enabled, Function<String, RuntimeException> exceptionCreator) {
        this(delegate, exceptionCreator);
        this.enabled = enabled;
    }

    @Override
    public void setUsername(String username) {
        throw readOnlyException("username");
    }

    @Override
    public void setEnabled(boolean enabled) {
        throw readOnlyException("enabled");
    }

    @Override
    public boolean isEnabled() {
        if (enabled == null) {
            return super.isEnabled();
        }
        return enabled;
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        throw readOnlyException("attribute(" + name + ")");
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        throw readOnlyException("attribute(" + name + ")");
    }

    @Override
    public void removeAttribute(String name) {
        throw readOnlyException("attribute(" + name + ")");
    }

    @Override
    public void addRequiredAction(String action) {
        throw readOnlyException("required action " + action);
    }

    @Override
    public void removeRequiredAction(String action) {
        throw readOnlyException("required action " + action);
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        throw readOnlyException("required action " + action);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        throw readOnlyException("required action " + action);
    }

    @Override
    public void setFirstName(String firstName) {
        throw readOnlyException("firstName");
    }

    @Override
    public void setLastName(String lastName) {
        throw readOnlyException("lastName");
    }

    @Override
    public void setEmail(String email) {
        throw readOnlyException("email");
    }

    @Override
    public void setEmailVerified(boolean verified) {
        throw readOnlyException("emailVerified");
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        throw readOnlyException("role mapping for role " + role.getName());
    }

    @Override
    public void setFederationLink(String link) {
        throw readOnlyException("federationLink");
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        throw readOnlyException("serviceAccountClientLink");
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        throw readOnlyException("createdTimestamp");
    }

    @Override
    public void joinGroup(GroupModel group) {
        throw readOnlyException("group mapping for group " + group.getName());
    }

    @Override
    public void leaveGroup(GroupModel group) {
        throw readOnlyException("group mapping for group " + group.getName());
    }

    @Override
    public void grantRole(RoleModel role) {
        throw readOnlyException("role mapping for role " + role.getName());
    }

    private RuntimeException readOnlyException(String detail) {
        String message = String.format("The user is read-only. Not possible to write '%s' when updating user '%s'.", detail, getUsername());
        return exceptionCreator.apply(message);
    }
}
