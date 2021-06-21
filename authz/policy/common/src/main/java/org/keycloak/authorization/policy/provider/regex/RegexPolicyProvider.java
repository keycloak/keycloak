/*
 *  Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.policy.provider.regex;

import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.attribute.Attributes;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.representations.idm.authorization.RegexPolicyRepresentation;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class RegexPolicyProvider implements PolicyProvider {

    private final BiFunction<Policy, AuthorizationProvider, RegexPolicyRepresentation> representationFunction;

    public RegexPolicyProvider(BiFunction<Policy, AuthorizationProvider, RegexPolicyRepresentation> representationFunction) {
        this.representationFunction = representationFunction;
    }

    @Override
    public void close() {
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        AuthorizationProvider authorizationProvider = evaluation.getAuthorizationProvider();
        RegexPolicyRepresentation policy = representationFunction.apply(evaluation.getPolicy(), authorizationProvider);
        Attributes.Entry targetClaim = evaluation.getContext().getIdentity().getAttributes().getValue(policy.getTargetClaim());

        if (targetClaim == null) {
            return;
        }

        Pattern pattern = Pattern.compile(policy.getPattern());
        Matcher matcher = pattern.matcher(targetClaim.asString(0));
        if (matcher.matches()) {
            evaluation.grant();
        }
    }

}
