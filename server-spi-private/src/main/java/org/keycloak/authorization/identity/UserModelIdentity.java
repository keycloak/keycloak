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
package org.keycloak.authorization.identity;

import java.util.Map;

import org.keycloak.authorization.attribute.Attributes;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserModelIdentity implements Identity {
    protected RealmModel realm;
    protected UserModel user;

    public UserModelIdentity(RealmModel realm, UserModel user) {
        this.realm = realm;
        this.user = user;
    }

    @Override
    public String getId() {
        return user.getId();
    }

    @Override
    public Attributes getAttributes() {
        Map attr = user.getAttributes();
        return Attributes.from(attr);
    }

    @Override
    public boolean hasRealmRole(String roleName) {
        RoleModel role = realm.getRole(roleName);
        if (role == null) return false;
        return user.hasRole(role);
    }

    @Override
    public boolean hasOneClientRole(String clientId, String... roleNames) {
        ClientModel client = realm.getClientByClientId(clientId);
        for (String roleName : roleNames) {
            RoleModel role = client.getRole(roleName);
            if (role == null) continue;
            if (user.hasRole(role)) return true;
        }
        return false;
    }

    @Override
    public boolean hasClientRole(String clientId, String roleName) {
        ClientModel client = realm.getClientByClientId(clientId);
        RoleModel role = client.getRole(roleName);
        if (role == null) return false;
        return user.hasRole(role);
    }
}
