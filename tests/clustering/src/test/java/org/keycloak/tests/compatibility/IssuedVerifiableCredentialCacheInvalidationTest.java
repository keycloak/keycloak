/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.compatibility;

import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.testframework.annotations.InjectLoadBalancer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.clustering.LoadBalancer;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.keycloak.testframework.remote.providers.runonserver.IssuedVerifiableCredentialTestHelper.add;
import static org.keycloak.testframework.remote.providers.runonserver.IssuedVerifiableCredentialTestHelper.count;
import static org.keycloak.testframework.remote.providers.runonserver.IssuedVerifiableCredentialTestHelper.removeExpiredAndCountRemaining;
import static org.keycloak.testframework.remote.providers.runonserver.IssuedVerifiableCredentialTestHelper.removeUserCredential;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = IssuedVerifiableCredentialCacheInvalidationTest.OID4VCServerConfig.class)
public class IssuedVerifiableCredentialCacheInvalidationTest {

    private static final String CREDENTIAL_SCOPE = "clustered-credential";

    @InjectRealm(config = OID4VCRealmConfig.class)
    ManagedRealm realm;

    @InjectUser
    ManagedUser user;

    @InjectLoadBalancer
    LoadBalancer loadBalancer;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectTestDatabase(lifecycle = LifeCycle.CLASS)
    TestDatabase database;

    @AfterEach
    public void cleanup() {
        loadBalancer.node(0);
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1, 0"})
    public void testUserScopedInvalidation(int writer, int reader) {
        String userId = user.getId();
        String scopeId = getCredentialScopeId();

        loadBalancer.node(writer);
        runOnServer.run(add(userId, scopeId, null));

        loadBalancer.node(reader);
        assertIssuedCredentialCount(userId, 1);

        loadBalancer.node(writer);
        assertTrue(runOnServer.fetch(removeUserCredential(userId, scopeId), Boolean.class));

        loadBalancer.node(reader);
        assertIssuedCredentialCount(userId, 0);
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1, 0"})
    public void testGlobalInvalidation(int writer, int reader) {
        String userId = user.getId();
        String scopeId = getCredentialScopeId();

        loadBalancer.node(writer);
        runOnServer.run(add(userId, scopeId, Time.currentTimeMillis() - 1));

        assertIssuedCredentialCount(userId, 1);

        loadBalancer.node(reader);
        assertIssuedCredentialCount(userId, 1);

        loadBalancer.node(writer);
        assertEquals(0L, runOnServer.fetch(removeExpiredAndCountRemaining(userId), Long.class));

        loadBalancer.node(reader);
        assertIssuedCredentialCount(userId, 0);

        loadBalancer.node(writer);
        assertEquals(0L, runOnServer.fetch(removeExpiredAndCountRemaining(userId), Long.class));
        assertTrue(runOnServer.fetch(removeUserCredential(userId, scopeId), Boolean.class));
    }

    private String getCredentialScopeId() {
        return realm.admin().clientScopes().findAll().stream()
                .filter(scope -> CREDENTIAL_SCOPE.equals(scope.getName()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private void assertIssuedCredentialCount(String userId, long expected) {
        assertEquals(expected, runOnServer.fetch(count(userId), Long.class));
    }

    public static class OID4VCServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI);
        }
    }

    public static class OID4VCRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.verifiableCredentialsEnabled(true)
                    .clientScopes(new CredentialScopeRepresentation(CREDENTIAL_SCOPE)
                            .setCredentialConfigurationId(CREDENTIAL_SCOPE));
        }
    }
}
