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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.UserBuilder;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Super class for all OID4VC tests. Provides convenience methods to ease the testing.
 */
@EnableFeature(value = Profile.Feature.OID4VC_VCI, skipRestart = false)
public abstract class OID4VCTest extends AbstractTestRealmKeycloakTest {

    private static final Logger LOGGER = Logger.getLogger(OID4VCTest.class);
    protected static final String CONTEXT_URL = "https://www.w3.org/2018/credentials/v1";
    protected static final URI TEST_DID = URI.create("did:web:test.org");
    protected static final List<String> TEST_TYPES = List.of("VerifiableCredential");
    protected static final Date TEST_EXPIRATION_DATE = Date.from(Instant.ofEpochSecond(2000));
    protected static final Date TEST_ISSUANCE_DATE = Date.from(Instant.ofEpochSecond(1000));

    protected static final KeyWrapper RSA_KEY = getRsaKey();

    protected static CredentialSubject getCredentialSubject(Map<String, Object> claims) {
        CredentialSubject credentialSubject = new CredentialSubject();
        claims.forEach(credentialSubject::setClaims);
        return credentialSubject;
    }

    protected static VerifiableCredential getTestCredential(Map<String, Object> claims) {

        VerifiableCredential testCredential = new VerifiableCredential();
        testCredential.setId(URI.create(String.format("uri:uuid:%s", UUID.randomUUID())));
        testCredential.setContext(List.of(CONTEXT_URL));
        testCredential.setType(TEST_TYPES);
        testCredential.setIssuer(TEST_DID);
        testCredential.setExpirationDate(TEST_EXPIRATION_DATE);
        if (claims.containsKey("issuanceDate")) {
            testCredential.setIssuanceDate((Date) claims.get("issuanceDate"));
        }

        testCredential.setCredentialSubject(getCredentialSubject(claims));
        return testCredential;
    }


