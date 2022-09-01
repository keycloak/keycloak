/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.admin.client.authorization;

import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

import static org.keycloak.common.Profile.Feature.AUTHORIZATION;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class EnforcerConfigTest extends AbstractKeycloakTest {

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadRealm(getClass().getResourceAsStream("/authorization-test/test-authz-realm.json"));
        testRealms.add(realm);
    }

    @Test
    public void testMultiplePathsWithSameName() {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getClass().getResourceAsStream("/authorization-test/enforcer-config-paths-same-name.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        Map<String, PolicyEnforcerConfig.PathConfig> paths = policyEnforcer.getPaths();
        assertEquals(1, paths.size());
        assertEquals(4, paths.values().iterator().next().getMethods().size());
    }

    @Test
    public void testPathConfigClaimInformationPoint() {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getClass().getResourceAsStream("/authorization-test/enforcer-config-path-cip.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        Map<String, PolicyEnforcerConfig.PathConfig> paths = policyEnforcer.getPaths();

        assertEquals(1, paths.size());

        PathConfig pathConfig = paths.values().iterator().next();
        Map<String, Map<String, Object>> cipConfig = pathConfig.getClaimInformationPointConfig();

        assertEquals(1, cipConfig.size());

        Map<String, Object> claims = cipConfig.get("claims");

        assertNotNull(claims);

        assertEquals(3, claims.size());
        assertEquals("{request.parameter['a']}", claims.get("claim-a"));
        assertEquals("{request.header['b']}", claims.get("claim-b"));
        assertEquals("{request.cookie['c']}", claims.get("claim-c"));
    }
}
