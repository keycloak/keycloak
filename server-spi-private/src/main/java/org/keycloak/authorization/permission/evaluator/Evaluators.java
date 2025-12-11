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

import java.util.Collection;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;

/**
 * A factory for the different {@link PermissionEvaluator} implementations.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class Evaluators {

    private final AuthorizationProvider authorizationProvider;

    public Evaluators(AuthorizationProvider authorizationProvider) {
        this.authorizationProvider = authorizationProvider;
    }

    public PermissionEvaluator from(Collection<ResourcePermission> permissions, EvaluationContext evaluationContext) {
        return new IterablePermissionEvaluator(permissions.iterator(), evaluationContext, authorizationProvider);
    }

    public PermissionEvaluator from(Collection<ResourcePermission> permissions, ResourceServer resourceServer, EvaluationContext evaluationContext) {
        return new IterablePermissionEvaluator(permissions.iterator(), resourceServer, evaluationContext, authorizationProvider);
    }

    public PermissionEvaluator from(EvaluationContext evaluationContext, ResourceServer resourceServer, AuthorizationRequest request) {
        return new UnboundedPermissionEvaluator(evaluationContext, authorizationProvider, resourceServer, request);
    }
}
