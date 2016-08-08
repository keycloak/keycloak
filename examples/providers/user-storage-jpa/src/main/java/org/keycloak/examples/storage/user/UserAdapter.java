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
package org.keycloak.examples.storage.user;

import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAdapter extends AbstractUserAdapterFederatedStorage {
    private static final Logger logger = Logger.getLogger(EjbExampleUserStorageProvider.class);
    protected UserEntity entity;
    protected String keycloakId;

    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, UserEntity entity) {
        super(session, realm, model);
        this.entity = entity;
        keycloakId = StorageId.keycloakId(model, entity.getId());
    }

    @Override
    public String getUsername() {
        return entity.getUsername();
    }

    @Override
    public void setUsername(String username) {
        entity.setUsername(username);

    }

    @Override
    public void setEmail(String email) {
        entity.setEmail(email);
    }

    @Override
    public String getEmail() {
        return entity.getEmail();
    }

    @Override
    public String getId() {
        return keycloakId;
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            entity.setPassword(cred.getValue());
        } else {
            super.updateCredential(cred);
        }
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        if (name.equals("phone")) {
            entity.setPhone(value);
        } else if (name.equals("password")) {
            // ignore

            // having a "password" attribute is a workaround so that passwords can be cached.  All done for performance reasons...
            // If we override getCredentialsDirectly/updateCredentialsDirectly
            // then the realm passsword policy will/may try and overwrite the plain text password with a hash.
            // If you don't like this workaround, you can query the database every time to validate the password
        } else {
            super.setSingleAttribute(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        if (name.equals("phone")) {
            entity.setPhone(null);
        } else if (name.equals("password")) {
            // ignore

            // having a "password" attribute is a workaround so that passwords can be cached.  All done for performance reasons...
            // If we override getCredentialsDirectly/updateCredentialsDirectly
            // then the realm passsword policy will/may try and overwrite the plain text password with a hash.
            // If you don't like this workaround, you can query the database every time to validate the password
        } else {
            super.removeAttribute(name);
        }
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (name.equals("phone")) {
            entity.setPhone(values.get(0));
        } else if (name.equals("password")) {
            // ignore

            // having a "password" attribute is a workaround so that passwords can be cached.  All done for performance reasons...
            // If we override getCredentialsDirectly/updateCredentialsDirectly
            // then the realm passsword policy will/may try and overwrite the plain text password with a hash.
            // If you don't like this workaround, you can query the database every time to validate the password
        } else {
            super.setAttribute(name, values);
        }
    }

    @Override
    public String getFirstAttribute(String name) {
        if (name.equals("phone")) {
            return entity.getPhone();
        } else if (name.equals("password")) {
            // having a "password" attribute is a workaround so that passwords can be cached.  All done for performance reasons...
            // If we override getCredentialsDirectly/updateCredentialsDirectly
            // then the realm passsword policy will/may try and overwrite the plain text password with a hash.
            // If you don't like this workaround, you can query the database every time to validate the password
            return entity.getPassword();
        } else {
            return super.getFirstAttribute(name);
        }
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attrs = super.getAttributes();
        MultivaluedHashMap<String, String> all = new MultivaluedHashMap<>();
        all.putAll(attrs);
        all.add("phone", entity.getPhone());

        // having a "password" attribute is a workaround so that passwords can be cached.  All done for performance reasons...
        // If we override getCredentialsDirectly/updateCredentialsDirectly
        // then the realm passsword policy will/may try and overwrite the plain text password with a hash.
        // If you don't like this workaround, you can query the database every time to validate the password
        all.add("password", entity.getPassword());
        return all;
    }

    @Override
    public List<String> getAttribute(String name) {
        if (name.equals("phone")) {
            List<String> phone = new LinkedList<>();
            phone.add(entity.getPhone());
            return phone;
        } else {
            return super.getAttribute(name);
        }
    }
}
