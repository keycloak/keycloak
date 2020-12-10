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

package org.keycloak.models.map.user;

import java.util.Comparator;
import java.util.UUID;

public class MapUserEntity extends AbstractUserEntity<UUID> {

    public static final Comparator<MapUserEntity> COMPARE_BY_USERNAME = Comparator.comparing(MapUserEntity::getUsername, String.CASE_INSENSITIVE_ORDER);
    
    protected MapUserEntity() {
        super();
    }

    public MapUserEntity(UUID id, String realmId) {
        super(id, realmId);
    }
}
