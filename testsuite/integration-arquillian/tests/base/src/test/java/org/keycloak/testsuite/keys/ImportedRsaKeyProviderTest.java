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
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.ImportedRsaKeyProviderFactory;
import org.keycloak.keys.KeyMetadata;
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
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.util.List;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ImportedRsaKeyProviderTest extends AbstractKeycloakTest {

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
    public void privateKeyOnly() throws Exception {
        long priority = System.currentTimeMillis();

        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        String kid = KeyUtils.createKeyId(keyPair.getPublic());

        ComponentRepresentation rep = createRep("valid", ImportedRsaKeyProviderFactory.ID);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, Long.toString(priority));

        Response response = adminClient.realm("test").components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        ComponentRepresentation createdRep = adminClient.realm("test").components().component(id).toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, createdRep.getConfig().getFirst(Attributes.PRIVATE_KEY_KEY));
        assertNotNull(createdRep.getConfig().getFirst(Attributes.CERTIFICATE_KEY));

        assertEquals(keyPair.getPublic(), PemUtils.decodeCertificate(createdRep.getConfig().getFirst(Attributes.CERTIFICATE_KEY)).getPublicKey());

        KeysMetadataRepresentation keys = adminClient.realm("test").keys().getKeyMetadata();

        assertEquals(kid, keys.getActive().get(Algorithm.RS256));

        KeysMetadataRepresentation.KeyMetadataRepresentation key = keys.getKeys().get(0);

        assertEquals(id, key.getProviderId());
        assertEquals(AlgorithmType.RSA.name(), key.getType());
        assertEquals(priority, key.getProviderPriority());
        assertEquals(kid, key.getKid());
        assertEquals(PemUtils.encodeKey(keyPair.getPublic()), keys.getKeys().get(0).getPublicKey());
        assertEquals(keyPair.getPublic(), PemUtils.decodeCertificate(key.getCertificate()).getPublicKey());
    }

    @Test
    public void keyAndCertificate() throws Exception {
        long priority = System.currentTimeMillis();

        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "test");
        String certificatePem = PemUtils.encodeCertificate(certificate);

        ComponentRepresentation rep = createRep("valid", ImportedRsaKeyProviderFactory.ID);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));
        rep.getConfig().putSingle(Attributes.CERTIFICATE_KEY, certificatePem);
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, Long.toString(priority));

        Response response = adminClient.realm("test").components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        ComponentRepresentation createdRep = adminClient.realm("test").components().component(id).toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, createdRep.getConfig().getFirst(Attributes.PRIVATE_KEY_KEY));
        assertEquals(certificatePem, createdRep.getConfig().getFirst(Attributes.CERTIFICATE_KEY));

        KeysMetadataRepresentation keys = adminClient.realm("test").keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = keys.getKeys().get(0);
        assertEquals(certificatePem, key.getCertificate());
    }

    @Test
    public void invalidPriority() throws Exception {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);

        ComponentRepresentation rep = createRep("invalid", ImportedRsaKeyProviderFactory.ID);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, "invalid");

        Response response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "'Priority' should be a number");
    }

    @Test
    public void invalidEnabled() throws Exception {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);

        ComponentRepresentation rep = createRep("invalid", ImportedRsaKeyProviderFactory.ID);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));
        rep.getConfig().putSingle(Attributes.ENABLED_KEY, "invalid");

        Response response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "'Enabled' should be 'true' or 'false'");
    }

    @Test
    public void invalidActive() throws Exception {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);

        ComponentRepresentation rep = createRep("invalid", ImportedRsaKeyProviderFactory.ID);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));
        rep.getConfig().putSingle(Attributes.ACTIVE_KEY, "invalid");

        Response response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "'Active' should be 'true' or 'false'");
    }

    @Test
    public void invalidPrivateKey() throws Exception {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);

        ComponentRepresentation rep = createRep("invalid", ImportedRsaKeyProviderFactory.ID);

        Response response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "'Private RSA Key' is required");

        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, "nonsense");
        response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "Failed to decode private key");

        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPublic()));
        response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "Failed to decode private key");
    }

    @Test
    public void invalidCertificate() throws Exception {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        Certificate invalidCertificate = CertificateUtils.generateV1SelfSignedCertificate(KeyUtils.generateRsaKeyPair(2048), "test");

        ComponentRepresentation rep = createRep("invalid", ImportedRsaKeyProviderFactory.ID);
        rep.getConfig().putSingle(Attributes.PRIVATE_KEY_KEY, PemUtils.encodeKey(keyPair.getPrivate()));

        rep.getConfig().putSingle(Attributes.CERTIFICATE_KEY, "nonsense");
        Response response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "Failed to decode certificate");

        rep.getConfig().putSingle(Attributes.CERTIFICATE_KEY, PemUtils.encodeCertificate(invalidCertificate));
        response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "Certificate does not match private key");

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
        rep.setParentId("test");
        rep.setProviderId(providerId);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        return rep;
    }

}

