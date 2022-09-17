/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.singleUseObject;

import org.keycloak.models.ActionTokenKeyModel;
import org.keycloak.models.ActionTokenValueModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;

import java.util.Objects;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public abstract class AbstractSingleUseObjectModel<E extends AbstractEntity> implements ActionTokenKeyModel, ActionTokenValueModel {

    protected final KeycloakSession session;
    protected final E entity;

    public AbstractSingleUseObjectModel(KeycloakSession session, E entity) {
        Objects.requireNonNull(entity, "entity");

        this.session = session;
        this.entity = entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActionTokenValueModel)) return false;

        MapSingleUseObjectAdapter that = (MapSingleUseObjectAdapter) o;
        return Objects.equals(that.entity.getId(), entity.getId());
    }

    @Override
    public int hashCode() {
        return entity.getId().hashCode();
    }
}
