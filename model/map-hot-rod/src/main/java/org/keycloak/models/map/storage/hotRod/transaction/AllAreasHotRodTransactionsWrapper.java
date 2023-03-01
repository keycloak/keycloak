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

package org.keycloak.models.map.storage.hotRod.transaction;

import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.map.storage.MapKeycloakTransaction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * This wrapper encapsulates transactions from all areas. This is needed because we need to control when the changes
 * from each area are applied to make sure it is performed before the HotRod client provided transaction is committed.
 */
public class AllAreasHotRodTransactionsWrapper extends AbstractKeycloakTransaction {

    private final Map<Class<?>, MapKeycloakTransaction<?, ?>> MapKeycloakTransactionsMap = new ConcurrentHashMap<>();

    public MapKeycloakTransaction<?, ?> getOrCreateTxForModel(Class<?> modelType, Supplier<MapKeycloakTransaction<?,?>> supplier) {
        MapKeycloakTransaction<?, ?> tx = MapKeycloakTransactionsMap.computeIfAbsent(modelType, t -> supplier.get());
        if (!tx.isActive()) {
            tx.begin();
        }

        return tx;
    }

    @Override
    protected void commitImpl() {
        MapKeycloakTransactionsMap.values().forEach(MapKeycloakTransaction::commit);
    }

    @Override
    protected void rollbackImpl() {
        MapKeycloakTransactionsMap.values().forEach(MapKeycloakTransaction::rollback);
    }

    @Override
    public void setRollbackOnly() {
        super.setRollbackOnly();
        MapKeycloakTransactionsMap.values().forEach(MapKeycloakTransaction::setRollbackOnly);
    }
}
