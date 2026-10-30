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

package org.keycloak.models.sessions.infinispan.changes;

import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * Explicit result of importing a database snapshot into a session cache.
 */
public record SessionImportResult<V extends SessionEntity>(Status status, SessionEntityWrapper<V> existingSession) {

    public enum Status {
        PUBLISHED,
        EXISTING,
        REQUEST_LOCAL
    }

    public static <V extends SessionEntity> SessionImportResult<V> published() {
        return new SessionImportResult<>(Status.PUBLISHED, null);
    }

    public static <V extends SessionEntity> SessionImportResult<V> existing(SessionEntityWrapper<V> existingSession) {
        return new SessionImportResult<>(Status.EXISTING, existingSession);
    }

    public static <V extends SessionEntity> SessionImportResult<V> requestLocal() {
        return new SessionImportResult<>(Status.REQUEST_LOCAL, null);
    }
}
