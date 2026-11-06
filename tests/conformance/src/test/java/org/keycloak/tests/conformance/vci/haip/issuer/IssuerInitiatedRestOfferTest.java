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

package org.keycloak.tests.conformance.vci.haip.issuer;

import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conformance.runner.BrowserInteraction;
import org.keycloak.testframework.conformance.runner.ConformanceModuleVariant;
import org.keycloak.testframework.conformance.runner.ConformanceResult;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.conformance.vci.AbstractVciConformanceTest;
import org.keycloak.tests.conformance.vci.VciRestCredentialOffer;
import org.keycloak.tests.conformance.vci.haip.configs.HaipVciRealmConfig;
import org.keycloak.tests.conformance.vci.haip.configs.HaipVciServerConfig;

import static org.keycloak.tests.conformance.vci.haip.configs.HaipVciRealmConfig.HAIP_PLAN;

/**
 * Runs the issuer_initiated flow with an offer from the REST API instead of the application initiated action.
 */
@KeycloakIntegrationTest(config = IssuerInitiatedRestOfferTest.RestOfferServerConfig.class)
public class IssuerInitiatedRestOfferTest extends AbstractVciConformanceTest {

    @InjectRealm(config = RestOfferRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = VciRestCredentialOffer.IssuerAppClient.class)
    OAuthClient oauth;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(
                HAIP_PLAN,
                issuerInitiated(),
                "oid4vci-1_0-issuer-happy-flow",
                ConformanceResult.PASSED,
                BrowserInteraction.LOGIN);
    }

    @Override
    protected String createCredentialOfferUri() {
        return VciRestCredentialOffer.createOfferUri(oauth);
    }

    public static class RestOfferServerConfig extends HaipVciServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return super.configure(config).features(Profile.Feature.OID4VC_VCI_REST_CREDENTIAL_OFFER);
        }
    }

    public static class RestOfferRealmConfig extends HaipVciRealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return VciRestCredentialOffer.allowHolderToCreateOffers(super.configure(realm));
        }
    }
}
