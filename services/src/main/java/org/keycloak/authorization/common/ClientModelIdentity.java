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
package org.keycloak.authorization.common;

import org.keycloak.authorization.attribute.Attributes;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientModelIdentity implements Identity {
    protected RealmModel realm;
    protected ClientModel client;
    protected UserModel serviceAccount;

    public ClientModelIdentity(KeycloakSession session, ClientModel client) {
        this.realm = client.getRealm();
        this.client = client;
        this.serviceAccount = session.users().getServiceAccount(client);
    }

    @Override
    public String getId() {
        return client.getId();
    }

    @Override
    public Attributes getAttributes() {
        MultivaluedHashMap map = new MultivaluedHashMap<String, String>();
        if (serviceAccount != null) map.addAll(serviceAccount.getAttributes());
        return Attributes.from(map);
    }

    @Override
    public boolean hasRealmRole(String roleName) {
        if (serviceAccount == null) return false;
        RoleModel role = realm.getRole(roleName);
        if (role == null) return false;
        return serviceAccount.hasRole(role);
    }

    @Override
    public boolean hasClientRole(String clientId, String roleName) {
        if (serviceAccount == null) return false;
        ClientModel client = realm.getClientByClientId(clientId);
        RoleModel role = client.getRole(roleName);
        if (role == null) return false;
        return serviceAccount.hasRole(role);
    }
}
