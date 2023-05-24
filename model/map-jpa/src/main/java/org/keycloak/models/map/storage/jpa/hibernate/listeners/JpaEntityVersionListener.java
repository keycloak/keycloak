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

package org.keycloak.models.map.storage.jpa.hibernate.listeners;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.keycloak.models.map.storage.jpa.JpaChildEntity;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;

/**
 * Listen on changes on child- and root entities and updates the current entity version of the root.
 *
 * This support a multiple level parent-child relationship, where all parents needs the entity version to be updated
 * in case it was effectively changed. The traversing is stopped at that point when it is detected that parent entity
 * version is the same one. 
 * 
 * It is based on an assumption that it may happen that one parent entity could be extracted into "parent A -> parent B -> child"
 * format. Then the change (insertion, deletion or update) of the child should bump the entity version of both parent B and parent A.
 */
public class JpaEntityVersionListener implements PreInsertEventListener, PreDeleteEventListener, PreUpdateEventListener {

    public static final JpaEntityVersionListener INSTANCE = new JpaEntityVersionListener();

    /**
     * Traverse from current entity through its parent tree and update the entity version of it. 
     * Stop if non-changed parent is found.
     */
    public void updateEntityVersion(Object entity) throws HibernateException {
        Object root = entity;
        if (root instanceof JpaChildEntity) {
            while (root instanceof JpaChildEntity) {
                root = ((JpaChildEntity<?>) entity).getParent();
                if (root instanceof JpaRootEntity) {
                    if (!((JpaRootEntity) root).updateEntityVersion()) {
                        return;
                    }
                }   
            }
        } else if (root instanceof JpaRootEntity) {
            ((JpaRootEntity) root).updateEntityVersion();
        }
    }

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        updateEntityVersion(event.getEntity());
        return false;
    }

    @Override
    public boolean onPreDelete(PreDeleteEvent event) {
        updateEntityVersion(event.getEntity());
        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        updateEntityVersion(event.getEntity());
        return false;
    }
}
