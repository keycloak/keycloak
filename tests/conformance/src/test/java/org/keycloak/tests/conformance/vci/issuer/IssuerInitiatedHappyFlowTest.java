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

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.conformance.runner.BrowserInteraction;
import org.keycloak.tests.conformance.runner.ConformanceModuleVariant;
import org.keycloak.tests.conformance.runner.ConformanceResult;
import org.keycloak.tests.conformance.vci.AbstractVciConformanceTest;
import org.keycloak.tests.conformance.vci.VciConformanceRealmConfig;

/**
 * The issuer_initiated flow: Keycloak creates a credential offer through the verifiable_credential_offer
 * application initiated action and the suite receives it, fetches the offer from its credential_offer_uri and
 * completes the authorization code flow with its own wallet client.
 */
@KeycloakIntegrationTest(config = VciConformanceRealmConfig.ServerConfig.class)
public class IssuerInitiatedHappyFlowTest extends AbstractVciConformanceTest {

    @InjectRealm(config = VciConformanceRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(
                HAIP_PLAN,
                ISSUER_INITIATED,
                "oid4vci-1_0-issuer-happy-flow",
                ConformanceResult.PASSED,
                BrowserInteraction.LOGIN)
                // TODO: include the encrypted variant once Keycloak keeps the credential offer state for the
                //  lifetime of the authorized session. Keycloak removes the offer state after the first
                //  successful issuance (OID4VCIssuerEndpoint), so the encrypted variant's second credential
                //  request (encryption + DEFLATE compression check) fails with "No credential offer state".
                .filter(module -> "plain".equals(module.moduleVariant().get("vci_credential_encryption")));
    }
}
