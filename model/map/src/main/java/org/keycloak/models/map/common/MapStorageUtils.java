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
package org.keycloak.models.map.common;

import org.keycloak.models.map.storage.MapKeycloakTransaction;

/**
 *
 * @author hmlnarik
 */
public class MapStorageUtils {

    /**
     * Returns a deep clone of an entity. If the clone is already in the transaction, returns this one.
     * <p>
     * Usually used before giving an entity from a source back to the caller,
     * to prevent changing it directly in the data store, but to keep transactional properties.
     * @param <K>
     * @param <V>
     * @param tx Transaction that is checked for existence of the entity before
     * @param origEntity
     * @return
     */
    public static <K, V extends AbstractEntity<K>> V registerEntityForChanges(MapKeycloakTransaction<K, V, ?> tx, V origEntity) {
        final V res = tx.read(origEntity.getId(), id -> Serialization.from(origEntity));
        tx.updateIfChanged(res, AbstractEntity<K>::isUpdated);
        return res;
    }
}
