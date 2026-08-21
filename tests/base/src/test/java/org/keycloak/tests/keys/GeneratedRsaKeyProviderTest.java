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
import java.util.Date;

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
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.utils.StringUtil;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
@DatabaseTest
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

    // testing custom validity periods

    @Test
    public void createSigWithCustomValidityDate() {
        createKeyWithCustomValidityDate(GeneratedRsaKeyProviderFactory.ID, KeyUse.SIG);
    }

    @Test
    public void createEncWithCustomValidityDate() {
        createKeyWithCustomValidityDate(GeneratedRsaEncKeyProviderFactory.ID, KeyUse.ENC);
    }

    private void createKeyWithCustomValidityDate(String providerId, KeyUse keyUse) {
        long priority = System.currentTimeMillis();

        Response response = createKeyProviderComponentWithCustomValidityDate(priority, providerId, "10");
        String id = ApiUtil.getCreatedId(response);
        realm.cleanup().add(r -> r.components().component(id).remove());

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();
        assertEquals("10", createdRep.getConfig().getFirst("numberDaysValid"));

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = keys.getKeys().get(0);

        assertNotNull(key);
        assertEquals(id, key.getProviderId());
        assertEquals(AlgorithmType.RSA.name(), key.getType());
        assertEquals(priority, key.getProviderPriority());

        Date certNotAfter = PemUtils.decodeCertificate(key.getCertificate()).getNotAfter();

        Long validTo = key.getValidTo();
        assertNotNull(validTo);
        assertEquals(certNotAfter.getTime(), validTo.longValue());

        Date now = new Date();
        Date lowerBound = DateUtils.addDays(now, 9);
        Date upperBound = DateUtils.addDays(now, 11);
        assertTrue(certNotAfter.after(lowerBound) && certNotAfter.before(upperBound));
        Date validToDate = new Date(validTo);
        assertTrue(validToDate.after(lowerBound) && validToDate.before(upperBound));

        assertEquals(keyUse, key.getUse());
    }


    private void createKeyProviderInvalidCustomDate(String providerId, String validDaysInput) {
        long priority = System.currentTimeMillis();

        Response response = createKeyProviderComponentWithCustomValidityDate(priority, providerId, validDaysInput);
        try (response) {
            assertEquals(400, response.getStatus());
        }
    }

    @Test
    public void createKeyWithInvalidNonNumberValidityDateForSig() {
        createKeyProviderInvalidCustomDate(GeneratedRsaKeyProviderFactory.ID, "non_number_string");
    }

    @Test
    public void createKeyWithInvalidNonNumberValidityDateForEnc() {
        createKeyProviderInvalidCustomDate(GeneratedRsaEncKeyProviderFactory.ID, "non_number_string");
    }

    @Test
    public void createKeyWithInvalidNonPositiveValidityDateForSig() {
        createKeyProviderInvalidCustomDate(GeneratedRsaKeyProviderFactory.ID, "-1");
        createKeyProviderInvalidCustomDate(GeneratedRsaKeyProviderFactory.ID, "0");
    }

    @Test
    public void createKeyWithInvalidNonPositiveValidityDateForEnc() {
        createKeyProviderInvalidCustomDate(GeneratedRsaEncKeyProviderFactory.ID, "-1");
        createKeyProviderInvalidCustomDate(GeneratedRsaEncKeyProviderFactory.ID, "0");
    }

    @Test
    public void regenerateOnValidityDecreaseForSig() {
        regenerateOnValidityDecrease(GeneratedRsaKeyProviderFactory.ID, KeyUse.SIG);
    }

    @Test
    public void regenerateOnValidityDecreaseForEnc() {
        regenerateOnValidityDecrease(GeneratedRsaEncKeyProviderFactory.ID, KeyUse.ENC);
    }

    private void regenerateOnValidityDecrease(String providerId, KeyUse keyUse) {
        // test updating key validity with regeneration to smaller value
        long priority = System.currentTimeMillis();

        Response response = createKeyProviderComponentWithCustomValidityDate(priority, providerId, "100");
        String id = ApiUtil.getCreatedId(response);
        realm.cleanup().add(r -> r.components().component(id).remove());

        KeysMetadataRepresentation.KeyMetadataRepresentation key100 = realm.admin().keys().getKeyMetadata().getKeys().get(0);

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();
        createdRep.getConfig().putSingle("numberDaysValid", String.valueOf(10));
        realm.admin().components().component(id).update(createdRep);

        KeysMetadataRepresentation.KeyMetadataRepresentation key10 = realm.admin().keys().getKeyMetadata().getKeys().get(0);

        assertNotNull(key10);
        assertNotNull(key100);
        assertEquals(id, key10.getProviderId());
        assertEquals(id, key100.getProviderId());

        // check if new key has been generated
        assertNotEquals(key10.getKid(), key100.getKid());

        assertTrue(key100.getValidTo() > key10.getValidTo());
        assertEquals(keyUse, key10.getUse());
        assertEquals(keyUse, key100.getUse());
    }

    @Test
    public void noRegenerateOnValidityIncreaseForSig() {
        noRegenerateOnValidityIncrease(GeneratedRsaKeyProviderFactory.ID, KeyUse.SIG);
    }


    @Test
    public void noRegenerateOnValidityIncreaseForEnc() {
        noRegenerateOnValidityIncrease(GeneratedRsaEncKeyProviderFactory.ID, KeyUse.ENC);
    }


    private void noRegenerateOnValidityIncrease(String providerId, KeyUse keyUse) {
        // test no regeneration on updating to larger validity period
        long priority = System.currentTimeMillis();

        Response response = createKeyProviderComponentWithCustomValidityDate(priority, providerId, "100");
        String id = ApiUtil.getCreatedId(response);
        realm.cleanup().add(r -> r.components().component(id).remove());

        KeysMetadataRepresentation.KeyMetadataRepresentation key100 = realm.admin().keys().getKeyMetadata().getKeys().get(0);

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();
        createdRep.getConfig().putSingle("numberDaysValid", String.valueOf(200));
        realm.admin().components().component(id).update(createdRep);

        KeysMetadataRepresentation.KeyMetadataRepresentation key200 = realm.admin().keys().getKeyMetadata().getKeys().get(0);

        assertNotNull(key200);
        assertNotNull(key100);
        assertEquals(id, key200.getProviderId());
        assertEquals(id, key100.getProviderId());

        // check if new key has been generated
        assertEquals(key200.getKid(), key100.getKid());

        assertEquals(key100.getValidTo(), key200.getValidTo());
        assertEquals(keyUse, key200.getUse());
        assertEquals(keyUse, key100.getUse());
    }

    private Response createKeyProviderComponentWithCustomValidityDate(long priority, String providerId, String validityDateInput) {
        // make priority the current time
        // this makes sure the provider gets the highest priority
        ComponentRepresentation rep = createRep("customValidityDate", providerId);
        rep.getConfig().putSingle("priority", Long.toString(priority));
        rep.getConfig().putSingle("numberDaysValid", validityDateInput);

        return realm.admin().components().add(rep);
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
