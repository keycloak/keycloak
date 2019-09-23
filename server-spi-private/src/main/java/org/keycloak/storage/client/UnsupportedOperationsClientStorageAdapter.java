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
package org.keycloak.storage.client;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RoleModel;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Base helper class.  Unsupported operations are implemented here that throw exception on invocation.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class UnsupportedOperationsClientStorageAdapter implements ClientModel {
    @Override
    public final RoleModel getRole(String name) {
        return null;
    }

    @Override
    public final RoleModel addRole(String name) {
        throw new ModelException("Unsupported operation");
    }

    @Override
    public final RoleModel addRole(String id, String name) {
        throw new ModelException("Unsupported operation");
    }

    @Override
    public final boolean removeRole(RoleModel role) {
        throw new ModelException("Unsupported operation");
    }

    @Override
    public final Set<RoleModel> getRoles() {
        return Collections.EMPTY_SET;
    }
    
    @Override
    public final Set<RoleModel> getRoles(Integer first, Integer max) {
        return Collections.EMPTY_SET;
    }
    
    @Override
    public final Set<RoleModel> searchForRoles(String search, Integer first, Integer max) {
        return Collections.EMPTY_SET;
    }

    @Override
    public final List<String> getDefaultRoles() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public final void addDefaultRole(String name) {
        throw new ModelException("Unsupported operation");

    }

    @Override
    public final void updateDefaultRoles(String... defaultRoles) {
        throw new ModelException("Unsupported operation");

    }

    @Override
    public final void removeDefaultRoles(String... defaultRoles) {
        throw new ModelException("Unsupported operation");
    }


}
