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

package org.keycloak.models.jpa.session;

import java.util.Objects;

/**
 * Record representing a {@link org.keycloak.models.AuthenticatedClientSessionModel}, with user session ID and its
 * associated client session ID.
 *
 * @param userSessionId         the user session ID (never null)
 * @param clientSessionId       the client ID for internal clients, or "external" for external clients (can be null from
 *                              LEFT JOIN)
 * @param clientStorageProvider the storage provider for external clients (can be null from LEFT JOIN)
 * @param externalClientId      the external client ID (can be null from LEFT JOIN)
 */
public record UserSessionIdAndClientSessionId(String userSessionId, String clientSessionId,
                                              String clientStorageProvider, String externalClientId) {

    public UserSessionIdAndClientSessionId {
        Objects.requireNonNull(userSessionId, "userSessionId");
        // clientSessionId, clientStorageProvider, and externalClientId can be null from LEFT JOIN
        // when a user session exists without any associated client sessions
    }


}
