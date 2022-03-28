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

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.keys.JavaKeystoreKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JavaKeystoreKeyProviderTest extends AbstractKeycloakTest {

    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsPAJ/X39oNRkoS+baWVhAghfO86ZPfkSHm4evmMDhbA0KqW1/hg55qUJoT91ytGozIsIxoCLKzQvZTluRpt0AMp7cmfaGWBQ8cBtb8/BL+5FkUucigmOcTrfPq9/xR9g4AMSXRItjLRsJPy2Bnjau64DVQ3N5NVbWAMw7/1XjuobEyPnw0RLqEr/TxWMteuaiV1n8amIAiT91xZ8UFyPv3urCkAz+r+iyVvdJcZwn2tUL6KLM7qX/HSX8SUtPrIMB8EdW1yNt5McO8Ro5GxwiyXimDKbY9ur2WP8/wrdk/0TkoUYeI1UsnFyoJcqqg2+1T+dNAMtJhF7uDhURVQ33QIDAQAB";
    private static final String CERTIFICATE = "MIIDeTCCAmGgAwIBAgIEbhSauDANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMCAXDTE2MTAxMzE4MjUxNFoYDzIyOTAwNzI4MTgyNTE0WjBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsPAJ/X39oNRkoS+baWVhAghfO86ZPfkSHm4evmMDhbA0KqW1/hg55qUJoT91ytGozIsIxoCLKzQvZTluRpt0AMp7cmfaGWBQ8cBtb8/BL+5FkUucigmOcTrfPq9/xR9g4AMSXRItjLRsJPy2Bnjau64DVQ3N5NVbWAMw7/1XjuobEyPnw0RLqEr/TxWMteuaiV1n8amIAiT91xZ8UFyPv3urCkAz+r+iyVvdJcZwn2tUL6KLM7qX/HSX8SUtPrIMB8EdW1yNt5McO8Ro5GxwiyXimDKbY9ur2WP8/wrdk/0TkoUYeI1UsnFyoJcqqg2+1T+dNAMtJhF7uDhURVQ33QIDAQABoyEwHzAdBgNVHQ4EFgQUgz0ABmkImZUEO2/w0shoH4rp6pwwDQYJKoZIhvcNAQELBQADggEBAK+syjqfFXmv7942+ZfmJfb4i/JilhwSyA2G1VvGR39dLW1nPmKMMUY6kKgJ2NZgaCGvJ4jxDhfNJ1jPG7rcO/eQuF3cx9r+nHiTcJ5PNLqG2q4dNNFshJ8aGuIaTQEB7S1OlGsEj0rd0YlJ+LTrFfEHsnsJvpvDRLdVMklib5fPk4W8ziuQ3rr6T/a+be3zfAqmFZx8j6E46jz9QO841uwqdzcR9kfSHS/76TNGZv8OB6jheyHrUdBygR85iizHgMqats/0zWmKEAvSp/DhAfyIFp8zZHvPjmpBl+mfmAqnrYY0oJRb5rRXmL8DKq5plc7jgO1H6aHh5mV6slXQDEw=";


    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;
    private File file;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();

        file = folder.newFile("keystore.jsk");

        InputStream resourceAsStream = JavaKeystoreKeyProviderTest.class.getResourceAsStream("keystore.jks");
        IOUtils.copy(resourceAsStream, new FileOutputStream(file));
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void create() throws Exception {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", priority);

        Response response = adminClient.realm("test").components().add(rep);
        String id = ApiUtil.getCreatedId(response);

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
        assertEquals(PUBLIC_KEY, key.getPublicKey());
        assertEquals(CERTIFICATE, key.getCertificate());
    }

    @Test
    public void invalidKeystore() throws Exception {
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis());
        rep.getConfig().putSingle("keystore", "/nosuchfile");

        Response response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "Failed to load keys. File not found on server.");
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void invalidKeystorePassword() throws Exception {
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis());
        rep.getConfig().putSingle("keystore", "invalid");

        Response response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "Failed to load keys. File not found on server.");
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void invalidKeyAlias() throws Exception {
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis());
        rep.getConfig().putSingle("keyAlias", "invalid");

        Response response = adminClient.realm("test").components().add(rep);
        assertErrror(response, "Failed to load keys. Error creating X509v1Certificate.");
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void invalidKeyPassword() throws Exception {
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis());
        rep.getConfig().putSingle("keyPassword", "invalid");

        Response response = adminClient.realm("test").components().add(rep);
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
        rep.getConfig().putSingle("keystore", file.getAbsolutePath());
        rep.getConfig().putSingle("keystorePassword", "password");
        rep.getConfig().putSingle("keyAlias", "selfsigned");
        rep.getConfig().putSingle("keyPassword", "password");
        return rep;
    }

}

