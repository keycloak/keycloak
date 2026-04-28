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
import java.util.Map;

import org.keycloak.VCFormat;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCMdocTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCMdocTestBase.VCTestServerWithPreAuthCodeAndMdocEnabled.class)
public class OID4VCMdocAuthorizationDetailsFlowPreAuthTest extends OID4VCAuthorizationDetailsFlowPreAuthTestBase {

    private ClientScopeRepresentation mdocScope;

    @Override
    protected String getCredentialFormat() {
        return VCFormat.MSO_MDOC;
    }

    @Override
    protected ClientScopeRepresentation getCredentialClientScope() {
        if (mdocScope == null) {
            OID4VCMdocTestBase.ensureEcSigningKeyProvider(this, "mdoc-preauth-auth-details-issuer-key", "P-256", "ES256", 200);
            mdocScope = OID4VCMdocTestBase.createMdocCredentialScope(this, "mdoc-preauth-auth-details-scope", "mdoc-preauth-auth-details-config");
        }
        return mdocScope;
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
        assertNotNull(credentialObj, "Credential object should not be null");
        assertInstanceOf(String.class, credentialObj, "mDoc credential should be a string");

        String encodedIssuerSigned = (String) credentialObj;
        assertFalse(encodedIssuerSigned.isBlank(), "mDoc credential response must contain a base64url payload");

        Map<String, Object> nameSpaces = OID4VCMdocTestBase.getMdocNamespacesFromCredential(encodedIssuerSigned);
        assertTrue(nameSpaces.containsKey("org.iso.18013.5.1"), "mDoc payload must include the configured namespace");
        Map<?, ?> namespaceClaims = assertInstanceOf(Map.class, nameSpaces.get("org.iso.18013.5.1"));
        assertTrue(namespaceClaims.containsKey("given_name"), "mDoc payload must contain the given_name claim");
        assertTrue(namespaceClaims.containsKey("document_number"), "mDoc payload must contain the document_number claim");

        Map<String, Object> mobileSecurityObject = OID4VCMdocTestBase.getMdocMobileSecurityObjectFromCredential(encodedIssuerSigned);
        assertEquals(OID4VCMdocTestBase.mdocTypeCredentialDocType, mobileSecurityObject.get("docType"));
    }
}
