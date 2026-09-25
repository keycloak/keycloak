/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.authz;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;

import org.junit.BeforeClass;

import static org.keycloak.common.Profile.Feature.AUTHORIZATION;

public abstract class AbstractAuthzTest extends AbstractKeycloakTest {

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    protected AccessToken toAccessToken(String rpt) {
        try {
            return new JWSInput(rpt).readJsonContent(AccessToken.class);
        } catch (JWSInputException cause) {
            throw new RuntimeException("Failed to deserialize RPT", cause);
        }
    }

    protected PolicyRepresentation createAlwaysGrantPolicy(AuthorizationResource authorization) {
        PolicyRepresentation policy = new PolicyRepresentation();
        policy.setName(KeycloakModelUtils.generateId());
        policy.setType("always-grant");
        authorization.policies().create(policy).close();
        return policy;
    }

    protected PolicyRepresentation createAlwaysDenyPolicy(AuthorizationResource authorization) {
        PolicyRepresentation policy = new PolicyRepresentation();
        policy.setName(KeycloakModelUtils.generateId());
        policy.setType("always-deny");
        authorization.policies().create(policy).close();
        return policy;
    }

    protected PolicyRepresentation createOnlyOwnerPolicy(AuthorizationResource authorization) {
        PolicyRepresentation onlyOwnerPolicy = new PolicyRepresentation();

        onlyOwnerPolicy.setName(KeycloakModelUtils.generateId());
        onlyOwnerPolicy.setType("allow-resource-owner");

        authorization.policies().create(onlyOwnerPolicy).close();

        return onlyOwnerPolicy;
    }
}
