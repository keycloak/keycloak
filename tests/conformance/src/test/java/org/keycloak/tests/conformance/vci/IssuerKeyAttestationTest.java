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

package org.keycloak.tests.conformance.vci;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.conformance.runner.BrowserInteraction;
import org.keycloak.tests.conformance.runner.ConformanceModuleVariant;
import org.keycloak.tests.conformance.runner.ConformanceResult;

/**
 * Issues a credential whose configuration requires key attestations, so the suite includes a valid key attestation
 * that Keycloak must accept.
 */
@KeycloakIntegrationTest(config = IssuerKeyAttestationTest.KeyAttestationServerConfig.class)
public class IssuerKeyAttestationTest extends AbstractVciConformanceTest {

    @InjectRealm(config = KeyAttestationRequiredRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(
                "oid4vci-1_0-issuer-haip-test-plan",
                Map.of(
                        "credential_format", "sd_jwt_vc",
                        "vci_authorization_code_flow_variant", "wallet_initiated"),
                "oid4vci-1_0-issuer-happy-flow",
                ConformanceResult.PASSED,
                BrowserInteraction.LOGIN);
    }

    public static class KeyAttestationServerConfig extends VciConformanceRealmConfig.ServerConfig {

        // TODO Drop this truststore path once Keycloak trusts key attestation keys via a trust material identity
        // provider instead of validating the x5c chain against the server truststore.
        private static final String ATTESTER_CA_PATH = writeAttesterCaCertificate();

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return super.configure(config).option("truststore-paths", ATTESTER_CA_PATH);
        }

        private static String writeAttesterCaCertificate() {
            try {
                Path caPath = Files.createTempFile("oid4vci-conformance-attester-ca", ".pem");
                Files.writeString(caPath, VciAttesterKey.caCertificatePem(), StandardCharsets.UTF_8);
                caPath.toFile().deleteOnExit();
                return caPath.toString();
            } catch (IOException e) {
                throw new RuntimeException("Failed to write the attester CA certificate for the truststore", e);
            }
        }
    }

    public static class KeyAttestationRequiredRealmConfig extends VciConformanceRealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return super.configure(realm).update(rep -> rep.getClientScopes().stream()
                    .filter(scope -> SD_JWT_SCOPE.equals(scope.getName()))
                    .forEach(scope -> scope.getAttributes().put(CredentialScopeModel.VC_KEY_ATTESTATION_REQUIRED, "true")));
        }
    }
}
