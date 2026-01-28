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

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.attribute.Attributes;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.representations.idm.authorization.RegexPolicyRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

import static org.keycloak.utils.JsonUtils.getJsonValue;
import static org.keycloak.utils.JsonUtils.hasPath;
import static org.keycloak.utils.JsonUtils.splitClaimPath;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class RegexPolicyProvider implements PolicyProvider {

    private static final Logger logger = Logger.getLogger(RegexPolicyProvider.class);
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
        String value = getClaimValue(evaluation, policy);

        if (value == null) {
            return;
        }

        Pattern pattern = Pattern.compile(policy.getPattern());
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            evaluation.grant();
            logger.debugf("policy %s evaluated with status %s on identity %s and claim value %s", policy.getName(), evaluation.getEffect(), evaluation.getContext().getIdentity().getId(), getClaimValue(evaluation, policy));
        }
    }

    private String getClaimValue(Evaluation evaluation, RegexPolicyRepresentation policy) {
        Attributes attributes = policy.isTargetContextAttributes()
                ? evaluation.getContext().getAttributes()
                : evaluation.getContext().getIdentity().getAttributes();
        String targetClaim = policy.getTargetClaim();

        try {
            if (hasPath(targetClaim)) {
                return resolveJsonValue(attributes, targetClaim);
            }

            return resolveSimpleValue(attributes, targetClaim);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to resolve value from claim: " + targetClaim, cause);
        }
    }

    private String resolveSimpleValue(Attributes attributes, String targetClaim) {
        Attributes.Entry value = attributes.getValue(targetClaim);

        if (value == null || value.isEmpty()) {
            return null;
        }

        return value.asString(0);
    }

    private String resolveJsonValue(Attributes attributes, String targetClaim) throws IOException {
        List<String> paths = splitClaimPath(targetClaim);

        if (paths.isEmpty()) {
            return null;
        }

        Attributes.Entry attribute = attributes.getValue(paths.get(0));

        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        JsonNode node = JsonSerialization.readValue(attribute.asString(0), JsonNode.class);
        String path = String.join(".", paths.subList(1, paths.size()));

        return Optional.ofNullable(getJsonValue(node, path)).map(Object::toString).orElse(null);
    }
}
