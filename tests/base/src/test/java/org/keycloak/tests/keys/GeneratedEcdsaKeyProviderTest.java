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

import java.security.KeyFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyType;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class GeneratedEcdsaKeyProviderTest {

    private static final String DEFAULT_EC = GeneratedEcdsaKeyProviderFactory.DEFAULT_ECDSA_ELLIPTIC_CURVE;
    private static final String ECDSA_ELLIPTIC_CURVE_KEY = GeneratedEcdsaKeyProviderFactory.ECDSA_ELLIPTIC_CURVE_KEY;
    private static final String TEST_REALM_NAME = "test";

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void defaultEc() {
        supportedEc(null, true);
    }

    @Test
    public void defaultEcWithCertificate() {
        supportedEc(null, false);
    }

    @Test
    public void supportedEcP521() {
        supportedEc("P-521", false);
    }

    @Test
    public void supportedEcP521WithCertificate() {
        supportedEc("P-521", true);
    }

    @Test
    public void supportedEcP384() {
        supportedEc("P-384", false);
    }

    @Test
    public void supportedEcP384WithCertificate() {
        supportedEc("P-384", true);
    }

    @Test
    public void supportedEcP256() {
        supportedEc("P-256", false);
    }

    @Test
    public void supportedEcP256AndCertificate() {
        supportedEc("P-256", true);
    }

    @Test
    public void unsupportedEcK163() {
        // NIST.FIPS.186-4 Koblitz Curve over Binary Field
        unsupportedEc("K-163");
    }

    private String supportedEc(String ecInNistRep, boolean withCertificate) {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", GeneratedEcdsaKeyProviderFactory.ID);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, Long.toString(priority));
        if (ecInNistRep != null) {
            rep.getConfig().putSingle(ECDSA_ELLIPTIC_CURVE_KEY, ecInNistRep);
        }
        else {
            ecInNistRep = DEFAULT_EC;
        }
        if (withCertificate) {
            rep.getConfig().putSingle(Attributes.EC_GENERATE_CERTIFICATE_KEY, "true");
        }

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        realm.cleanup().add(r -> r.components().component(id).remove());
        response.close();

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();

        // stands for the number of properties in the key provider config
        assertEquals(withCertificate ? 3 : 2, createdRep.getConfig().size());
        assertEquals(Long.toString(priority), createdRep.getConfig().getFirst(Attributes.PRIORITY_KEY));
        assertEquals(ecInNistRep, createdRep.getConfig().getFirst(ECDSA_ELLIPTIC_CURVE_KEY));
        if (withCertificate) {
            assertNotNull(createdRep.getConfig().getFirst(Attributes.EC_GENERATE_CERTIFICATE_KEY));
        }

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = null;

        for (KeyMetadataRepresentation k : keys.getKeys()) {
           if (KeyType.EC.equals(k.getType()) && id.equals(k.getProviderId())) {
                key = k;
                break;
           }
        }
        assertNotNull(key);

        assertEquals(id, key.getProviderId());
        assertEquals(KeyType.EC, key.getType());
        assertEquals(priority, key.getProviderPriority());
        if (withCertificate) {
            assertNotNull(key.getCertificate());
            X509Certificate certificate = PemUtils.decodeCertificate(key.getCertificate());
            final String expectedIssuerAndSubject = "CN=" + realm.getName();
            assertEquals(expectedIssuerAndSubject, certificate.getIssuerX500Principal().getName());
            assertEquals(expectedIssuerAndSubject, certificate.getSubjectX500Principal().getName());
        }

        return id; // created key's component id
    }

    private void unsupportedEc(String ecInNistRep) {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", GeneratedEcdsaKeyProviderFactory.ID);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));
        rep.getConfig().putSingle(ECDSA_ELLIPTIC_CURVE_KEY, ecInNistRep);

        Response response = realm.admin().components().add(rep);
        response.close();
        Assertions.assertEquals(400, response.getStatus());
    }

    @Test
    public void changeCurveFromP256ToP384() throws Exception {
        changeCurve("P-256", "P-384");
    }

    @Test
    public void changeCurveFromP384ToP521() throws Exception  {
        changeCurve("P-384", "P-521");
    }

    @Test
    public void changeCurveFromP521ToP256() throws Exception  {
        changeCurve("P-521", "P-256");
    }

    private void changeCurve(String FromEcInNistRep, String ToEcInNistRep) throws Exception {
        String keyComponentId = supportedEc(FromEcInNistRep, false);
        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();
        KeysMetadataRepresentation.KeyMetadataRepresentation originalKey = null;
        for (KeyMetadataRepresentation k : keys.getKeys()) {
           if (KeyType.EC.equals(k.getType()) && keyComponentId.equals(k.getProviderId())) {
                originalKey = k;
                break;
           }
        }

        ComponentRepresentation createdRep = realm.admin().components().component(keyComponentId).toRepresentation();
        createdRep.getConfig().putSingle(ECDSA_ELLIPTIC_CURVE_KEY, ToEcInNistRep);
        realm.admin().components().component(keyComponentId).update(createdRep);

        createdRep = realm.admin().components().component(keyComponentId).toRepresentation();

        // stands for the number of properties in the key provider config
        assertEquals(2, createdRep.getConfig().size());
        assertEquals(ToEcInNistRep, createdRep.getConfig().getFirst(ECDSA_ELLIPTIC_CURVE_KEY));

        keys = realm.admin().keys().getKeyMetadata();
        KeysMetadataRepresentation.KeyMetadataRepresentation key = null;
        for (KeyMetadataRepresentation k : keys.getKeys()) {
           if (KeyType.EC.equals(k.getType()) && keyComponentId.equals(k.getProviderId())) {
                key = k;
                break;
           }
        }
        assertNotNull(key);

        assertEquals(keyComponentId, key.getProviderId());
        assertNotEquals(originalKey.getKid(), key.getKid());  // kid is changed if key was regenerated
        assertEquals(KeyType.EC, key.getType());
        assertNotEquals(originalKey.getAlgorithm(), key.getAlgorithm());
        assertEquals(ToEcInNistRep, GeneratedEcdsaKeyProviderFactory.convertJWSAlgorithmToECDomainParmNistRep(key.getAlgorithm()));
        assertEquals(ToEcInNistRep, getCurveFromPublicKey(key.getPublicKey()));
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

    private String getCurveFromPublicKey(String publicEcdsaKeyBase64Encoded) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicEcdsaKeyBase64Encoded));
        ECPublicKey ecKey = (ECPublicKey) kf.generatePublic(publicKeySpec);
        return "P-" + ecKey.getParams().getCurve().getField().getFieldSize();
    }
}
