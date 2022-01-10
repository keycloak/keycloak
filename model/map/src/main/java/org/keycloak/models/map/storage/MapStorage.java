/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;

/**
 * Implementation of this interface interacts with a persistence storage storing various entities, e.g. users, realms.
 *
 * @author hmlnarik
 * @param <V> Type of the stored values that contains all the data stripped of session state. In other words, in the entities
 *            there are only IDs and mostly primitive types / {@code String}, never references to {@code *Model} instances.
 *            See the {@code Abstract*Entity} classes in this module.
 * @param <M> Type of the {@code *Model} corresponding to the stored value, e.g. {@code UserModel}. This is used for
 *            filtering via model fields in {@link ModelCriteriaBuilder} which is necessary to abstract from physical
 *            layout and thus to support no-downtime upgrade.
 */
public interface MapStorage<V extends AbstractEntity, M> {
    
    /**
     * Creates a {@code MapKeycloakTransaction} object that tracks a new transaction related to this storage.
     * In case of JPA or similar, the transaction object might be supplied by the container (via JTA) or
     * shared same across storages accessing the same database within the same session; in other cases
     * (e.g. plain map) a separate transaction handler might be created per each storage.
     *
     * @return See description. Never returns {@code null}
     */
    MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session);

}
