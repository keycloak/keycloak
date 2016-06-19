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

package org.keycloak.examples.domainextension.services.repository;

import java.lang.reflect.ParameterizedType;

import javax.persistence.EntityManager;

public abstract class AbstractRepository<T> {

    private final EntityManager entityManager;
    private final Class<T> clazz;

    @SuppressWarnings("unchecked")
    public AbstractRepository(EntityManager entityManager) {
        this.entityManager = entityManager;

        clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    public T findById(String id) {
        return entityManager.find(clazz, id);
    }

    public void remove(T entity) {
        entityManager.remove(entity);
    }

    public void persist(T entity) {
        entityManager.persist(entity);
    }
}