    public static KeyWrapper getECKey(String keyId) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
            kpg.initialize(256);
            var keyPair = kpg.generateKeyPair();
            KeyWrapper kw = new KeyWrapper();
            kw.setPrivateKey(keyPair.getPrivate());
            kw.setPublicKey(keyPair.getPublic());
            kw.setUse(KeyUse.SIG);
            kw.setKid(keyId);
            kw.setType("EC");
            kw.setAlgorithm("ES256");
            return kw;

        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyWrapper getEd25519Key(String keyId) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519", "BC");
            var keyPair = kpg.generateKeyPair();
            KeyWrapper kw = new KeyWrapper();
            kw.setPrivateKey(keyPair.getPrivate());
            kw.setPublicKey(keyPair.getPublic());
            kw.setUse(KeyUse.SIG);
            kw.setKid(keyId);
            kw.setType("Ed25519");
            return kw;

        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }


    public static KeyWrapper getRsaKey() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            var keyPair = kpg.generateKeyPair();
            KeyWrapper kw = new KeyWrapper();
            kw.setPrivateKey(keyPair.getPrivate());
            kw.setPublicKey(keyPair.getPublic());
            kw.setUse(KeyUse.SIG);
            kw.setKid(KeyUtils.createKeyId(keyPair.getPublic()));
            kw.setType("RSA");
            kw.setAlgorithm("RS256");
            return kw;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static ComponentExportRepresentation getRsaKeyProvider(KeyWrapper keyWrapper) {
        ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
        componentExportRepresentation.setName("rsa-key-provider");
        componentExportRepresentation.setId(UUID.randomUUID().toString());
        componentExportRepresentation.setProviderId("rsa");

        Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(
                new KeyPair((PublicKey) keyWrapper.getPublicKey(), (PrivateKey) keyWrapper.getPrivateKey()), "TestKey");

        componentExportRepresentation.setConfig(new MultivaluedHashMap<>(
                Map.of(
                        "privateKey", List.of(PemUtils.encodeKey(keyWrapper.getPrivateKey())),
                        "certificate", List.of(PemUtils.encodeCertificate(certificate)),
                        "active", List.of("true"),
                        "priority", List.of("0"),
                        "enabled", List.of("true"),
                        "algorithm", List.of("RS256")
                )
        ));
        return componentExportRepresentation;
    }

    public static ClientRepresentation getTestClient(String clientId) {
        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setClientId(clientId);
        clientRepresentation.setProtocol(OID4VCLoginProtocolFactory.PROTOCOL_ID);
        clientRepresentation.setEnabled(true);
        clientRepresentation.setAttributes(Map.of(
                "vc.test-credential.expiry_in_s", "100",
                "vc.test-credential.format", Format.JWT_VC.toString(),
                "vc.test-credential.scope", "VerifiableCredential"));
        clientRepresentation.setProtocolMappers(
                List.of(
                        getRoleMapper(clientId),
                        getEmailMapper(),
                        getIdMapper(),
                        getStaticClaimMapper("VerifiableCredential"),
                        getStaticClaimMapper("AnotherCredentialType")
                )
        );
        return clientRepresentation;
    }

    protected ComponentExportRepresentation getEdDSAKeyProvider() {
        ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
        componentExportRepresentation.setName("eddsa-generated");
        componentExportRepresentation.setId(UUID.randomUUID().toString());
        componentExportRepresentation.setName("eddsa-generated");
        componentExportRepresentation.setId(UUID.randomUUID().toString());
        componentExportRepresentation.setProviderId("eddsa-generated");

        componentExportRepresentation.setConfig(new MultivaluedHashMap<>(
                        Map.of(
                                "eddsaEllipticCurveKey", List.of("Ed25519"))
                )
        );
        return componentExportRepresentation;
    }

    public static ProtocolMapperRepresentation getRoleMapper(String clientId) {
        ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
        protocolMapperRepresentation.setName("role-mapper");
        protocolMapperRepresentation.setId(UUID.randomUUID().toString());
        protocolMapperRepresentation.setProtocol("oid4vc");
        protocolMapperRepresentation.setProtocolMapper("oid4vc-target-role-mapper");
        protocolMapperRepresentation.setConfig(
                Map.of(
                        "subjectProperty", "roles",
                        "clientId", clientId,
                        "supportedCredentialTypes", "VerifiableCredential")
        );
        return protocolMapperRepresentation;
    }

    public static ProtocolMapperRepresentation getEmailMapper() {
        ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
        protocolMapperRepresentation.setName("email-mapper");
        protocolMapperRepresentation.setProtocol("oid4vc");
        protocolMapperRepresentation.setId(UUID.randomUUID().toString());
        protocolMapperRepresentation.setProtocolMapper("oid4vc-user-attribute-mapper");
        protocolMapperRepresentation.setConfig(
                Map.of(
                        "subjectProperty", "email",
                        "userAttribute", "email",
                        "supportedCredentialTypes", "VerifiableCredential")
        );
        return protocolMapperRepresentation;
    }

    public static ProtocolMapperRepresentation getIdMapper() {
        ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
        protocolMapperRepresentation.setName("id-mapper");
        protocolMapperRepresentation.setProtocol("oid4vc");
        protocolMapperRepresentation.setId(UUID.randomUUID().toString());
        protocolMapperRepresentation.setProtocolMapper("oid4vc-subject-id-mapper");
        protocolMapperRepresentation.setConfig(
                Map.of(
                        "supportedCredentialTypes", "VerifiableCredential")
        );
        return protocolMapperRepresentation;
    }

    public static ProtocolMapperRepresentation getStaticClaimMapper(String supportedType) {
        ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
        protocolMapperRepresentation.setName(UUID.randomUUID().toString());
        protocolMapperRepresentation.setProtocol("oid4vc");
        protocolMapperRepresentation.setId(UUID.randomUUID().toString());
        protocolMapperRepresentation.setProtocolMapper("oid4vc-static-claim-mapper");
        protocolMapperRepresentation.setConfig(
                Map.of(
                        "subjectProperty", supportedType,
                        "staticValue", "true",
                        "supportedCredentialTypes", supportedType)
        );
        return protocolMapperRepresentation;
    }

    public static KeyWrapper getKeyFromSession(KeycloakSession keycloakSession) {
        // we only set one key to the realm, thus can just take the first one
        // if run inside the testsuite, configure is called separated from the test itself, thus we cannot just take
        // the key from the `configureTestRealm` method.
        KeyWrapper kw = keycloakSession
                .keys()
                .getKeysStream(keycloakSession.getContext().getRealm())
                .peek(k -> LOGGER.warnf("THE KEY: %s - %s", k.getKid(), k.getAlgorithm()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No key was configured"));
        LOGGER.warnf("Kid is %s", kw.getKid());
        return kw;
    }

    public static ComponentExportRepresentation getJwtSigningProvider(KeyWrapper keyWrapper) {
        ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
        componentExportRepresentation.setName("jwt-signing-service");
        componentExportRepresentation.setId(UUID.randomUUID().toString());
        componentExportRepresentation.setProviderId("jwt_vc");

        componentExportRepresentation.setConfig(new MultivaluedHashMap<>(
                Map.of(
                        "keyId", List.of(keyWrapper.getKid()),
                        "algorithmType", List.of(keyWrapper.getAlgorithm()),
                        "tokenType", List.of("JWT"),
                        "issuerDid", List.of(TEST_DID.toString())
                )
        ));
        return componentExportRepresentation;
    }

    public static UserRepresentation getUserRepresentation(Map<String, List<String>> clientRoles) {
        UserBuilder userBuilder = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("john")
                .enabled(true)
                .email("john@email.cz")
                .emailVerified(true)
                .firstName("John")
                .lastName("Doe")
                .password("password")
                .role("account", "manage-account")
                .role("account", "view-profile");

        clientRoles.entrySet().forEach(entry -> {
            entry.getValue().forEach(role -> userBuilder.role(entry.getKey(), role));
        });

        return userBuilder.build();
    }

    public static RoleRepresentation getRoleRepresentation(String roleName, String clientId) {

        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);
        role.setId(clientId);
        role.setClientRole(true);
        return role;
    }

    static class StaticTimeProvider implements TimeProvider {
        private final int currentTimeInS;

        StaticTimeProvider(int currentTimeInS) {
            this.currentTimeInS = currentTimeInS;
        }

        @Override
        public int currentTimeSeconds() {
            return currentTimeInS;
        }

        @Override
        public long currentTimeMillis() {
            return currentTimeInS * 1000L;
        }
    }

}