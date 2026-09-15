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

package org.keycloak.tests.conformance.vci.nonhaip.mdoc;

import java.util.stream.Stream;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conformance.runner.BrowserInteraction;
import org.keycloak.testframework.conformance.runner.ConformanceModuleVariant;
import org.keycloak.testframework.conformance.runner.ConformanceResult;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.conformance.vci.nonhaip.AbstractNonHaipVciConformanceTest;
import org.keycloak.tests.conformance.vci.nonhaip.configs.MdocNonHaipVciRealmConfig;
import org.keycloak.tests.conformance.vci.nonhaip.configs.MdocNonHaipVciServerConfig;
import org.keycloak.tests.conformance.vci.nonhaip.issuer.IssuerHappyFlowTest;

import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.MDOC_CREDENTIAL_FORMAT_VARIANT;
import static org.keycloak.tests.conformance.vci.nonhaip.configs.NonHaipVciRealmConfig.NON_HAIP_PLAN;

/**
 * Runs the issuer happy flow with the ISO mdoc credential format, mirroring {@link IssuerHappyFlowTest} for SD-JWT VC.
 */
@KeycloakIntegrationTest(config = MdocNonHaipVciServerConfig.class)
public class IssuerMdocHappyFlowTest extends AbstractNonHaipVciConformanceTest {

    @InjectRealm(config = MdocNonHaipVciRealmConfig.class)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(
                NON_HAIP_PLAN,
                planVariant(MDOC_CREDENTIAL_FORMAT_VARIANT, "wallet_initiated"),
                "oid4vci-1_0-issuer-happy-flow",
                ConformanceResult.PASSED,
                BrowserInteraction.LOGIN);
    }
}
