/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * <p>Function definition used for the lifespan and idle calculations for the infinispan
 * session entities. The method receives the realm, client if needed (it's optional)
 * and the entity. It returns the timestamp for the entity (lifespan, idle
 * timeout,...) in milliseconds.</p>
 *
 * @param <V> The session entity to apply the function
 *
 * @author rmartinc
 */
@FunctionalInterface
public interface SessionFunction<V extends SessionEntity> {

    Long apply(RealmModel realm, ClientModel client, V entity);
}
