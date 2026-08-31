/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.Objects;
import java.util.function.Predicate;

import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.RootAuthenticationSessionEntity;

import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoTypeId;

import static org.keycloak.marshalling.Marshalling.AUTHENTICATED_USER_AUTH_SESSION_PREDICATE;

/**
 * A {@link Predicate} to match {@link RootAuthenticationSessionEntity} values that hold an in progress
 * authentication session for the given user in the given realm, except for an optional root session id to keep.
 *
 * @param realmId                            The Realm ID.
 * @param userId                             The authenticated User ID.
 * @param rootAuthenticationSessionIdToKeep  Optional root authentication session id to keep; {@code null} keeps none.
 */
@ProtoTypeId(AUTHENTICATED_USER_AUTH_SESSION_PREDICATE)
@Proto
public record AuthenticatedUserAuthSessionPredicate(String realmId, String userId,
                                                    String rootAuthenticationSessionIdToKeep)
        implements Predicate<SessionEntityWrapper<RootAuthenticationSessionEntity>> {

    @Override
    public boolean test(SessionEntityWrapper<RootAuthenticationSessionEntity> wrapper) {
        var entity = wrapper.getEntity();
        return Objects.equals(realmId, entity.getRealmId())
                && entity.hasAuthenticationSessionForUser(userId)
                && !Objects.equals(rootAuthenticationSessionIdToKeep, entity.getId());
    }
}
