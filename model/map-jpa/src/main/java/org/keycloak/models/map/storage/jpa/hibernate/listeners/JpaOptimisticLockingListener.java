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
import org.hibernate.Session;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.keycloak.models.map.storage.jpa.JpaChildEntity;

import javax.persistence.LockModeType;
import java.util.Objects;

/**
 * Listen on changes on child entities and forces an optimistic locking increment on the topmost parent aka root.
 *
 * This support a multiple level parent-child relationship, where only the upmost parent is locked.
 */
public class JpaOptimisticLockingListener implements PreInsertEventListener, PreDeleteEventListener, PreUpdateEventListener {

    public static final JpaOptimisticLockingListener INSTANCE = new JpaOptimisticLockingListener();

    /**
     * Check if the entity is a child with a parent and force optimistic locking increment on the upmost parent aka root.
     */
    public void lockRootEntity(Session session, Object entity) throws HibernateException {
        if(entity instanceof JpaChildEntity) {
            Object root = entity;
            while (root instanceof JpaChildEntity) {
                root = ((JpaChildEntity<?>) entity).getParent();
                Objects.requireNonNull(root, "children must always return their parent, never null");
            }

            // a session would not contain the entity if it has been deleted
            // if the entity has been deleted JPA would throw an IllegalArgumentException with the message
            // "entity not in the persistence context".
            if (session.contains(root)) {
                session.lock(root, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            }
        }
    }

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        lockRootEntity(event.getSession(), event.getEntity());
        return false;
    }

    @Override
    public boolean onPreDelete(PreDeleteEvent event) {
        lockRootEntity(event.getSession(), event.getEntity());
        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        lockRootEntity(event.getSession(), event.getEntity());
        return false;
    }
}
