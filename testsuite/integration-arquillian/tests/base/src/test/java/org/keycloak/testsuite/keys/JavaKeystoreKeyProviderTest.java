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
import org.junit.rules.TemporaryFolder;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.keys.JavaKeystoreKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.KeystoreUtils;

import javax.ws.rs.core.Response;
import java.util.List;

import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.keycloak.common.util.KeystoreUtil.KeystoreFormat.PKCS12;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JavaKeystoreKeyProviderTest extends AbstractKeycloakTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;
    private KeystoreUtils.KeystoreInfo generatedKeystore;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void createJks() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.JKS);
    }

    @Test
    public void createPkcs12() throws Exception {
        createSuccess(PKCS12);
    }

    @Test
    public void createBcfks() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.BCFKS);
    }

    private void createSuccess(KeystoreUtil.KeystoreFormat keystoreType) throws Exception {
        KeystoreUtils.assumeKeystoreTypeSupported(keystoreType);
        generateKeystore(keystoreType);

        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", priority);

        Response response = adminClient.realm("test").components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addComponentId(id);

        ComponentRepresentation createdRep = adminClient.realm("test").components().component(id).toRepresentation();
        assertEquals(5, createdRep.getConfig().size());
        assertEquals(Long.toString(priority), createdRep.getConfig().getFirst("priority"));
        assertEquals(ComponentRepresentation.SECRET_VALUE, createdRep.getConfig().getFirst("keystorePassword"));
        assertEquals(ComponentRepresentation.SECRET_VALUE, createdRep.getConfig().getFirst("keyPassword"));

        KeysMetadataRepresentation keys = adminClient.realm("test").keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = keys.getKeys().get(0);

        assertEquals(id, key.getProviderId());
        assertEquals(AlgorithmType.RSA.name(), key.getType());
        assertEquals(priority, key.getProviderPriority());
        assertEquals(generatedKeystore.getCertificateInfo().getPublicKey(), key.getPublicKey());
        assertEquals(generatedKeystore.getCertificateInfo().getCertificate(), key.getCertificate());
    }

    @Test
    public void invalidKeystore() throws Exception {
        generateKeystore(KeystoreUtils.getPreferredKeystoreType());
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis());
        rep.getConfig().putSingle("keystore", "/nosuchfile");

        Response response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "Failed to load keys. File not found on server.");
    }

    @Test
    public void invalidKeystorePassword() throws Exception {
        generateKeystore(KeystoreUtils.getPreferredKeystoreType());
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis());
        rep.getConfig().putSingle("keystore", "invalid");

        Response response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "Failed to load keys. File not found on server.");
    }

    @Test
    public void invalidKeyAlias() throws Exception {
        generateKeystore(KeystoreUtils.getPreferredKeystoreType());
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis());
        rep.getConfig().putSingle("keyAlias", "invalid");

        Response response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "Failed to load keys. Error creating X509v1Certificate.");
    }

    @Test
    public void invalidKeyPassword() throws Exception {
        KeystoreUtil.KeystoreFormat keystoreType = KeystoreUtils.getPreferredKeystoreType();
        if (keystoreType == PKCS12) {
            // only the keyStore password is significant with PKCS12. Hence we need to test with different keystore type
            String[] supportedKsTypes = KeystoreUtils.getSupportedKeystoreTypes();
            if (supportedKsTypes.length <= 1) {
                Assert.fail("Only PKCS12 type is supported, but invalidKeyPassword() scenario cannot be tested with it");
            }
            keystoreType = Enum.valueOf(KeystoreUtil.KeystoreFormat.class, supportedKsTypes[1]);
            log.infof("Fallback to keystore type '%s' for the invalidKeyPassword() test", keystoreType);
        }
        generateKeystore(keystoreType);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis());
        rep.getConfig().putSingle("keyPassword", "invalid");

        Response response = adminClient.realm("test").components().add(rep);
        Assert.assertEquals(400, response.getStatus());
        assertErrror(response, "Failed to load keys. Keystore on server can not be recovered.");
    }

    protected void assertErrror(Response response, String error) {
        if (!response.hasEntity()) {
            fail("No error message set");
        }

        ErrorRepresentation errorRepresentation = response.readEntity(ErrorRepresentation.class);
        assertTrue(errorRepresentation.getErrorMessage().startsWith(error));
        response.close();
    }

    protected ComponentRepresentation createRep(String name, long priority) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(adminClient.realm("test").toRepresentation().getId());
        rep.setProviderId(JavaKeystoreKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));
        rep.getConfig().putSingle("keystore", generatedKeystore.getKeystoreFile().getAbsolutePath());
        rep.getConfig().putSingle("keystorePassword", "password");
        rep.getConfig().putSingle("keyAlias", "selfsigned");
        rep.getConfig().putSingle("keyPassword", "password");
        return rep;
    }

    private void generateKeystore(KeystoreUtil.KeystoreFormat keystoreType) throws Exception {
        this.generatedKeystore = KeystoreUtils.generateKeystore(folder, keystoreType, "selfsigned", "password", "password");
    }

}

