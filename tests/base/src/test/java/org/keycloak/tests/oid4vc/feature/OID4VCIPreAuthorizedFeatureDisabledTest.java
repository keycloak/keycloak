/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.oid4vc.feature;


import org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCMinimalServerConfig.class)
public class OID4VCIPreAuthorizedFeatureDisabledTest extends OID4VCIssuerTestBase {

    @Test
    public void testPreAuthorizedGrantTypeNotAvailable() throws Exception {

        OIDCConfigurationRepresentation oidcConfig = oauth.doWellKnownRequest();

        assertNotNull(oidcConfig.getTokenEndpoint(), "A token endpoint should be included.");
        assertFalse(oidcConfig.getGrantTypesSupported().contains(PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE), "The pre-authorized code should not be supported.");
    }
}
