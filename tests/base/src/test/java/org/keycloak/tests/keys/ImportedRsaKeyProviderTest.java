/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.keys;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.ImportedRsaEncKeyProviderFactory;
import org.keycloak.keys.ImportedRsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.saml.SamlConstants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class ImportedRsaKeyProviderTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oAuth;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Test
    public void privateKeyOnlyForSig() throws Exception {
        privateKeyOnly(ImportedRsaKeyProviderFactory.ID, KeyUse.SIG, Algorithm.RS256);
    }

    @Test
    public void privateKeyOnlyForEnc() throws Exception {
        privateKeyOnly(ImportedRsaEncKeyProviderFactory.ID, KeyUse.ENC, Algorithm.RSA_OAEP);
    }

    private void privateKeyOnly(String providerId, KeyUse keyUse, String algorithm) throws Exception {
        long priority = System.currentTimeMillis();

        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        String kid = KeyUtils.createKeyId(keyPair.getPublic());

        ComponentRepresentation rep = createRep("valid", providerId);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, Long.toString(priority));

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, createdRep.getConfig().getFirst(Attributes.PRIVATE_KEY_KEY));
        assertNotNull(createdRep.getConfig().getFirst(Attributes.CERTIFICATE_KEY));

        assertEquals(keyPair.getPublic(), PemUtils.decodeCertificate(createdRep.getConfig().getFirst(Attributes.CERTIFICATE_KEY)).getPublicKey());

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();

        assertEquals(kid, keys.getActive().get(algorithm));

        KeysMetadataRepresentation.KeyMetadataRepresentation key = keys.getKeys().get(0);

        assertEquals(id, key.getProviderId());
        assertEquals(AlgorithmType.RSA.name(), key.getType());
        assertEquals(priority, key.getProviderPriority());
        assertEquals(kid, key.getKid());
        assertEquals(PemUtils.encodeKey(keyPair.getPublic()), keys.getKeys().get(0).getPublicKey());
        assertEquals(keyPair.getPublic(), PemUtils.decodeCertificate(key.getCertificate()).getPublicKey());
        assertEquals(keyUse, keys.getKeys().get(0).getUse());
    }

    @Test
    public void keyAndCertificateForSig() throws Exception {
        keyAndCertificate(ImportedRsaKeyProviderFactory.ID, KeyUse.SIG);
    }

    @Test
    public void keyAndCertificateForEnc() throws Exception {
        keyAndCertificate(ImportedRsaEncKeyProviderFactory.ID, KeyUse.ENC);
    }

    private void keyAndCertificate(String providerId, KeyUse keyUse) throws Exception {
        long priority = System.currentTimeMillis();

        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "test");
        String certificatePem = PemUtils.encodeCertificate(certificate);

        ComponentRepresentation rep = createRep("valid", providerId);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));
        rep.getConfig().putSingle(Attributes.CERTIFICATE_KEY, certificatePem);
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, Long.toString(priority));

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, createdRep.getConfig().getFirst(Attributes.PRIVATE_KEY_KEY));
        assertEquals(certificatePem, createdRep.getConfig().getFirst(Attributes.CERTIFICATE_KEY));

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = keys.getKeys().get(0);
        assertEquals(certificatePem, key.getCertificate());
        assertEquals(keyUse, keys.getKeys().get(0).getUse());
    }

    @Test
    public void invalidPriorityForSig() throws Exception {
        invalidPriority(ImportedRsaKeyProviderFactory.ID);
    }

    @Test
    public void invalidPriorityForEnc() throws Exception {
        invalidPriority(ImportedRsaEncKeyProviderFactory.ID);
    }

    private void invalidPriority(String providerId) throws Exception {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);

        ComponentRepresentation rep = createRep("invalid", providerId);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, "invalid");

        Response response = realm.admin().components().add(rep);
        assertError(response, "'Priority' should be a number");
    }

    @Test
    public void invalidEnabledForSig() throws Exception {
        invalidEnabled(ImportedRsaKeyProviderFactory.ID);
    }

    @Test
    public void invalidEnabledForEnc() throws Exception {
        invalidEnabled(ImportedRsaEncKeyProviderFactory.ID);
    }

    private void invalidEnabled(String providerId) throws Exception {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);

        ComponentRepresentation rep = createRep("invalid", providerId);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));
        rep.getConfig().putSingle(Attributes.ENABLED_KEY, "invalid");

        Response response = realm.admin().components().add(rep);
        assertError(response, "'Enabled' should be 'true' or 'false'");
    }

    @Test
    public void invalidActiveForSig() throws Exception {
        invalidActive(ImportedRsaKeyProviderFactory.ID);
    }

    @Test
    public void invalidActiveForEnc() throws Exception {
        invalidActive(ImportedRsaEncKeyProviderFactory.ID);
    }

    private void invalidActive(String providerId) throws Exception {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);

        ComponentRepresentation rep = createRep("invalid", providerId);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));
        rep.getConfig().putSingle(Attributes.ACTIVE_KEY, "invalid");

        Response response = realm.admin().components().add(rep);
        assertError(response, "'Active' should be 'true' or 'false'");
    }

    @Test
    public void invalidPrivateKeyForSig() throws Exception {
        invalidPrivateKey(ImportedRsaKeyProviderFactory.ID);
    }

    @Test
    public void invalidPrivateKeyForEnc() throws Exception {
        invalidPrivateKey(ImportedRsaEncKeyProviderFactory.ID);
    }

    private void invalidPrivateKey(String providerId) throws Exception {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);

        ComponentRepresentation rep = createRep("invalid", providerId);

        Response response = realm.admin().components().add(rep);
        assertError(response, "'Private RSA Key' is required");

        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, "nonsense");
        response = realm.admin().components().add(rep);
        assertError(response, "Failed to decode private key");

        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPublic()));
        response = realm.admin().components().add(rep);
        assertError(response, "Failed to decode private key");
    }

    @Test
    public void invalidCertificateForSig() throws Exception {
        invalidCertificate(ImportedRsaKeyProviderFactory.ID);
    }

    @Test
    public void invalidCertificateForEnc() throws Exception {
        invalidCertificate(ImportedRsaEncKeyProviderFactory.ID);
    }

    @Test
    public void invalidExpiredCertificate() throws Exception {
        ComponentRepresentation rep = createRep("invalid", ImportedRsaEncKeyProviderFactory.ID);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, SamlConstants.SAML_CLIENT_SALES_POST_SIG_EXPIRED_PRIVATE_KEY);

        rep.getConfig().putSingle(Attributes.CERTIFICATE_KEY, SamlConstants.SAML_CLIENT_SALES_POST_SIG_EXPIRED_CERTIFICATE);
        Response response = realm.admin().components().add(rep);
        assertError(response, "Certificate is not valid");
    }


    @Test
    public void testExpiredCertificateInOneHour() {
        long priority = System.currentTimeMillis();

        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(
                keyPair, "test", new BigInteger("1"), Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));
        String certificatePem = PemUtils.encodeCertificate(certificate);

        ComponentRepresentation rep = createRep("valid", ImportedRsaKeyProviderFactory.ID);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));
        rep.getConfig().putSingle(Attributes.CERTIFICATE_KEY, certificatePem);
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, Long.toString(priority));

        String id;
        try (Response response = realm.admin().components().add(rep)) {
            id = ApiUtil.getCreatedId(response);
        }

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, createdRep.getConfig().getFirst(Attributes.PRIVATE_KEY_KEY));
        assertEquals(certificatePem, createdRep.getConfig().getFirst(Attributes.CERTIFICATE_KEY));

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = keys.getKeys().get(0);
        assertEquals(certificatePem, key.getCertificate());
        assertEquals(KeyUse.SIG, key.getUse());
        assertEquals(KeyStatus.ACTIVE.name(), key.getStatus());

        timeOffSet.set(3610);

        keys = realm.admin().keys().getKeyMetadata();
        key = keys.getKeys().get(0);
        assertEquals(KeyStatus.PASSIVE.name(), key.getStatus());
    }

    private void invalidCertificate(String providerId) throws Exception {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        Certificate invalidCertificate = CertificateUtils.generateV1SelfSignedCertificate(KeyUtils.generateRsaKeyPair(2048), "test");

        ComponentRepresentation rep = createRep("invalid", providerId);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));

        rep.getConfig().putSingle(Attributes.CERTIFICATE_KEY, "nonsense");
        Response response = realm.admin().components().add(rep);
        assertError(response, "Failed to decode certificate");

        rep.getConfig().putSingle(Attributes.CERTIFICATE_KEY, PemUtils.encodeCertificate(invalidCertificate));
        response = realm.admin().components().add(rep);
        assertError(response, "Certificate does not match private key");

    }

    protected void assertError(Response response, String error) {
        if (!response.hasEntity()) {
            fail("No error message set");
        }

        ErrorRepresentation errorRepresentation = response.readEntity(ErrorRepresentation.class);
        assertEquals(error, errorRepresentation.getErrorMessage());
        response.close();
    }

    protected ComponentRepresentation createRep(String name, String providerId) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(realm.admin().toRepresentation().getId());
        rep.setProviderId(providerId);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        return rep;
    }
}
