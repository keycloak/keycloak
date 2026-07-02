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

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.conformance.vci.VciConformanceRealmConfig;

import org.junit.jupiter.api.Disabled;

// TODO: enable once https://github.com/keycloak/keycloak/issues/48188 is fixed. The REST endpoint binds the
//  offer to the client of the access token used to create it, and redeeming an offer with a different client is
//  rejected ("Unexpected login client"). The suite redeems with its own wallet client, so the offer must be
//  created without a target client, which only the AIA path (see IssuerInitiatedHappyFlowTest) supports today.
@Disabled("The create-credential-offer REST endpoint binds the offer to the creating client, see the TODO")
@KeycloakIntegrationTest(config = VciConformanceRealmConfig.ServerConfig.class)
public class IssuerInitiatedRestOfferTest extends IssuerInitiatedHappyFlowTest {

    @Override
    protected String createCredentialOfferUri() {
        throw new UnsupportedOperationException(
                "Create the offer via POST /realms/" + VciConformanceRealmConfig.REALM
                        + "/protocol/oid4vc/create-credential-offer once offers are no longer bound to the creating client");
    }
}
