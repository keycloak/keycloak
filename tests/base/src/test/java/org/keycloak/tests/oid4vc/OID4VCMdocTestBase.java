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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.VCFormat;
import org.keycloak.common.Profile;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.mdoc.MdocIssuerSignedDocument;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCMapper;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import static org.keycloak.OID4VCConstants.CRYPTOGRAPHIC_BINDING_METHOD_COSE_KEY;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BINDING_REQUIRED;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BINDING_REQUIRED_PROOF_TYPES;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CRYPTOGRAPHIC_BINDING_METHODS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class OID4VCMdocTestBase extends OID4VCIssuerTestBase {

    public static class VCTestServerWithMdocEnabled implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI, Profile.Feature.OID4VC_VCI_MDOC);
        }
    }

    public static class VCTestServerWithPreAuthCodeAndMdocEnabled implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI, Profile.Feature.OID4VC_VCI_REST_CREDENTIAL_OFFER, Profile.Feature.OID4VC_VCI_PREAUTH_CODE, Profile.Feature.OID4VC_VCI_MDOC);
        }
    }

    protected CredentialScopeRepresentation createMdocCredentialScope(String scopeName, String credentialConfigurationId) {
        return createMdocCredentialScope(this, scopeName, credentialConfigurationId);
    }

    public static CredentialScopeRepresentation createMdocCredentialScope(OID4VCIssuerTestBase base, String scopeName, String credentialConfigurationId) {
        CredentialScopeRepresentation existingScope = base.getCredentialScope(scopeName);
        if (existingScope != null) {
            addScopeToOid4vciClients(base, existingScope.getId());
            addCredentialToTestUser(base, scopeName);
            return existingScope;
        }

        return createCustomMdocCredentialScope(
                base,
                scopeName,
                credentialConfigurationId,
                List.of(
                        ProtocolMapperUtils.getUserAttributeMapper("given_name", "firstName", "org.iso.18013.5.1"),
                        ProtocolMapperUtils.getUserAttributeMapper("family_name", "lastName", "org.iso.18013.5.1"),
                        ProtocolMapperUtils.getSubjectIdMapper("id", UserModel.DID, "org.iso.18013.5.1")
                ),
                "ES256",
                true
        );
    }

    protected CredentialScopeRepresentation createCustomMdocCredentialScope(String scopeName,
                                                                            String credentialConfigurationId,
                                                                            List<ProtocolMapperRepresentation> protocolMappers) {
        return createCustomMdocCredentialScope(this, scopeName, credentialConfigurationId, protocolMappers, "ES256", true);
    }

    protected CredentialScopeRepresentation createCustomMdocCredentialScope(String scopeName,
                                                                            String credentialConfigurationId,
                                                                            List<ProtocolMapperRepresentation> protocolMappers,
                                                                            String signingAlg,
                                                                            boolean bindingRequired) {
        return createCustomMdocCredentialScope(this, scopeName, credentialConfigurationId, protocolMappers, signingAlg, bindingRequired);
    }

    public static CredentialScopeRepresentation createCustomMdocCredentialScope(OID4VCIssuerTestBase base,
                                                                                String scopeName,
                                                                                String credentialConfigurationId,
                                                                                List<ProtocolMapperRepresentation> protocolMappers,
                                                                                String signingAlg,
                                                                                boolean bindingRequired) {
        CredentialScopeRepresentation existingScope = base.getCredentialScope(scopeName);
        if (existingScope != null) {
            addScopeToOid4vciClients(base, existingScope.getId());
            addCredentialToTestUser(base, scopeName);
            return existingScope;
        }

        CredentialScopeRepresentation scope = new CredentialScopeRepresentation(scopeName)
                .setIncludeInTokenScope(true)
                .setExpiryInSeconds(15)
                .setCredentialConfigurationId(credentialConfigurationId)
                .setCredentialIdentifier(scopeName)
                .setFormat(VCFormat.MSO_MDOC)
                .setVct(mdocTypeCredentialDocType);

        if (signingAlg != null) {
            scope.setSigningAlg(signingAlg);
        }
        if (bindingRequired) {
            scope.setCryptographicBindingMethods(List.of(CRYPTOGRAPHIC_BINDING_METHOD_COSE_KEY));
        }
        scope.setProtocolMappers(protocolMappers);

        Map<String, String> attrs = scope.getAttributes() != null ? scope.getAttributes() : new HashMap<>();
        attrs.put(VC_BINDING_REQUIRED, String.valueOf(bindingRequired));
        if (bindingRequired) {
            attrs.put(VC_BINDING_REQUIRED_PROOF_TYPES, "jwt");
            attrs.put(VC_CRYPTOGRAPHIC_BINDING_METHODS, CRYPTOGRAPHIC_BINDING_METHOD_COSE_KEY);
        } else {
            attrs.remove(VC_BINDING_REQUIRED_PROOF_TYPES);
            attrs.remove(VC_CRYPTOGRAPHIC_BINDING_METHODS);
        }
        scope.setAttributes(attrs);

        String scopeId;
        try (Response response = base.testRealm.admin().clientScopes().create(scope)) {
            scopeId = ApiUtil.getCreatedId(response);
        }
        base.testRealm.cleanup().add(realm -> realm.clientScopes().get(scopeId).remove());

        addScopeToOid4vciClients(base, scopeId);
        addCredentialToTestUser(base, scopeName);
        return new CredentialScopeRepresentation(base.testRealm.admin().clientScopes().get(scopeId).toRepresentation());
    }

    protected void addScopeToOid4vciClients(String scopeId) {
        addScopeToOid4vciClients(this, scopeId);
    }

    private static void addScopeToOid4vciClients(OID4VCIssuerTestBase base, String scopeId) {
        base.testRealm.admin().clients().get(base.client.getId()).addOptionalClientScope(scopeId);
        base.testRealm.admin().clients().get(base.pubClient.getId()).addOptionalClientScope(scopeId);
    }

    private static void addCredentialToTestUser(OID4VCIssuerTestBase base, String scopeName) {
        String userId = base.requireExistingUser(TEST_USER).getId();
        boolean alreadyPresent = base.testRealm.admin().users().get(userId).verifiableCredentials().getCredentials().stream()
                .anyMatch(credential -> scopeName.equals(credential.getCredentialScopeName()));
        if (!alreadyPresent) {
            UserVerifiableCredentialRepresentation credential = new UserVerifiableCredentialRepresentation();
            credential.setCredentialScopeName(scopeName);
            base.testRealm.admin().users().get(userId).verifiableCredentials().createCredential(credential);
        }
    }

    protected MdocIssuerSignedDocument parseMdocCredential(String encodedIssuerSigned) {
        return parseCredential(encodedIssuerSigned);
    }

    protected Map<String, Object> getMdocNamespaces(String encodedIssuerSigned) {
        return getMdocNamespacesFromCredential(encodedIssuerSigned);
    }

    protected Map<String, Object> getMdocMobileSecurityObject(String encodedIssuerSigned) {
        return getMdocMobileSecurityObjectFromCredential(encodedIssuerSigned);
    }

    public static Map<String, Object> getMdocNamespacesFromCredential(String encodedIssuerSigned) {
        return parseCredential(encodedIssuerSigned).getNamespaces();
    }

    public static Map<String, Object> getMdocMobileSecurityObjectFromCredential(String encodedIssuerSigned) {
        return parseCredential(encodedIssuerSigned).getMobileSecurityObject();
    }

    public static void assertMdocCredentialStructure(Object credentialObj) {
        assertNotNull(credentialObj, "mDoc credential object should not be null");
        assertInstanceOf(String.class, credentialObj, "mDoc credential should be a string");

        String encodedIssuerSigned = (String) credentialObj;
        assertFalse(encodedIssuerSigned.isBlank(), "mDoc credential response must contain a base64url payload");

        Map<String, Object> nameSpaces = getMdocNamespacesFromCredential(encodedIssuerSigned);
        assertTrue(nameSpaces.containsKey("org.iso.18013.5.1"), "mDoc payload must include the configured namespace");
        Map<?, ?> namespaceClaims = assertInstanceOf(Map.class, nameSpaces.get("org.iso.18013.5.1"));
        assertTrue(namespaceClaims.containsKey("given_name"), "mDoc payload must contain the given_name claim");
        assertTrue(namespaceClaims.containsKey("id"), "mDoc payload must contain the id claim");

        Map<String, Object> mobileSecurityObject = getMdocMobileSecurityObjectFromCredential(encodedIssuerSigned);
        assertEquals(mdocTypeCredentialDocType, mobileSecurityObject.get("docType"));
    }

    protected static void assertValidMdocCredential(CredentialResponse credentialResponse, boolean expectDeviceKeyInfo) {
        assertNotNull(credentialResponse);
        assertNotNull(credentialResponse.getCredentials());
        assertEquals(1, credentialResponse.getCredentials().size());

        Object credential = credentialResponse.getCredentials().get(0).getCredential();
        assertInstanceOf(String.class, credential, "mDoc credential should be a string");

        Map<String, Object> nameSpaces = getMdocNamespacesFromCredential((String) credential);
        assertTrue(nameSpaces.containsKey("org.iso.18013.5.1"));

        Map<String, Object> mobileSecurityObject = getMdocMobileSecurityObjectFromCredential((String) credential);
        assertEquals(mdocTypeCredentialDocType, mobileSecurityObject.get("docType"));
        if (expectDeviceKeyInfo) {
            assertNotNull(mobileSecurityObject.get("deviceKeyInfo"));
        } else {
            assertNull(mobileSecurityObject.get("deviceKeyInfo"));
        }
    }

    public static class ProtocolMapperUtils extends OID4VCIssuerTestBase.ProtocolMapperUtils {

        static ProtocolMapperRepresentation getSubjectIdMapper(String subjectProperty, String attributeName, String mdocNamespace) {
            ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
            protocolMapperRepresentation.setName(attributeName + "-mapper");
            protocolMapperRepresentation.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
            protocolMapperRepresentation.setId(UUID.randomUUID().toString());
            protocolMapperRepresentation.setProtocolMapper("oid4vc-subject-id-mapper");
            Map<String, String> config = new HashMap<>();
            config.put(OID4VCMapper.CLAIM_NAME, subjectProperty);
            config.put(OID4VCMapper.USER_ATTRIBUTE_KEY, attributeName);
            config.put(OID4VCMapper.MDOC_NAMESPACE, mdocNamespace);
            protocolMapperRepresentation.setConfig(config);
            return protocolMapperRepresentation;
        }

        static ProtocolMapperRepresentation getUserAttributeMapper(String subjectProperty, String attributeName, String mdocNamespace) {
            ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
            protocolMapperRepresentation.setName(attributeName + "-mapper");
            protocolMapperRepresentation.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
            protocolMapperRepresentation.setId(UUID.randomUUID().toString());
            protocolMapperRepresentation.setProtocolMapper("oid4vc-user-attribute-mapper");
            Map<String, String> config = new HashMap<>();
            config.put(OID4VCMapper.CLAIM_NAME, subjectProperty);
            config.put(OID4VCMapper.USER_ATTRIBUTE_KEY, attributeName);
            config.put(OID4VCMapper.MDOC_NAMESPACE, mdocNamespace);
            protocolMapperRepresentation.setConfig(config);
            return protocolMapperRepresentation;
        }
    }

    private static MdocIssuerSignedDocument parseCredential(String encodedIssuerSigned) {
        return MdocIssuerSignedDocument.parse(encodedIssuerSigned);
    }

}
