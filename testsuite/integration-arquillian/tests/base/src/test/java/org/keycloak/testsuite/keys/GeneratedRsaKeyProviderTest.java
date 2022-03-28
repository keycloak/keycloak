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

package org.keycloak.testsuite.keys;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;

import javax.ws.rs.core.Response;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GeneratedRsaKeyProviderTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

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

        Response response = adminClient.realm("test").components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addComponentId(id);
        response.close();

        ComponentRepresentation createdRep = adminClient.realm("test").components().component(id).toRepresentation();
        assertEquals(1, createdRep.getConfig().size());
        assertEquals(Long.toString(priority), createdRep.getConfig().getFirst("priority"));

        KeysMetadataRepresentation keys = adminClient.realm("test").keys().getKeyMetadata();

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

        Response response = adminClient.realm("test").components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addComponentId(id);
        response.close();

        ComponentRepresentation createdRep = adminClient.realm("test").components().component(id).toRepresentation();
        assertEquals(2, createdRep.getConfig().size());
        assertEquals("4096", createdRep.getConfig().getFirst("keySize"));

        KeysMetadataRepresentation keys = adminClient.realm("test").keys().getKeyMetadata();

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

        Response response = adminClient.realm("test").components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addComponentId(id);
        response.close();

        KeysMetadataRepresentation keys = adminClient.realm("test").keys().getKeyMetadata();

        String publicKey = keys.getKeys().get(0).getPublicKey();

        ComponentRepresentation createdRep = adminClient.realm("test").components().component(id).toRepresentation();

        priority += 1000;

        createdRep.getConfig().putSingle("priority", Long.toString(priority));
        adminClient.realm("test").components().component(id).update(createdRep);

        keys = adminClient.realm("test").keys().getKeyMetadata();

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

        Response response = adminClient.realm("test").components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addComponentId(id);
        response.close();

        KeysMetadataRepresentation keys = adminClient.realm("test").keys().getKeyMetadata();

        String publicKey = keys.getKeys().get(0).getPublicKey();

        ComponentRepresentation createdRep = adminClient.realm("test").components().component(id).toRepresentation();
        createdRep.getConfig().putSingle("keySize", "4096");
        adminClient.realm("test").components().component(id).update(createdRep);

        keys = adminClient.realm("test").keys().getKeyMetadata();

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
    public void invalidKeysizeForEnd() throws Exception {
        invalidKeysize(GeneratedRsaEncKeyProviderFactory.ID);
    }

    private void invalidKeysize(String providerId) throws Exception {
        ComponentRepresentation rep = createRep("invalid", providerId);
        rep.getConfig().putSingle("keySize", "1234");

        Response response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "'Key size' should be 1024, 2048 or 4096");
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
        rep.setParentId(adminClient.realm("test").toRepresentation().getId());
        rep.setProviderId(providerId);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        return rep;
    }

}

