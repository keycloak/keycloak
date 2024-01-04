/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.policy.provider.user;

import java.util.function.BiFunction;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserPolicyProvider implements PolicyProvider {

    private static final Logger logger = Logger.getLogger(UserPolicyProvider.class);

    private final BiFunction<Policy, AuthorizationProvider, UserPolicyRepresentation> representationFunction;

    public UserPolicyProvider(BiFunction<Policy, AuthorizationProvider, UserPolicyRepresentation> representationFunction) {
        this.representationFunction = representationFunction;
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        EvaluationContext context = evaluation.getContext();
        UserPolicyRepresentation representation = representationFunction.apply(evaluation.getPolicy(), evaluation.getAuthorizationProvider());

        for (String userId : representation.getUsers()) {
            if (context.getIdentity().getId().equals(userId)) {
                evaluation.grant();
                break;
            }
        }
        logger.debugv("User policy {} evaluated to status {} on identity {} with accepted users: {}", evaluation.getPolicy().getName(), evaluation.getEffect(), evaluation.getContext().getIdentity().getId(), representation.getUsers());
    }

    @Override
    public void close() {

    }
}
