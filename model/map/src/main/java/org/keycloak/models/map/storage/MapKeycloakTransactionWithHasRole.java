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

package org.keycloak.models.map.storage;

import org.keycloak.models.map.role.MapRoleEntity;

import java.util.Set;

/**
 * A map store transaction that can check if a role is part of another role.
 * This must only be used if this store contains all the role information for the given realm.
 * @author Alexander Schwartz
 */
public interface MapKeycloakTransactionWithHasRole<V extends MapRoleEntity, M> extends MapKeycloakTransaction<V, M> {


    boolean hasRole(String realmId, Set<String> roleIds, Set<String> targetRoleIds);

    Set<V> expandCompositeRoles(String realmId, Set<String> targetRoles);
}
