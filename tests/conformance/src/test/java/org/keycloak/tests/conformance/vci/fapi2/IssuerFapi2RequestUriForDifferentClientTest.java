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
 * One client pushes a request, then a different client presents the same request_uri at the authorization
 * endpoint: Keycloak must reject it because the pushed request is bound to the client that created it.
 */
@KeycloakIntegrationTest(config = VciConformanceRealmConfig.ServerConfig.class)
public class IssuerFapi2RequestUriForDifferentClientTest extends AbstractVciConformanceTest {

    @InjectRealm(config = VciConformanceRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(
                HAIP_PLAN,
                WALLET_INITIATED,
                "fapi2-security-profile-final-par-attempt-to-use-request_uri-for-different-client",
                ConformanceResult.REVIEW,
                // the second client's client_id does not match the one bound to the pushed request_uri
                BrowserInteraction.errorPage("Invalid Request"));
    }
}
