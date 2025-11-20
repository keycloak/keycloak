/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedEcdhKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class GeneratedEcdhKeyProviderTest {

    private static final String DEFAULT_EC = GeneratedEcdhKeyProviderFactory.DEFAULT_ECDH_ELLIPTIC_CURVE;
    private static final String ECDH_ELLIPTIC_CURVE_KEY = GeneratedEcdhKeyProviderFactory.ECDH_ELLIPTIC_CURVE_KEY;
    private static final String ECDH_ALGORITHM_KEY = GeneratedEcdhKeyProviderFactory.ECDH_ALGORITHM_KEY;

    @InjectRealm
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    public void defaultEcDirect() {
        supportedEc(null, Algorithm.ECDH_ES);
    }

    @Test
    public void supportedEcP521Direct() {
        supportedEc("P-521", Algorithm.ECDH_ES);
    }

    @Test
    public void supportedEcP384Direct() {
        supportedEc("P-384", Algorithm.ECDH_ES);
    }

    @Test
    public void supportedEcP256Direct() {
        supportedEc("P-256", Algorithm.ECDH_ES);
    }

    @Test
    public void unsupportedEcK163Direct() {
        // NIST.FIPS.186-4 Koblitz Curve over Binary Field
        unsupportedEc("K-163", Algorithm.ECDH_ES);
    }

    @Test
    public void defaultEcKeyWrap128() {
        supportedEc(null, Algorithm.ECDH_ES_A128KW);
    }

    @Test
    public void defaultEcKeyWrap192() {
        supportedEc(null, Algorithm.ECDH_ES_A192KW);
    }

    @Test
    public void defaultEcKeyWrap256() {
        supportedEc(null, Algorithm.ECDH_ES_A256KW);
    }

    @Test
    public void supportedEcP521KeyWrap128() {
        supportedEc("P-521", Algorithm.ECDH_ES_A128KW);
    }

    @Test
    public void supportedEcP521KeyWrap192() {
        supportedEc("P-521", Algorithm.ECDH_ES_A192KW);
    }

    @Test
    public void supportedEcP521KeyWrap256() {
        supportedEc("P-521", Algorithm.ECDH_ES_A256KW);
    }

    @Test
    public void supportedEcP384KeyWrap128() {
        supportedEc("P-384", Algorithm.ECDH_ES_A128KW);
    }

    @Test
    public void supportedEcP384KeyWrap192() {
        supportedEc("P-384", Algorithm.ECDH_ES_A192KW);
    }

    @Test
    public void supportedEcP384KeyWrap256() {
        supportedEc("P-384", Algorithm.ECDH_ES_A256KW);
    }

    @Test
    public void supportedEcP256KeyWrap128() {
        supportedEc("P-256", Algorithm.ECDH_ES_A128KW);
    }

    @Test
    public void supportedEcP256KeyWrap192() {
        supportedEc("P-256", Algorithm.ECDH_ES_A192KW);
    }

    @Test
    public void supportedEcP256KeyWrap256() {
        supportedEc("P-256", Algorithm.ECDH_ES_A256KW);
    }

    @Test
    public void unsupportedEcK163KeyWrap128() {
        // NIST.FIPS.186-4 Koblitz Curve over Binary Field
        unsupportedEc("K-163", Algorithm.ECDH_ES_A128KW);
    }

    @Test
    public void unsupportedEcK163KeyWrap192() {
        // NIST.FIPS.186-4 Koblitz Curve over Binary Field
        unsupportedEc("K-163", Algorithm.ECDH_ES_A192KW);
    }

    @Test
    public void unsupportedEcK163KeyWrap256() {
        // NIST.FIPS.186-4 Koblitz Curve over Binary Field
        unsupportedEc("K-163", Algorithm.ECDH_ES_A256KW);
    }

    private String supportedEc(String ecInNistRep, String algorithm) {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", GeneratedEcdhKeyProviderFactory.ID);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, Long.toString(priority));
        if (ecInNistRep != null) {
            rep.getConfig().putSingle(ECDH_ELLIPTIC_CURVE_KEY, ecInNistRep);
        } else {
            ecInNistRep = DEFAULT_EC;
        }
        rep.getConfig().putSingle(ECDH_ALGORITHM_KEY, algorithm);

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        realm.cleanup().add(r -> r.components().component(id).remove());
        response.close();

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();

        // stands for the number of properties in the key provider config
        assertEquals(3, createdRep.getConfig().size());
        assertEquals(Long.toString(priority), createdRep.getConfig().getFirst(Attributes.PRIORITY_KEY));
        assertEquals(ecInNistRep, createdRep.getConfig().getFirst(ECDH_ELLIPTIC_CURVE_KEY));
        assertEquals(algorithm, createdRep.getConfig().getFirst(ECDH_ALGORITHM_KEY));

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
        assertEquals(KeyUse.ENC, key.getUse());
        assertEquals(priority, key.getProviderPriority());

        return id; // created key's component id
    }

    private void unsupportedEc(String ecInNistRep, String algorithmMode) {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", GeneratedEcdhKeyProviderFactory.ID);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, Long.toString(priority));
        rep.getConfig().putSingle(ECDH_ELLIPTIC_CURVE_KEY, ecInNistRep);
        rep.getConfig().putSingle(ECDH_ALGORITHM_KEY, algorithmMode);

        Response response = realm.admin().components().add(rep);
        Assertions.assertEquals(400, response.getStatus());
        response.close();
    }

    @Test
    public void changeCurveFromP256ToP384Direct() throws Exception {
        changeCurve("P-256", "P-384", Algorithm.ECDH_ES, Algorithm.ECDH_ES);
    }

    @Test
    public void changeCurveFromP384ToP521Direct() throws Exception  {
        changeCurve("P-384", "P-521", Algorithm.ECDH_ES, Algorithm.ECDH_ES);
    }

    @Test
    public void changeCurveFromP521ToP256Direct() throws Exception  {
        changeCurve("P-521", "P-256", Algorithm.ECDH_ES, Algorithm.ECDH_ES);
    }

    @Test
    public void changeCurveFromP256ToP384KeyWrap() throws Exception {
        changeCurve("P-256", "P-384", Algorithm.ECDH_ES_A128KW, Algorithm.ECDH_ES_A192KW);
    }

    @Test
    public void changeCurveFromP384ToP521KeyWrap() throws Exception  {
        changeCurve("P-384", "P-521", Algorithm.ECDH_ES_A192KW, Algorithm.ECDH_ES_A256KW);
    }

    @Test
    public void changeCurveFromP521ToP256KeyWrap() throws Exception  {
        changeCurve("P-521", "P-256", Algorithm.ECDH_ES_A256KW, Algorithm.ECDH_ES_A128KW);
    }

	private void changeCurve(String fromEcInNistRep, String toEcInNistRep, String fromAlgorithm, String toAlgorithm)
            throws Exception {
        String keyComponentId = supportedEc(fromEcInNistRep, fromAlgorithm);
        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();
        KeysMetadataRepresentation.KeyMetadataRepresentation originalKey = null;
        for (KeyMetadataRepresentation k : keys.getKeys()) {
           if (KeyType.EC.equals(k.getType()) && keyComponentId.equals(k.getProviderId())) {
                originalKey = k;
                break;
           }
        }

        ComponentRepresentation createdRep = realm.admin().components().component(keyComponentId).toRepresentation();
        createdRep.getConfig().putSingle(ECDH_ELLIPTIC_CURVE_KEY, toEcInNistRep);
        createdRep.getConfig().putSingle(ECDH_ALGORITHM_KEY, toAlgorithm);
        realm.admin().components().component(keyComponentId).update(createdRep);

        createdRep = realm.admin().components().component(keyComponentId).toRepresentation();

        // stands for the number of properties in the key provider config
        assertEquals(3, createdRep.getConfig().size());
        assertEquals(toEcInNistRep, createdRep.getConfig().getFirst(ECDH_ELLIPTIC_CURVE_KEY));
        assertEquals(toAlgorithm, createdRep.getConfig().getFirst(ECDH_ALGORITHM_KEY));

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
        assertEquals(KeyUse.ENC, key.getUse());
        assertEquals(originalKey.getAlgorithm(), fromAlgorithm);
        assertEquals(key.getAlgorithm(), toAlgorithm);
        assertEquals(toEcInNistRep, getCurveFromPublicKey(key.getPublicKey()));
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

    private String getCurveFromPublicKey(String publicEcKeyBase64Encoded) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicEcKeyBase64Encoded));
        ECPublicKey ecKey = (ECPublicKey) kf.generatePublic(publicKeySpec);
        return "P-" + ecKey.getParams().getCurve().getField().getFieldSize();
    }
}
