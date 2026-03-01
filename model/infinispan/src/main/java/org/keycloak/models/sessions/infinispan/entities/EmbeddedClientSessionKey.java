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

package org.keycloak.models.sessions.infinispan.entities;

import org.keycloak.marshalling.Marshalling;

import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * The key stored in the {@link org.infinispan.Cache} for {@link AuthenticatedClientSessionEntity}.
 * <p>
 * Although this class is the same as {@link ClientSessionKey}, we keep them separates so they can evolve independent.
 */
@ProtoTypeId(Marshalling.EMBEDDED_CLIENT_SESSION_KEY)
@Proto
public record EmbeddedClientSessionKey(String userSessionId, String clientId) {

    public String toId() {
        return userSessionId + "::" + clientId;
    }

}
