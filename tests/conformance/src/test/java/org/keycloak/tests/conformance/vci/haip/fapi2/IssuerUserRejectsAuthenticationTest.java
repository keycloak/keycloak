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

package org.keycloak.tests.conformance.vci.haip.fapi2;

import java.util.stream.Stream;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conformance.runner.BrowserInteraction;
import org.keycloak.testframework.conformance.runner.ConformanceModuleVariant;
import org.keycloak.testframework.conformance.runner.ConformanceResult;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.tests.conformance.vci.AbstractVciConformanceTest;
import org.keycloak.tests.conformance.vci.haip.configs.HaipVciRealmConfig;
import org.keycloak.tests.conformance.vci.haip.configs.HaipVciServerConfig;

import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CLIENT;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CLIENT2;
import static org.keycloak.tests.conformance.vci.haip.configs.HaipVciRealmConfig.HAIP_PLAN;

@KeycloakIntegrationTest(config = HaipVciServerConfig.class)
public class IssuerUserRejectsAuthenticationTest extends AbstractVciConformanceTest {

    @InjectRealm(config = ConsentRequiredRealmConfig.class)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(
                HAIP_PLAN,
                walletInitiated(),
                "fapi2-security-profile-final-user-rejects-authentication",
                ConformanceResult.PASSED,
                BrowserInteraction.DENY_CONSENT);
    }

    private static class ConsentRequiredRealmConfig extends HaipVciRealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return super.configure(realm).update(rep -> rep.getClients().stream()
                    .filter(client -> CLIENT.equals(client.getClientId()) || CLIENT2.equals(client.getClientId()))
                    .forEach(client -> client.setConsentRequired(true)));
        }
    }
}
