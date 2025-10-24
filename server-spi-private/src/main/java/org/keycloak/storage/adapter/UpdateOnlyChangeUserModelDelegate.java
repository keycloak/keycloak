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

package org.keycloak.storage.adapter;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.models.utils.UserModelDelegate;

import static org.keycloak.common.util.ObjectUtil.isEqualOrBothNull;

/**
 * This will perform update operation for particular attribute/property just if the existing value is not already same.
 * In other words, just "real updates" will be passed to the delegate.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UpdateOnlyChangeUserModelDelegate extends UserModelDelegate {

    public UpdateOnlyChangeUserModelDelegate(UserModel delegate) {
        super(delegate);
    }

    @Override
    public void setUsername(String username) {
        if (!isEqualOrBothNull(getUsername(), username)) {
            delegate.setUsername(username);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!isEqualOrBothNull(isEnabled(), enabled)) {
            delegate.setEnabled(enabled);
        }
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        if (!isEqualOrBothNull(getFirstAttribute(name), value)) {
            delegate.setSingleAttribute(name, value);
        }
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (!isEqualOrBothNull(getAttributeStream(name).collect(Collectors.toList()), values)) {
            delegate.setAttribute(name, values);
        }
    }

    @Override
    public void removeAttribute(String name) {
        if (getAttributeStream(name).count() > 0) {
            delegate.removeAttribute(name);
        }
    }

    @Override
    public void addRequiredAction(String action) {
        if (action != null && getRequiredActionsStream().noneMatch(action::equals)) {
            delegate.addRequiredAction(action);
        }
    }

    @Override
    public void removeRequiredAction(String action) {
        if (action != null && getRequiredActionsStream().anyMatch(action::equals)) {
            delegate.removeRequiredAction(action);
        }
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        String actionName = action.name();
        addRequiredAction(actionName);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        String actionName = action.name();
        removeRequiredAction(actionName);
    }


    @Override
    public void setFirstName(String firstName) {
        if (!isEqualOrBothNull(getFirstName(), firstName)) {
            delegate.setFirstName(firstName);
        }
    }

    @Override
    public void setLastName(String lastName) {
        if (!isEqualOrBothNull(getLastName(), lastName)) {
            delegate.setLastName(lastName);
        }
    }

    @Override
    public void setEmail(String email) {
        if (!isEqualOrBothNull(getEmail(), email)) {
            delegate.setEmail(email);
        }
    }



    @Override
    public void setEmailVerified(boolean verified) {
        if (!isEqualOrBothNull(isEmailVerified(), verified)) {
            delegate.setEmailVerified(verified);
        }
    }

    @Override
    public void grantRole(RoleModel role) {
        if (!hasDirectRole(role)) {
            delegate.grantRole(role);
        }
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        if (hasDirectRole(role)) {
            delegate.deleteRoleMapping(role);
        }
    }

    @Override
    public void setFederationLink(String link) {
        if (!isEqualOrBothNull(getFederationLink(), link)) {
            delegate.setFederationLink(link);
        }
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        if (!isEqualOrBothNull(getServiceAccountClientLink(), clientInternalId)) {
            delegate.setServiceAccountClientLink(clientInternalId);
        }
    }

    @Override
    public void setCreatedTimestamp(Long timestamp){
        if (!isEqualOrBothNull(getCreatedTimestamp(), timestamp)) {
            delegate.setCreatedTimestamp(timestamp);
        }
    }

    @Override
    public void joinGroup(GroupModel group) {
        if (!RoleUtils.isDirectMember(getGroupsStream(),group)) {
            delegate.joinGroup(group);
        }

    }

    @Override
    public void leaveGroup(GroupModel group) {
        if (RoleUtils.isDirectMember(getGroupsStream(),group)) {
            delegate.leaveGroup(group);
        }
    }


}
