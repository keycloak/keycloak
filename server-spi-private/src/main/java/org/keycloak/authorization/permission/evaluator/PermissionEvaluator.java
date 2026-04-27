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

import org.keycloak.authorization.Decision;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.Permission;

/**
 * An {@link PermissionEvaluator} represents a source of {@link org.keycloak.authorization.permission.ResourcePermission}, responsible for emitting these permissions
 * to a consumer in order to evaluate the authorization policies based on a {@link org.keycloak.authorization.policy.evaluation.EvaluationContext}.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface PermissionEvaluator {

    <D extends Decision> D evaluate(D decision);
    Collection<Permission> evaluate(ResourceServer resourceServer, AuthorizationRequest request);
    <D extends Decision<?>> D getDecision(ResourceServer resourceServer, AuthorizationRequest request, Class<D> decisionType);
}
