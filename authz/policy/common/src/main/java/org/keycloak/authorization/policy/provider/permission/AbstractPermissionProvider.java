/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.policy.provider.permission;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.DefaultEvaluation;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AbstractPermissionProvider implements PolicyProvider {

    public AbstractPermissionProvider() {

    }

    @Override
    public void evaluate(Evaluation evaluation) {
        if (!(evaluation instanceof DefaultEvaluation)) {
            throw new IllegalArgumentException("Unexpected evaluation instance type [" + evaluation.getClass() + "]");
        }

        Policy policy = evaluation.getPolicy();
        AuthorizationProvider authorization = evaluation.getAuthorizationProvider();

        policy.getAssociatedPolicies().forEach(associatedPolicy -> {
            PolicyProvider policyProvider = authorization.getProvider(associatedPolicy.getType());
            DefaultEvaluation.class.cast(evaluation).setPolicy(associatedPolicy);
            policyProvider.evaluate(evaluation);
            evaluation.denyIfNoEffect();
        });
    }

    @Override
    public void close() {

    }
}
