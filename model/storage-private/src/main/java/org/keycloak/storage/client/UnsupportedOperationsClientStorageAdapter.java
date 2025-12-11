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

import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RoleModel;

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
    public final Stream<RoleModel> getRolesStream() {
        return Stream.empty();
    }
    
    @Override
    public final Stream<RoleModel> getRolesStream(Integer first, Integer max) {
        return Stream.empty();
    }
    
    @Override
    public final Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        return Stream.empty();
    }

}
