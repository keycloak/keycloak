/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.jpa.hibernate.dirtiness;

import org.hibernate.Session;
import org.hibernate.boot.internal.DefaultCustomEntityDirtinessStrategy;
import org.hibernate.persister.entity.EntityPersister;
import org.keycloak.models.map.common.UpdatableEntity;

/**
 * A Dirtiness strategy that can check the dirtiness of an entity if it is an
 * {@link UpdatableEntity} and when it hasn't been updated.
 * If it has been updated, the standard mechanisms of Hibernate are in effect to find out if it is really dirty,
 * and which fields have been changed.
 *
 * This avoids an expensive check for attributes like metadata that would otherwise traverse a lot of entries.
 *
 * @author Alexander Schwartz
 */
public class JpaDirtinessStrategy extends DefaultCustomEntityDirtinessStrategy {

    @Override
    public boolean canDirtyCheck(Object entity, EntityPersister persister, Session session) {
        return entity instanceof UpdatableEntity && !(((UpdatableEntity) entity).isUpdated());
    }

    @Override
    public boolean isDirty(Object entity, EntityPersister persister, Session session) {
        // as the previous call of canDirtyCheck only returned true for non-updated entity, we can return false here without an additional check
        return false;
    }

    @Override
    public void resetDirty(Object entity, EntityPersister persister, Session session) {
        if (entity instanceof UpdatableEntity) {
            ((UpdatableEntity) entity).clearUpdatedFlag();
        }
    }

    @Override
    public void findDirty(Object entity, EntityPersister persister, Session session, DirtyCheckContext dirtyCheckContext) {
        // Hibernate will have already found the modified attributes
    }
}
