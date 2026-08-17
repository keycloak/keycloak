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

package org.keycloak.tests.conformance.vci.fapi2;

import java.util.List;
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
 * Sends authorization requests with missing or plain PKCE: Keycloak rejects them at the authorization endpoint
 * and redirects the error to the callback without ever presenting a login, where the module validates it.
 */
@KeycloakIntegrationTest(config = VciConformanceRealmConfig.ServerConfig.class)
public class IssuerFapi2PkceRejectedTest extends AbstractVciConformanceTest {

    private static final List<String> MODULES = List.of(
            "fapi2-security-profile-final-par-ensure-pkce-required",
            "fapi2-security-profile-final-par-plain-pkce-rejected");

    @InjectRealm(config = VciConformanceRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(HAIP_PLAN, WALLET_INITIATED, MODULES,
                ConformanceResult.PASSED, BrowserInteraction.ERROR_CALLBACK);
    }
}
