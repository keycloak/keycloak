/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.stream;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;

import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoTypeId;

import static org.keycloak.marshalling.Marshalling.CLIENT_SESSION_USER_FILTER;

/**
 * A {@link Predicate} to filter {@link AuthenticatedClientSessionEntity} values based on the Realm ID and the User ID.
 *
 * @param realmId The Realm ID.
 * @param userId  The User ID.
 */
@ProtoTypeId(CLIENT_SESSION_USER_FILTER)
@Proto
public record ClientSessionFilterByUser(String realmId,
                                        String userId) implements Predicate<Map.Entry<?, SessionEntityWrapper<AuthenticatedClientSessionEntity>>> {

    @Override
    public boolean test(Map.Entry<?, SessionEntityWrapper<AuthenticatedClientSessionEntity>> entry) {
        var entity = entry.getValue().getEntity();
        return Objects.equals(userId, entity.getUserId()) &&
                Objects.equals(realmId, entity.getRealmId());
    }
}
