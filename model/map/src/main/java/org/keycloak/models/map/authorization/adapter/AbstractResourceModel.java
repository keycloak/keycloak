/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.authorization.adapter;

import org.keycloak.authorization.model.AbstractAuthorizationModel;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.map.common.AbstractEntity;

import java.util.Objects;

public abstract class AbstractResourceModel<E extends AbstractEntity> extends AbstractAuthorizationModel implements Resource {

    protected final E entity;

    public AbstractResourceModel(E entity, StoreFactory storeFactory) {
        super(storeFactory);

        Objects.requireNonNull(entity, "entity");
        this.entity = entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource)) return false;

        Resource that = (Resource) o;
        return Objects.equals(that.getId(), getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
