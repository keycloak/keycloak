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

package org.keycloak.models.jpa;

import java.security.SecureRandom;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.keycloak.common.util.Time;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.jpa.entities.MigrationModelEntity;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrationModelAdapter implements MigrationModel {
    protected EntityManager em;
    protected MigrationModelEntity latest;

    private static final int RESOURCE_TAG_LENGTH = 5;
    private static final char[] RESOURCE_TAG_CHARSET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

    public MigrationModelAdapter(EntityManager em) {
        this.em = em;
        init();
    }

    @Override
    public String getStoredVersion() {
        return latest != null ? latest.getVersion() : null;
    }

    @Override
    @Deprecated
    public String getResourcesTag() {
        return latest != null ? latest.getId() : null;
    }

    private void init() {
        TypedQuery<MigrationModelEntity> q = em.createNamedQuery("getLatest", MigrationModelEntity.class);
        q.setMaxResults(1);
        List<MigrationModelEntity> l = q.getResultList();
        if (l.isEmpty()) {
            latest = null;
        } else {
            latest = l.get(0);
        }
    }

    @Override
    public void setStoredVersion(String version) {
        String resourceTag = createResourceTag();

        // Make sure resource-tag is unique within current installation
        while (em.find(MigrationModelEntity.class, resourceTag) != null) {
            resourceTag = createResourceTag();
        }

        MigrationModelEntity entity = new MigrationModelEntity();
        entity.setId(resourceTag);
        entity.setVersion(version);
        entity.setUpdatedTime(Time.currentTime());

        em.persist(entity);

        latest = entity;
    }

    private String createResourceTag() {
        StringBuilder sb = new StringBuilder(RESOURCE_TAG_LENGTH);
        for (int i = 0; i < RESOURCE_TAG_LENGTH; i++) {
            sb.append(RESOURCE_TAG_CHARSET[new SecureRandom().nextInt(RESOURCE_TAG_CHARSET.length)]);
        }
        return sb.toString();
    }

}
