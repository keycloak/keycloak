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
package org.keycloak.models.map.storage.file;


import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapCrudOperations;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapKeycloakTransaction;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;

/**
 * {@link MapKeycloakTransaction} implementation used with the file map storage.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class FileMapKeycloakTransaction<V extends AbstractEntity & UpdatableEntity, M> extends ConcurrentHashMapKeycloakTransaction<String, V, M> {

    public FileMapKeycloakTransaction(Class<V> entityClass, ConcurrentHashMapCrudOperations<V, M> crud) {
        super(
          crud,
          StringKeyConverter.StringKey.INSTANCE,
          DeepCloner.DUMB_CLONER,
          MapFieldPredicates.getPredicates(ModelEntityUtil.getModelType(entityClass)),
          ModelEntityUtil.getRealmIdField(entityClass)
        );
    }
}
