/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.oid4vc;

import java.util.List;

import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.keycloak.protocol.oidc.OIDCLoginProtocol.ATTEST_JWT_CLIENT_AUTH;

import static org.junit.jupiter.api.Assertions.assertFalse;


@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OIDCAuthorizationMetadataTest extends OID4VCIssuerTestBase {

    @Test
    public void testTokenEndpointAuthMethods() {
        OIDCConfigurationRepresentation oidcConfiguration = oauth.doWellKnownRequest();
        List<String> tokenAuthMethodsSupported = oidcConfiguration.getTokenEndpointAuthMethodsSupported();
        assertFalse(tokenAuthMethodsSupported.contains(ATTEST_JWT_CLIENT_AUTH), "Should not contain: " + ATTEST_JWT_CLIENT_AUTH);
    }
}
