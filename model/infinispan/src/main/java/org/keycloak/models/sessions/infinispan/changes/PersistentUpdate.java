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

package org.keycloak.models.sessions.infinispan.changes;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.keycloak.models.KeycloakSession;

import io.opentelemetry.api.trace.Span;

/**
 * Capture information for a deferred update of the session stores.
 *
 * @author Alexander Schwartz
 */
public class PersistentUpdate {

    private final Consumer<KeycloakSession> task;
    private final CompletableFuture<Void> future = new CompletableFuture<>();
    private final Span span;

    public PersistentUpdate(Consumer<KeycloakSession> task) {
        this.task = task;
        this.span = Span.current();
    }

    public void perform(KeycloakSession session) {
        task.accept(session);
    }

    public void complete() {
        future.complete(null);
    }

    public void fail(Throwable throwable) {
        future.completeExceptionally(throwable);
    }

    public CompletableFuture<Void> future() {
        return future;
    }

    public Span getSpan() {
        return span;
    }
}
