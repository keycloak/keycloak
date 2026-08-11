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

package org.keycloak.tests.conformance.vp;

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.conformance.runner.ConformanceModuleVariant;

/**
 * The verifier rejects a key binding JWT whose sd_hash does not match the presentation.
 */
@KeycloakIntegrationTest(config = VpConformanceRealmConfig.ServerConfig.class)
public class VerifierInvalidSdHashTest extends AbstractVpConformanceTest {

    @InjectRealm(config = VpConformanceRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(
                "oid4vp-1final-verifier-test-plan",
                Map.of(
                        "vp_profile", "plain_vp",
                        "credential_format", "sd_jwt_vc",
                        "client_id_prefix", "x509_hash",
                        "request_method", "request_uri_signed",
                        "response_mode", "direct_post"),
                "oid4vp-1final-verifier-invalid-sd-hash");
    }
}
