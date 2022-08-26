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

package org.keycloak.models.map.storage.hotRod.userSession;

import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntityFields;

public abstract class HotRodAuthenticatedClientSessionEntityDelegateProvider implements DelegateProvider<MapAuthenticatedClientSessionEntity> {

    private MapAuthenticatedClientSessionEntity fullClientSessionData;
    private MapAuthenticatedClientSessionEntity idClientIdReferenceOnly;

    public HotRodAuthenticatedClientSessionEntityDelegateProvider(MapAuthenticatedClientSessionEntity idClientIdReferenceOnly) {
        this.idClientIdReferenceOnly = idClientIdReferenceOnly;
    }

    @Override
    public MapAuthenticatedClientSessionEntity getDelegate(boolean isRead, Enum<? extends EntityField<MapAuthenticatedClientSessionEntity>> field, Object... parameters) {
        if (fullClientSessionData != null) return fullClientSessionData;

        if (isRead) {
            switch ((MapAuthenticatedClientSessionEntityFields) field) {
                case ID:
                case CLIENT_ID:
                    return idClientIdReferenceOnly;
            }
        }

        fullClientSessionData = loadClientSessionFromDatabase();
        if (fullClientSessionData == null) {
            throw new ModelIllegalStateException("Unable to retrieve client session data with id: " + idClientIdReferenceOnly.getId());
        }

        return fullClientSessionData;
    }

    public abstract MapAuthenticatedClientSessionEntity loadClientSessionFromDatabase();
}
