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

package org.keycloak.storage.ldap;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.ReadOnlyException;

/**
 * Will be good to get rid of this class and use ReadOnlyUserModelDelegate, but it can't be done now due the backwards compatibility.
 * See KEYCLOAK-15139 as an example
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ReadonlyLDAPUserModelDelegate extends UserModelDelegate {

    public ReadonlyLDAPUserModelDelegate(UserModel delegate) {
        super(delegate);
    }

    @Override
    public void setUsername(String username) {
        if (!Objects.equals(getUsername(), username)) {
            throw new ReadOnlyException("Federated storage is not writable");
        }
    }

    @Override
    public void setLastName(String lastName) {
        if (!Objects.equals(getLastName(), lastName)) {
            throw new ReadOnlyException("Federated storage is not writable");
        }
    }

    @Override
    public void setFirstName(String first) {
        if (!Objects.equals(getFirstName(), first)) {
            throw new ReadOnlyException("Federated storage is not writable");
        }
    }

    @Override
    public void setEmail(String email) {
        if (!Objects.equals(getEmail(), email)) {
            throw new ReadOnlyException("Federated storage is not writable");
        }
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        if (!Objects.equals(getAttributeStream(name).collect(Collectors.toList()), Collections.singletonList(value))) {
            throw new ReadOnlyException("Federated storage is not writable");
        }
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (!Objects.equals(getAttributeStream(name).collect(Collectors.toList()), values)) {
            throw new ReadOnlyException("Federated storage is not writable");
        }
    }

    @Override
    public void removeAttribute(String name) {
        if (getAttributeStream(name).count() > 0) {
            throw new ReadOnlyException("Federated storage is not writable");
        }
    }
}
