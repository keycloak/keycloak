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

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.models.ParConfig;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.tests.conformance.runner.BrowserInteraction;
import org.keycloak.tests.conformance.runner.ConformanceModuleVariant;
import org.keycloak.tests.conformance.runner.ConformanceResult;

@KeycloakIntegrationTest(config = VciConformanceRealmConfig.ServerConfig.class)
public class IssuerExpiredRequestUriTest extends AbstractVciConformanceTest {

    @InjectRealm(config = ShortParRequestUriLifespanRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(
                "oid4vci-1_0-issuer-haip-test-plan",
                Map.of(
                        "credential_format", "sd_jwt_vc",
                        "vci_authorization_code_flow_variant", "wallet_initiated"),
                "fapi2-security-profile-final-par-attempt-to-use-expired-request_uri",
                ConformanceResult.REVIEW,
                BrowserInteraction.errorPage("Invalid Request"));
    }

    public static class ShortParRequestUriLifespanRealmConfig extends VciConformanceRealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return super.configure(realm).attribute(ParConfig.PAR_REQUEST_URI_LIFESPAN, "15");
        }
    }
}
