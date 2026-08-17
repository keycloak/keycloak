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

package org.keycloak.tests.conformance.vci.issuer;

import java.util.stream.Stream;

import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.tests.conformance.runner.BrowserInteraction;
import org.keycloak.tests.conformance.runner.ConformanceModuleVariant;
import org.keycloak.tests.conformance.runner.ConformanceResult;
import org.keycloak.tests.conformance.vci.AbstractVciConformanceTest;
import org.keycloak.tests.conformance.vci.VciConformanceRealmConfig;

/**
 * Issues a credential whose configuration requires key attestations, so the suite includes a valid key attestation
 * that Keycloak must accept. The attestation x5c chain is trusted through the {@code conformance-attester-x509}
 * trust-material identity provider configured by {@link VciConformanceRealmConfig}.
 */
@KeycloakIntegrationTest(config = VciConformanceRealmConfig.ServerConfig.class)
public class IssuerKeyAttestationTest extends AbstractVciConformanceTest {

    @InjectRealm(config = KeyAttestationRequiredRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(
                HAIP_PLAN,
                WALLET_INITIATED,
                "oid4vci-1_0-issuer-happy-flow",
                ConformanceResult.PASSED,
                BrowserInteraction.LOGIN);
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
