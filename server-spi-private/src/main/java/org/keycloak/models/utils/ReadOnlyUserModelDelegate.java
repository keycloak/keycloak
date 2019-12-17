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

import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ReadOnlyException;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ReadOnlyUserModelDelegate extends UserModelDelegate {
    public ReadOnlyUserModelDelegate(UserModel delegate) {
        super(delegate);
    }

    @Override
    public void setUsername(String username) {
        throw new ReadOnlyException();
    }

    @Override
    public void setEnabled(boolean enabled) {
        throw new ReadOnlyException();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        throw new ReadOnlyException();
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        throw new ReadOnlyException();
    }

    @Override
    public void removeAttribute(String name) {
        throw new ReadOnlyException();
    }

    @Override
    public void addRequiredAction(String action) {
        throw new ReadOnlyException();
    }

    @Override
    public void removeRequiredAction(String action) {
        throw new ReadOnlyException();
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        throw new ReadOnlyException();
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        throw new ReadOnlyException();
    }

    @Override
    public void setFirstName(String firstName) {
        throw new ReadOnlyException();
    }

    @Override
    public void setLastName(String lastName) {
        throw new ReadOnlyException();
    }

    @Override
    public void setEmail(String email) {
        throw new ReadOnlyException();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        throw new ReadOnlyException();
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        throw new ReadOnlyException();
    }

    @Override
    public void setFederationLink(String link) {
        throw new ReadOnlyException();
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        throw new ReadOnlyException();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        throw new ReadOnlyException();
    }

    @Override
    public void joinGroup(GroupModel group) {
        throw new ReadOnlyException();
    }

    @Override
    public void leaveGroup(GroupModel group) {
        throw new ReadOnlyException();
    }

    @Override
    public void grantRole(RoleModel role) {
        throw new ReadOnlyException();
    }
}
