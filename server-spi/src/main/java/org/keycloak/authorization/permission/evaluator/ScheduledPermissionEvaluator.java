/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.permission.evaluator;

import org.keycloak.authorization.Decision;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 * @see PermissionEvaluator
 */
class ScheduledPermissionEvaluator implements PermissionEvaluator {

    private final PermissionEvaluator publisher;
    private final Executor scheduler;

    ScheduledPermissionEvaluator(PermissionEvaluator publisher, Executor scheduler) {
        this.publisher = publisher;
        this.scheduler = scheduler;
    }

    @Override
    public void evaluate(Decision decision) {
        CompletableFuture.runAsync(() -> publisher.evaluate(decision), scheduler);
    }
}
