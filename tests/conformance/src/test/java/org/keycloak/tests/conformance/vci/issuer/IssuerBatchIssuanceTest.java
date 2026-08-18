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

import org.keycloak.constants.OID4VCIConstants;
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

@KeycloakIntegrationTest(config = VciConformanceRealmConfig.ServerConfig.class)
public class IssuerBatchIssuanceTest extends AbstractVciConformanceTest {

    @InjectRealm(config = BatchIssuanceRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(
                HAIP_PLAN,
                WALLET_INITIATED,
                "oid4vci-1_0-issuer-batch-issuance",
                ConformanceResult.PASSED,
                BrowserInteraction.LOGIN);
    }

    public static class BatchIssuanceRealmConfig extends VciConformanceRealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return super.configure(realm).attribute(OID4VCIConstants.BATCH_CREDENTIAL_ISSUANCE_BATCH_SIZE, "20");
        }
    }
}
