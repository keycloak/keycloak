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

package org.keycloak.tests.conformance.vci.nonhaip.issuer;

import java.util.stream.Stream;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conformance.runner.BrowserInteraction;
import org.keycloak.testframework.conformance.runner.ConformanceModuleVariant;
import org.keycloak.testframework.conformance.runner.ConformanceResult;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.conformance.vci.nonhaip.AbstractNonHaipVciConformanceTest;
import org.keycloak.tests.conformance.vci.nonhaip.NonHaipVciConformanceRealmConfig;

import static org.keycloak.tests.conformance.vci.nonhaip.NonHaipVciConformanceRealmConfig.NON_HAIP_PLAN;

/**
 * The issuer_initiated flow: Keycloak creates a credential offer through the verifiable_credential_offer
 * application initiated action and the suite receives it, fetches the offer from its credential_offer_uri and
 * completes the authorization code flow with its own wallet client.
 */
@KeycloakIntegrationTest(config = NonHaipVciConformanceRealmConfig.ServerConfig.class)
public class IssuerInitiatedHappyFlowTest extends AbstractNonHaipVciConformanceTest {

    @InjectRealm(config = NonHaipVciConformanceRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(
                NON_HAIP_PLAN,
                issuerInitiated(),
                "oid4vci-1_0-issuer-happy-flow",
                ConformanceResult.PASSED,
                BrowserInteraction.LOGIN)
                // TODO (#50889): include the encrypted variant once Keycloak keeps the credential offer state for the
                //  lifetime of the authorized session. Keycloak removes the offer state after the first
                //  successful issuance (OID4VCIssuerEndpoint), so the encrypted variant's second credential
                //  request (encryption + DEFLATE compression check) fails with "No credential offer state".
                //  The generic non-HAIP plan does not pin credential encryption, so the discovered variant may
                //  omit the key entirely (defaults to plain); exclude only the explicit encrypted variant.
                .filter(module -> !"encrypted".equals(module.moduleVariant().get("vci_credential_encryption")));
    }
}
