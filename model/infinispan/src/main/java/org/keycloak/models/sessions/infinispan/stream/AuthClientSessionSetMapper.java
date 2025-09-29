/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
import java.util.Set;
import java.util.function.Function;

import org.infinispan.CacheStream;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * A {@link Function} to be used by {@link CacheStream} to extract the client's ID from the client sessions associated
 * to a {@link UserSessionEntity}.
 * <p>
 * This function is marshaled with ProtoStream.
 */
@ProtoTypeId(Marshalling.AUTHENTICATION_CLIENT_SESSION_KEY_SET_MAPPER)
public class AuthClientSessionSetMapper implements Function<Map.Entry<String, SessionEntityWrapper<UserSessionEntity>>, Set<String>> {

    private static final AuthClientSessionSetMapper INSTANCE = new AuthClientSessionSetMapper();

    private AuthClientSessionSetMapper() {
    }

    @ProtoFactory
    public static AuthClientSessionSetMapper getInstance() {
        return INSTANCE;
    }

    @Override
    public Set<String> apply(Map.Entry<String, SessionEntityWrapper<UserSessionEntity>> entry) {
        return entry.getValue().getEntity().getClientSessions();
    }
}
