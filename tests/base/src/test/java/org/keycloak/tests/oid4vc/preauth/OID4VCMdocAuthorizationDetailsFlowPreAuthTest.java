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
package org.keycloak.tests.oid4vc.preauth;

import java.util.List;

import org.keycloak.VCFormat;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCMdocTestBase;

import static org.keycloak.tests.oid4vc.OID4VCMdocTestBase.assertMdocCredentialStructure;

@KeycloakIntegrationTest(config = OID4VCMdocTestBase.VCTestServerWithPreAuthCodeAndMdocEnabled.class)
public class OID4VCMdocAuthorizationDetailsFlowPreAuthTest extends OID4VCAuthorizationDetailsFlowPreAuthTestBase {

    @Override
    protected String getCredentialFormat() {
        return VCFormat.MSO_MDOC;
    }

    @Override
    protected ClientScopeRepresentation getCredentialClientScope() {
        ensureEcSigningKeyProvider("mdoc-preauth-auth-details-issuer-key", "P-256", "ES256", 200);
        return mdocTypeCredentialScope;
    }

    @Override
    protected List<Object> getExpectedClaimPath() {
        return List.of("org.iso.18013.5.1", "given_name");
    }

    @Override
    protected String getClaimsNamespace() {
        return "org.iso.18013.5.1";
    }

    @Override
    protected void verifyCredentialStructure(Object credentialObj) {
        assertMdocCredentialStructure(credentialObj);
    }
}
