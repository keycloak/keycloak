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

package org.keycloak.testsuite.oid4vc.issuance.signing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.models.oid4vci.Oid4vcProtocolMapperModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCMapper;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.protocol.oid4vc.model.ClaimDisplay;
import org.keycloak.protocol.oid4vc.model.Claims;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.ProofTypesSupported;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OID4VCIssuerWellKnownProviderTest extends OID4VCIssuerEndpointTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        Map<String, String> attributes = Optional.ofNullable(testRealm.getAttributes()).orElseGet(HashMap::new);
        attributes.put("credential_response_encryption.alg_values_supported", "[\"RSA-OAEP\"]");
        attributes.put("credential_response_encryption.enc_values_supported", "[\"A256GCM\"]");
        attributes.put("credential_response_encryption.encryption_required", "true");
        attributes.put("batch_credential_issuance.batch_size", "10");
        attributes.put("signed_metadata", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJmb28iOiJiYXIifQ.XYZ123abc");
        testRealm.setAttributes(attributes);
        super.configureTestRealm(testRealm);
    }

    /**
     * this test will use the configured scopes {@link #jwtTypeCredentialClientScope} and
     * {@link #sdJwtTypeCredentialClientScope} to verify that the metadata endpoint is presenting the expected data
     */
    @Test
    public void testMetaDataEndpointIsCorrectlySetup() {
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();

        Assert.assertEquals(getRealmPath(TEST_REALM_NAME), credentialIssuer.getCredentialIssuer());
        Assert.assertEquals(getBasePath(TEST_REALM_NAME) + OID4VCIssuerEndpoint.CREDENTIAL_PATH,
                            credentialIssuer.getCredentialEndpoint());
        Assert.assertNull("Display was not configured", credentialIssuer.getDisplay());
        Assert.assertEquals("Authorization Server should have the realm-address.",
                            1,
                            credentialIssuer.getAuthorizationServers().size());
        Assert.assertEquals("Authorization Server should point to the realm-address.",
                            getRealmPath(TEST_REALM_NAME),
                            credentialIssuer.getAuthorizationServers().get(0));

        // Check credential_response_encryption
        CredentialIssuer.CredentialResponseEncryption encryption = credentialIssuer.getCredentialResponseEncryption();
        Assert.assertNotNull("credential_response_encryption should be present", encryption);
        Assert.assertEquals(List.of("RSA-OAEP"), encryption.getAlgValuesSupported());
        Assert.assertEquals(List.of("A256GCM"), encryption.getEncValuesSupported());
        Assert.assertTrue("encryption_required should be true", encryption.getEncryptionRequired());

        // Check batch_credential_issuance
        CredentialIssuer.BatchCredentialIssuance batch = credentialIssuer.getBatchCredentialIssuance();
        Assert.assertNotNull("batch_credential_issuance should be present", batch);
        Assert.assertEquals(Integer.valueOf(10), batch.getBatchSize());

        // Check signed_metadata
        Assert.assertEquals(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJmb28iOiJiYXIifQ.XYZ123abc",
                credentialIssuer.getSignedMetadata()
        );

        for (ClientScopeRepresentation clientScope : List.of(jwtTypeCredentialClientScope,
                                                             sdJwtTypeCredentialClientScope,
                                                             minimalJwtTypeCredentialClientScope)) {
            compareMetadataToClientScope(credentialIssuer, clientScope);
        }
    }

    /**
     * this test will make sure that the default values are correctly added into the metadata endpoint
     */
    @Test
    public void testMinimalJwtCredentialHardcodedTest()
    {
        ClientScopeRepresentation clientScope = minimalJwtTypeCredentialClientScope;
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        SupportedCredentialConfiguration supportedConfig = credentialIssuer.getCredentialsSupported()
                                                                           .get(clientScope.getName());
        Assert.assertNotNull(supportedConfig);
        Assert.assertEquals(Format.SD_JWT_VC, supportedConfig.getFormat());
        Assert.assertEquals(clientScope.getName(), supportedConfig.getScope());
        Assert.assertEquals(1, supportedConfig.getCredentialDefinition().getType().size());
        Assert.assertEquals(clientScope.getName(), supportedConfig.getCredentialDefinition().getType().get(0));
        Assert.assertEquals(1, supportedConfig.getCredentialDefinition().getContext().size());
        Assert.assertEquals(clientScope.getName(), supportedConfig.getCredentialDefinition().getContext().get(0));
        Assert.assertNull(supportedConfig.getDisplay());
        Assert.assertEquals(clientScope.getName(), supportedConfig.getScope());

        compareClaims(supportedConfig.getFormat(), supportedConfig.getClaims(), clientScope.getProtocolMappers());
    }

    private void compareMetadataToClientScope(CredentialIssuer credentialIssuer, ClientScopeRepresentation clientScope) {
        String credentialConfigurationId = Optional.ofNullable(clientScope.getAttributes()
                                                                          .get(CredentialScopeModel.CONFIGURATION_ID))
                                                   .orElse(clientScope.getName());
        SupportedCredentialConfiguration supportedConfig = credentialIssuer.getCredentialsSupported()
                                                                           .get(credentialConfigurationId);
        Assert.assertNotNull("Configuration of type '" + credentialConfigurationId + "' must be present",
                             supportedConfig);
        Assert.assertEquals(credentialConfigurationId, supportedConfig.getId());

        String expectedFormat = Optional.ofNullable(clientScope.getAttributes().get(CredentialScopeModel.FORMAT))
                                        .orElse(Format.SD_JWT_VC);
        Assert.assertEquals(expectedFormat, supportedConfig.getFormat());

        Assert.assertEquals(clientScope.getName(), supportedConfig.getScope());
        {
            // TODO this is still hardcoded
            Assert.assertEquals(1, supportedConfig.getCryptographicBindingMethodsSupported().size());
            Assert.assertEquals(CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT,
                                supportedConfig.getCryptographicBindingMethodsSupported().get(0));
        }

        compareDisplay(supportedConfig, clientScope);

        String expectedVct = Optional.ofNullable(clientScope.getAttributes().get(CredentialScopeModel.VCT))
                                     .orElse(clientScope.getName());
        Assert.assertEquals(expectedVct, supportedConfig.getVct());

        Assert.assertNotNull(supportedConfig.getCredentialDefinition());
        Assert.assertNotNull(supportedConfig.getCredentialDefinition().getType());
        List<String> credentialDefinitionTypes = Optional.ofNullable(clientScope.getAttributes()
                                                                                .get(CredentialScopeModel.TYPES))
                                                         .map(s -> s.split(","))
                                                         .map(Arrays::asList)
                                                         .orElseGet(() -> List.of(clientScope.getName()));
        Assert.assertEquals(credentialDefinitionTypes.size(),
                            supportedConfig.getCredentialDefinition().getType().size());

        MatcherAssert.assertThat(supportedConfig.getCredentialDefinition().getContext(),
                                 Matchers.containsInAnyOrder(credentialDefinitionTypes.toArray()));
        List<String> credentialDefinitionContexts = Optional.ofNullable(clientScope.getAttributes()
                                                                                   .get(CredentialScopeModel.CONTEXTS))
                                                            .map(s -> s.split(","))
                                                            .map(Arrays::asList)
                                                            .orElseGet(() -> List.of(clientScope.getName()));
        Assert.assertEquals(credentialDefinitionContexts.size(),
                            supportedConfig.getCredentialDefinition().getContext().size());
        MatcherAssert.assertThat(supportedConfig.getCredentialDefinition().getContext(),
                                 Matchers.containsInAnyOrder(credentialDefinitionTypes.toArray()));

        List<String> signingAlgsSupported = new ArrayList<>(supportedConfig.getCredentialSigningAlgValuesSupported());
        String proofTypesSupportedString = supportedConfig.getProofTypesSupported().toJsonString();

        try {
            withCausePropagation(() -> testingClient.server(TEST_REALM_NAME).run((session -> {
                ProofTypesSupported expectedProofTypesSupported = ProofTypesSupported.parse(session,
                                                                                            List.of(Algorithm.RS256));
                Assert.assertEquals(expectedProofTypesSupported,
                                    ProofTypesSupported.fromJsonString(proofTypesSupportedString));

                List<String> expectedSigningAlgs = OID4VCIssuerWellKnownProvider.getSupportedSignatureAlgorithms(session);
                MatcherAssert.assertThat(signingAlgsSupported,
                                         Matchers.containsInAnyOrder(expectedSigningAlgs.toArray()));

            })));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        compareClaims(expectedFormat, supportedConfig.getClaims(), clientScope.getProtocolMappers());
    }

    private void compareDisplay(SupportedCredentialConfiguration supportedConfig, ClientScopeRepresentation clientScope) {
        String display = clientScope.getAttributes().get(CredentialScopeModel.VC_DISPLAY);
        if (StringUtil.isBlank(display)) {
            Assert.assertNull(supportedConfig.getDisplay());
            return;
        }
        List<DisplayObject> expectedDisplayObjectList;
        try {
            expectedDisplayObjectList = JsonSerialization.mapper.readValue(display, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Assert.assertEquals(expectedDisplayObjectList.size(), supportedConfig.getDisplay().size());
        MatcherAssert.assertThat("Must contain all expected display-objects",
                                 supportedConfig.getDisplay(),
                                 Matchers.containsInAnyOrder(expectedDisplayObjectList.toArray()));
    }

    /**
     * each claim representation from the metadata is based on a protocol-mapper which we compare here
     */
    private void compareClaims(String credentialFormat,
                               Claims originalClaims,
                               List<ProtocolMapperRepresentation> originalProtocolMappers) {
        // the data must be serializable to transfer them to the server, so we convert the data to strings
        String claimsString = originalClaims.toJsonString();
        String protocolMappersString = toJsonString(originalProtocolMappers);

        try {
            withCausePropagation(() -> testingClient.server(TEST_REALM_NAME).run((session -> {
                Claims actualClaims = fromJsonString(claimsString, Claims.class);
                List<ProtocolMapperRepresentation> protocolMappers = fromJsonString(protocolMappersString,
                                                                                    new SerializableProtocolMapperReference());
                // check only protocol-mappers of type oid4vc
                protocolMappers = protocolMappers.stream().filter(protocolMapper -> {
                    return OID4VCLoginProtocolFactory.PROTOCOL_ID.equals(protocolMapper.getProtocol());
                }).toList();

                for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
                    OID4VCMapper mapper = (OID4VCMapper) session.getProvider(ProtocolMapper.class,
                                                                             protocolMapper.getProtocolMapper());
                    ProtocolMapperModel protocolMapperModel = new ProtocolMapperModel();
                    protocolMapperModel.setConfig(protocolMapper.getConfig());
                    mapper.setMapperModel(protocolMapperModel, credentialFormat);
                    Claim claim = actualClaims.stream()
                                              .filter(c -> c.getPath().equals(mapper.getMetadataAttributePath()))
                                              .findFirst().orElse(null);
                    if (mapper.includeInMetadata()) {
                        Assert.assertNotNull("There should be a claim matching the protocol-mappers config!", claim);
                    }
                    else {
                        Assert.assertNull("This claim should not be included in the metadata-config!", claim);
                        // no other checks to do for this claim
                        continue;
                    }
                    Assert.assertEquals(claim.isMandatory(),
                                        Optional.ofNullable(protocolMapper.getConfig()
                                                                          .get(Oid4vcProtocolMapperModel.MANDATORY))
                                                .map(Boolean::parseBoolean)
                                                .orElse(false));
                    String expectedDisplayString = protocolMapper.getConfig().get(Oid4vcProtocolMapperModel.DISPLAY);
                    List<ClaimDisplay> expectedDisplayList = fromJsonString(expectedDisplayString,
                                                                            new SerializableClaimDisplayReference());
                    List<ClaimDisplay> actualDisplayList = claim.getDisplay();
                    if (expectedDisplayList == null) {
                        Assert.assertNull(actualDisplayList);
                    }
                    else {
                        Assert.assertEquals(expectedDisplayList.size(), actualDisplayList.size());
                        MatcherAssert.assertThat(actualDisplayList,
                                                 Matchers.containsInAnyOrder(expectedDisplayList.toArray()));
                    }
                }
            })));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * a jackson type-reference that can be used in the run-server-block
     */
    public static class SerializableProtocolMapperReference extends TypeReference<List<ProtocolMapperRepresentation>>
            implements Serializable {
    }

    /**
     * a jackson type-reference that can be used in the run-server-block
     */
    public static class SerializableClaimDisplayReference extends TypeReference<List<ClaimDisplay>>
            implements Serializable {
    }

    public static void testCredentialConfig(SuiteContext suiteContext, KeycloakTestingClient testingClient) {
        String expectedIssuer = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + TEST_REALM_NAME;
        String expectedCredentialsEndpoint = expectedIssuer + "/protocol/oid4vc/credential";
        String expectedDeferredEndpoint = expectedIssuer + "/protocol/oid4vc/deferred_credential";
        final String expectedAuthorizationServer = expectedIssuer;
        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    OID4VCIssuerWellKnownProvider oid4VCIssuerWellKnownProvider = new OID4VCIssuerWellKnownProvider(session);
                    Object issuerConfig = oid4VCIssuerWellKnownProvider.getConfig();
                    Assert.assertTrue("Valid credential-issuer metadata should be returned.", issuerConfig instanceof CredentialIssuer);
                    CredentialIssuer credentialIssuer = (CredentialIssuer) issuerConfig;
                    Assert.assertEquals("The correct issuer should be included.", expectedIssuer, credentialIssuer.getCredentialIssuer());
                    Assert.assertEquals("The correct credentials endpoint should be included.", expectedCredentialsEndpoint, credentialIssuer.getCredentialEndpoint());
                    Assert.assertEquals("The correct deferred_credential_endpoint should be included.", expectedDeferredEndpoint, credentialIssuer.getDeferredCredentialEndpoint());
                    Assert.assertEquals("Since the authorization server is equal to the issuer, just 1 should be returned.", 1, credentialIssuer.getAuthorizationServers().size());
                    Assert.assertEquals("The expected server should have been returned.", expectedAuthorizationServer, credentialIssuer.getAuthorizationServers().get(0));
                    Assert.assertTrue("The test-credential should be supported.", credentialIssuer.getCredentialsSupported().containsKey("test-credential"));
                    Assert.assertEquals("The test-credential should offer type VerifiableCredential", "VerifiableCredential", credentialIssuer.getCredentialsSupported().get("test-credential").getScope());
                    Assert.assertEquals("The test-credential should be offered in the jwt-vc format.", Format.JWT_VC, credentialIssuer.getCredentialsSupported().get("test-credential").getFormat());
                    Assert.assertNotNull("The test-credential can optionally provide a claims claim.", credentialIssuer.getCredentialsSupported().get("test-credential").getClaims());
                }));
    }

    public static void extendConfigureTestRealm(RealmRepresentation testRealm, ClientRepresentation clientRepresentation) {
        if (testRealm.getComponents() == null) {
            testRealm.setComponents(new MultivaluedHashMap<>());
        }

        testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getRsaKeyProvider(RSA_KEY));
        testRealm.getComponents().add("org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder", getCredentialBuilderProvider(Format.JWT_VC));


        if (testRealm.getClients() != null) {
            testRealm.getClients().add(clientRepresentation);
        } else {
            testRealm.setClients(new ArrayList<>(List.of(clientRepresentation)));
        }

        if (testRealm.getUsers() != null) {
            testRealm.getUsers().add(OID4VCTest.getUserRepresentation(Map.of(clientRepresentation.getClientId(), List.of("testRole"))));
        } else {
            testRealm.setUsers(new ArrayList<>(List.of(OID4VCTest.getUserRepresentation(Map.of(clientRepresentation.getClientId(), List.of("testRole"))))));
        }

        if (testRealm.getAttributes() != null) {
            testRealm.getAttributes().put("issuerDid", TEST_DID.toString());
        } else {
            testRealm.setAttributes(new HashMap<>(Map.of("issuerDid", TEST_DID.toString())));
        }
    }
}
