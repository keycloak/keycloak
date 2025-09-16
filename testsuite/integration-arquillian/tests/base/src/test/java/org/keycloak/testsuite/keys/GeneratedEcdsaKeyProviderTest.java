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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.keycloak.testsuite.AbstractAdminTest.loadJson;

import java.security.KeyFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyType;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;

public class GeneratedEcdsaKeyProviderTest extends AbstractKeycloakTest {
    private static final String DEFAULT_EC = GeneratedEcdsaKeyProviderFactory.DEFAULT_ECDSA_ELLIPTIC_CURVE;
    private static final String ECDSA_ELLIPTIC_CURVE_KEY = GeneratedEcdsaKeyProviderFactory.ECDSA_ELLIPTIC_CURVE_KEY;
    private static final String TEST_REALM_NAME = "test";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"),
                                             RealmRepresentation.class);
        testRealms.add(realm);
    }

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

        Response response = adminClient.realm(TEST_REALM_NAME).components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addComponentId(id);
        response.close();

        ComponentRepresentation createdRep = adminClient.realm(TEST_REALM_NAME).components().component(id).toRepresentation();

        // stands for the number of properties in the key provider config
        assertEquals(withCertificate ? 3 : 2, createdRep.getConfig().size());
        assertEquals(Long.toString(priority), createdRep.getConfig().getFirst(Attributes.PRIORITY_KEY));
        assertEquals(ecInNistRep, createdRep.getConfig().getFirst(ECDSA_ELLIPTIC_CURVE_KEY));
        if (withCertificate) {
            assertNotNull(createdRep.getConfig().getFirst(Attributes.EC_GENERATE_CERTIFICATE_KEY));
        }

        KeysMetadataRepresentation keys = adminClient.realm(TEST_REALM_NAME).keys().getKeyMetadata();

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
            final String expectedIssuerAndSubject = "CN=" + TEST_REALM_NAME;
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
        boolean isEcAccepted = true;

        Response response = null;
        try {
            response = adminClient.realm(TEST_REALM_NAME).components().add(rep);
            String id = ApiUtil.getCreatedId(response);
            getCleanup().addComponentId(id);
            response.close();
        } catch (WebApplicationException e) {
            isEcAccepted = false;
        } finally {
            response.close();
        }
        assertEquals(isEcAccepted, false);
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
        KeysMetadataRepresentation keys = adminClient.realm(TEST_REALM_NAME).keys().getKeyMetadata();
        KeysMetadataRepresentation.KeyMetadataRepresentation originalKey = null;
        for (KeyMetadataRepresentation k : keys.getKeys()) {
           if (KeyType.EC.equals(k.getType()) && keyComponentId.equals(k.getProviderId())) {
                originalKey = k;
                break;
           }
        }

        ComponentRepresentation createdRep = adminClient.realm(TEST_REALM_NAME).components().component(keyComponentId).toRepresentation();
        createdRep.getConfig().putSingle(ECDSA_ELLIPTIC_CURVE_KEY, ToEcInNistRep);
        adminClient.realm(TEST_REALM_NAME).components().component(keyComponentId).update(createdRep);

        createdRep = adminClient.realm(TEST_REALM_NAME).components().component(keyComponentId).toRepresentation();

        // stands for the number of properties in the key provider config
        assertEquals(2, createdRep.getConfig().size());
        assertEquals(ToEcInNistRep, createdRep.getConfig().getFirst(ECDSA_ELLIPTIC_CURVE_KEY));

        keys = adminClient.realm(TEST_REALM_NAME).keys().getKeyMetadata();
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
        rep.setParentId(adminClient.realm(TEST_REALM_NAME).toRepresentation().getId());
        rep.setProviderId(providerId);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        return rep;
    }

    private String getCurveFromPublicKey(String publicEcdsaKeyBase64Encoded) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.decode(publicEcdsaKeyBase64Encoded));
        ECPublicKey ecKey = (ECPublicKey) kf.generatePublic(publicKeySpec);
        return "P-" + ecKey.getParams().getCurve().getField().getFieldSize();
    }
}
