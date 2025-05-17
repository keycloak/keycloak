/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2025 Red Hat, Inc., and individual contributors
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

package org.keycloak.authorization.fgap.evaluation.partial;

import java.util.stream.Stream;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.ResourceType;

/**
 * {@link PolicyProvider} types can implement this interface if they support to partially evaluate the permissions
 * that should apply to a given subject, a {@link UserModel}, when calculating the realm resources they have access in the context of the
 * {@link org.keycloak.common.Profile.Feature#ADMIN_FINE_GRAINED_AUTHZ} feature.
 */
public interface PartialEvaluationPolicyProvider {

    /**
     * Returns a list of {@link Policy} instances representing the permissions that apply to a given {@code subject} when
     * partially evaluating the realm resources that can be accessed.
     *
     * @param session the session
     * @param resourceType the type of the resource
     * @param subject the subject
     * @return the permissions that apply to the given {@code subject}
     */
    Stream<Policy> getPermissions(KeycloakSession session, ResourceType resourceType, UserModel subject);

    /**
     * If partial evaluation is supported for the given {@code policy}.
     *
     * @param policy the policy
     * @return {@code true} if supported. Otherwise, returns {@code false}
     */
    boolean supports(Policy policy);

    /**
     * Evaluate if the given {@code policy} grants access to the given {@code subject}.
     *
     * @param session the session
     * @param policy the policy
     * @param subject the subject
     * @return {@code true} if access is granted. Otherwise, returns {@code false}
     */
    boolean evaluate(KeycloakSession session, Policy policy, UserModel subject);
}
