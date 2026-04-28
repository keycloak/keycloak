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
package org.keycloak.tests.oid4vc;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.VCFormat;
import org.keycloak.crypto.Algorithm;
import org.keycloak.mdoc.MdocAlgorithm;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.SupportedProofTypeData;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.CRYPTOGRAPHIC_BINDING_METHOD_COSE_KEY;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCMdocTestBase.VCTestServerWithMdocEnabled.class)
public class OID4VCMdocIssuerWellKnownProviderTest extends OID4VCMdocTestBase {

    private CredentialScopeRepresentation mdocScope;

    @BeforeEach
    void setUpMdocScope() {
        ensureEcSigningKeyProvider("mdoc-issuer-key", "P-256", "ES256", 200);
        mdocScope = createMdocCredentialScope("mdoc-feature-on-scope", "mdoc-feature-on-config");
    }

    @Test
    void testMdocCredentialConfigurationMetadata() {
        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        SupportedCredentialConfiguration supportedConfig =
                credentialIssuer.getCredentialsSupported().get(mdocScope.getCredentialConfigurationId());

        assertNotNull(supportedConfig, "mso_mdoc configuration must be advertised when the mDoc feature is enabled");
        assertEquals(VCFormat.MSO_MDOC, supportedConfig.getFormat());
        assertEquals(mdocTypeCredentialDocType, supportedConfig.getDocType());
        assertEquals(List.of(CRYPTOGRAPHIC_BINDING_METHOD_COSE_KEY), supportedConfig.getCryptographicBindingMethodsSupported());
        assertEquals(List.of(-7), supportedConfig.getCredentialSigningAlgValuesSupported());
        assertNotNull(supportedConfig.getProofTypesSupported());

        SupportedProofTypeData jwtProofSupport =
                supportedConfig.getProofTypesSupported().getSupportedProofTypes().get(ProofType.JWT);
        assertNotNull(jwtProofSupport, "JWT proof support must be advertised for mDoc binding");
        assertEquals(Set.copyOf(getAllAsymmetricAlgorithms()), Set.copyOf(jwtProofSupport.getSigningAlgorithmsSupported()));

        assertHasClaimPath(supportedConfig, List.of("org.iso.18013.5.1", "given_name"));
        assertHasClaimPath(supportedConfig, List.of("org.iso.18013.5.1", "family_name"));
        assertHasClaimPath(supportedConfig, List.of("org.iso.18013.5.1", "document_number"));
    }

    @Test
    void testMdocCredentialConfigurationSupportsNestedClaimPaths() {
        ProtocolMapperRepresentation nestedAddressMapper =
                ProtocolMapperUtils.getUserAttributeMapper("address.street", "address_street_address", "org.iso.18013.5.1");
        nestedAddressMapper.getConfig().put(CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true");
        CredentialScopeRepresentation scope =
                createCustomMdocCredentialScope("mdoc-nested-claim-scope", "mdoc-nested-claim-config", List.of(nestedAddressMapper));

        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        SupportedCredentialConfiguration supportedConfig =
                credentialIssuer.getCredentialsSupported().get(scope.getCredentialConfigurationId());

        assertHasClaimPath(supportedConfig, List.of("org.iso.18013.5.1", "address", "street"));
    }

    @Test
    void testMdocCredentialConfigurationUsesScopeNameAsDoctypeFallback() {
        CredentialScopeRepresentation scope = createMdocCredentialScope("mdoc-doctype-fallback-scope", "mdoc-doctype-fallback-config");
        scope.setVct(null);
        updateCredentialScope(scope);

        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        SupportedCredentialConfiguration supportedConfig =
                credentialIssuer.getCredentialsSupported().get(scope.getCredentialConfigurationId());

        assertNotNull(supportedConfig);
        assertEquals(scope.getName(), supportedConfig.getDocType());
    }

    @Test
    void testMdocCredentialConfigurationUsesConfiguredSigningAlgMetadata() {
        ensureEcSigningKeyProvider("mdoc-issuer-key-es384", "P-384", "ES384", 210);
        CredentialScopeRepresentation scope = createMdocCredentialScope("mdoc-es384-scope", "mdoc-es384-config");
        scope.setSigningAlg("ES384");
        updateCredentialScope(scope);

        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        SupportedCredentialConfiguration supportedConfig =
                credentialIssuer.getCredentialsSupported().get(scope.getCredentialConfigurationId());

        assertEquals(List.of(MdocAlgorithm.ES384.getCoseAlgorithmIdentifier()),
                supportedConfig.getCredentialSigningAlgValuesSupported());
    }

    @Test
    void testMdocCredentialConfigurationUsesSupportedRealmSigningAlgsByDefault() {
        CredentialScopeRepresentation scope = createCustomMdocCredentialScope(
                "mdoc-default-signing-scope",
                "mdoc-default-signing-config",
                List.of(ProtocolMapperUtils.getUserAttributeMapper("given_name", "firstName", "org.iso.18013.5.1")),
                null,
                true
        );

        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        SupportedCredentialConfiguration supportedConfig =
                credentialIssuer.getCredentialsSupported().get(scope.getCredentialConfigurationId());

        Set<Integer> expectedSigningAlgs = Arrays.stream(MdocAlgorithm.values())
                .map(MdocAlgorithm::getCoseAlgorithmIdentifier)
                .collect(Collectors.toSet());
        assertEquals(expectedSigningAlgs, Set.copyOf(supportedConfig.getCredentialSigningAlgValuesSupported()));
    }

    @Test
    void testMdocCredentialConfigurationOmitsBindingMetadataWhenBindingIsOptional() {
        CredentialScopeRepresentation scope = createCustomMdocCredentialScope(
                "mdoc-optional-binding-scope",
                "mdoc-optional-binding-config",
                List.of(ProtocolMapperUtils.getUserAttributeMapper("given_name", "firstName", "org.iso.18013.5.1")),
                "ES256",
                false
        );

        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        SupportedCredentialConfiguration supportedConfig =
                credentialIssuer.getCredentialsSupported().get(scope.getCredentialConfigurationId());

        assertNotNull(supportedConfig, "mDoc configuration should be advertised");
        assertEquals(VCFormat.MSO_MDOC, supportedConfig.getFormat());
        assertEquals(List.of(MdocAlgorithm.ES256.getCoseAlgorithmIdentifier()),
                supportedConfig.getCredentialSigningAlgValuesSupported());
        assertNull(supportedConfig.getCryptographicBindingMethodsSupported());
        assertNull(supportedConfig.getProofTypesSupported());
    }

    private static void assertHasClaimPath(SupportedCredentialConfiguration supportedConfig, List<String> expectedPath) {
        assertNotNull(supportedConfig.getCredentialMetadata());
        assertNotNull(supportedConfig.getCredentialMetadata().getClaims());
        List<List<String>> actualPaths = supportedConfig.getCredentialMetadata().getClaims().stream()
                .map(Claim::getPath)
                .toList();
        assertTrue(actualPaths.stream().anyMatch(expectedPath::equals),
                "Missing mDoc claim path " + expectedPath + " in " + actualPaths);
    }

    private static List<String> getAllAsymmetricAlgorithms() {
        return List.of(
                Algorithm.PS256, Algorithm.PS384, Algorithm.PS512,
                Algorithm.RS256, Algorithm.RS384, Algorithm.RS512,
                Algorithm.ES256, Algorithm.ES384, Algorithm.ES512,
                Algorithm.EdDSA
        );
    }
}
