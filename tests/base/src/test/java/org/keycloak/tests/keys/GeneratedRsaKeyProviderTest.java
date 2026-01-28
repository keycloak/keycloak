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

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.keys.GeneratedRsaEncKeyProviderFactory;
import org.keycloak.keys.GeneratedRsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectCryptoHelper;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.crypto.CryptoHelper;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.utils.StringUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class GeneratedRsaKeyProviderTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectCryptoHelper
    CryptoHelper cryptoHelper;

    @Test
    public void defaultKeysizeForSig() throws Exception {
        defaultKeysize(GeneratedRsaKeyProviderFactory.ID, KeyUse.SIG);
    }

    @Test
    public void defaultKeysizeForEnc() throws Exception {
        defaultKeysize(GeneratedRsaEncKeyProviderFactory.ID, KeyUse.ENC);
    }

    private void defaultKeysize(String providerId, KeyUse keyUse) throws Exception {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", providerId);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        realm.cleanup().add(r -> r.components().component(id).remove());
        response.close();

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();
        assertEquals(1, createdRep.getConfig().size());
        assertEquals(Long.toString(priority), createdRep.getConfig().getFirst("priority"));

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = keys.getKeys().get(0);

        assertEquals(id, key.getProviderId());
        assertEquals(AlgorithmType.RSA.name(), key.getType());
        assertEquals(priority, key.getProviderPriority());
        assertEquals(2048, ((RSAPublicKey) PemUtils.decodePublicKey(keys.getKeys().get(0).getPublicKey())).getModulus().bitLength());
        assertEquals(keyUse, key.getUse());
    }

    @Test
    public void largeKeysizeForSig() throws Exception {
        largeKeysize(GeneratedRsaKeyProviderFactory.ID, KeyUse.SIG);
    }

    @Test
    public void largeKeysizeForEnc() throws Exception {
        largeKeysize(GeneratedRsaEncKeyProviderFactory.ID, KeyUse.ENC);
    }

    private void largeKeysize(String providerId, KeyUse keyUse) throws Exception {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", providerId);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));
        rep.getConfig().putSingle("keySize", "4096");

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        realm.cleanup().add(r -> r.components().component(id).remove());
        response.close();

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();
        assertEquals(2, createdRep.getConfig().size());
        assertEquals("4096", createdRep.getConfig().getFirst("keySize"));

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = keys.getKeys().get(0);

        assertEquals(id, key.getProviderId());
        assertEquals(AlgorithmType.RSA.name(), key.getType());
        assertEquals(priority, key.getProviderPriority());
        assertEquals(4096, ((RSAPublicKey) PemUtils.decodePublicKey(keys.getKeys().get(0).getPublicKey())).getModulus().bitLength());
        assertEquals(keyUse, key.getUse());
    }

    @Test
    public void updatePriorityForSig() throws Exception {
        updatePriority(GeneratedRsaKeyProviderFactory.ID, KeyUse.SIG);
    }

    @Test
    public void updatePriorityForEnc() throws Exception {
        updatePriority(GeneratedRsaEncKeyProviderFactory.ID, KeyUse.ENC);
    }

    private void updatePriority(String providerId, KeyUse keyUse) throws Exception {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", providerId);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        realm.cleanup().add(r -> r.components().component(id).remove());
        response.close();

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();

        String publicKey = keys.getKeys().get(0).getPublicKey();

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();

        priority += 1000;

        createdRep.getConfig().putSingle("priority", Long.toString(priority));
        realm.admin().components().component(id).update(createdRep);

        keys = realm.admin().keys().getKeyMetadata();

        String publicKey2 = keys.getKeys().get(0).getPublicKey();

        assertEquals(publicKey, publicKey2);
        assertEquals(keyUse, keys.getKeys().get(0).getUse());
    }

    @Test
    public void updateKeysizeForSig() throws Exception {
        updateKeysize(GeneratedRsaKeyProviderFactory.ID, KeyUse.SIG);
    }

    @Test
    public void updateKeysizeForEnc() throws Exception {
        updateKeysize(GeneratedRsaEncKeyProviderFactory.ID, KeyUse.ENC);
    }

    private void updateKeysize(String providerId, KeyUse keyUse) throws Exception {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", providerId);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        realm.cleanup().add(r -> r.components().component(id).remove());
        response.close();

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();

        String publicKey = keys.getKeys().get(0).getPublicKey();

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();
        createdRep.getConfig().putSingle("keySize", "4096");
        realm.admin().components().component(id).update(createdRep);

        keys = realm.admin().keys().getKeyMetadata();

        String publicKey2 = keys.getKeys().get(0).getPublicKey();

        assertNotEquals(publicKey, publicKey2);
        assertEquals(2048, ((RSAPublicKey) PemUtils.decodePublicKey(publicKey)).getModulus().bitLength());
        assertEquals(4096, ((RSAPublicKey) PemUtils.decodePublicKey(publicKey2)).getModulus().bitLength());
        assertEquals(keyUse, keys.getKeys().get(0).getUse());
    }

    @Test
    public void invalidKeysizeForSig() throws Exception {
        invalidKeysize(GeneratedRsaKeyProviderFactory.ID);
    }

    @Test
    public void invalidKeysizeForEnc() throws Exception {
        invalidKeysize(GeneratedRsaEncKeyProviderFactory.ID);
    }

    private void invalidKeysize(String providerId) throws Exception {
        ComponentRepresentation rep = createRep("invalid", providerId);
        rep.getConfig().putSingle("keySize", "1234");

        Response response = realm.admin().components().add(rep);
        String expectedKeySizesDisplay = StringUtil.joinValuesWithLogicalCondition("or", Arrays.asList(cryptoHelper.getExpectedSupportedRsaKeySizes()));
        assertErrror(response, "'Key size' should be " + expectedKeySizesDisplay);
    }

    protected void assertErrror(Response response, String error) {
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
