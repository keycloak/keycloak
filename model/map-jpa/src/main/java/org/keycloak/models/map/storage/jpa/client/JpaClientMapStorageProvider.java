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
package org.keycloak.models.map.storage.jpa.client;

import javax.persistence.EntityManager;
import org.keycloak.models.ClientModel;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Flag;

public class JpaClientMapStorageProvider implements MapStorageProvider {

    private final EntityManager em;

    public JpaClientMapStorageProvider(EntityManager em) {
        this.em = em;
    }

    @Override
    public void close() {
        em.close();
    }

    @Override
    public MapStorage<MapClientEntity, ClientModel> getStorage(Class modelType, Flag... flags) {
        return new JpaClientMapStorage(em);
    }
}
