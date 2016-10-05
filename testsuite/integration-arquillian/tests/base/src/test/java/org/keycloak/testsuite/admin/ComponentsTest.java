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

package org.keycloak.testsuite.admin;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.KeyProvider;
import org.keycloak.keys.RsaKeyProviderFactory;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ComponentsTest extends AbstractAdminTest {

    private KeyPair keyPair;
    private String privateKeyPem;
    private String certificatePem;
    private ComponentsResource components;

    @Before
    public void before() throws Exception {
        components = adminClient.realm(REALM_NAME).components();

        keyPair = KeyUtils.generateRsaKeyPair(1024);
        privateKeyPem = PemUtils.encodeKey(keyPair.getPrivate());
        certificatePem = PemUtils.encodeCertificate(CertificateUtils.generateV1SelfSignedCertificate(keyPair, "name", new BigInteger("aaaa".getBytes())));
    }

    @Test
    public void testCreateValidation() {
        ComponentRepresentation rep = createComponentRepresentation(privateKeyPem, certificatePem);

        // Check validation is invoked
        try {
            createComponent(rep);
        } catch (WebApplicationException e) {
            assertErrror(e.getResponse(), "Private RSA Key is required");
        }
    }

    @Test
    public void testCreateEmptyValues() {
        ComponentRepresentation rep = createComponentRepresentation(privateKeyPem, certificatePem);

        rep.getConfig().addFirst(Attributes.PRIVATE_KEY_KEY, privateKeyPem);
        rep.getConfig().addFirst(Attributes.PRIORITY_KEY, "");
        rep.getConfig().put(Attributes.CERTIFICATE_KEY, null);
        rep.getConfig().put(Attributes.ENABLED_KEY, Collections.emptyList());

        String id = createComponent(rep);
        ComponentRepresentation returned = components.component(id).toRepresentation();
        assertEquals(2, returned.getConfig().size());
        assertEquals(1, returned.getConfig().get(Attributes.PRIVATE_KEY_KEY).size());
        assertEquals(1, returned.getConfig().get(Attributes.CERTIFICATE_KEY).size());
    }

    @Test
    public void testUpdate() {
        ComponentRepresentation rep = createComponentRepresentation(privateKeyPem, certificatePem);

        rep.getConfig().addFirst(Attributes.PRIVATE_KEY_KEY, privateKeyPem);
        rep.getConfig().addFirst(Attributes.PRIORITY_KEY, "100");
        rep.getConfig().addFirst(Attributes.ENABLED_KEY, "true");

        String id = createComponent(rep);
        ComponentRepresentation returned = components.component(id).toRepresentation();
        assertEquals(4, returned.getConfig().size());
        assertEquals("100", returned.getConfig().getFirst(Attributes.PRIORITY_KEY));

        // Check value updated
        returned.getConfig().putSingle(Attributes.PRIORITY_KEY, "200");

        // Check null deletes property
        returned.getConfig().putSingle(Attributes.ENABLED_KEY, null);

        components.component(id).update(returned);

        returned = components.component(id).toRepresentation();
        assertEquals(3, returned.getConfig().size());
        assertEquals("200", returned.getConfig().getFirst(Attributes.PRIORITY_KEY));

        // Check empty string removes property
        returned.getConfig().addFirst(Attributes.PRIORITY_KEY, "");

        components.component(id).update(returned);

        returned = components.component(id).toRepresentation();
        assertEquals(2, returned.getConfig().size());
        assertEquals(null, returned.getConfig().getFirst(Attributes.PRIORITY_KEY));

        // Check empty list removes property
        returned.getConfig().put(Attributes.PRIORITY_KEY, Collections.emptyList());

        components.component(id).update(returned);

        returned = components.component(id).toRepresentation();
        assertEquals(2, returned.getConfig().size());
    }

    @Test
    public void secretConfig() throws Exception {
        // Create component with secret config, relies on RsaKeyProvider as it has secret values and also a
        // means to verify it's configured through rest endpoints
        ComponentRepresentation rep = createComponentRepresentation(privateKeyPem, certificatePem);
        rep.getConfig().addFirst(Attributes.PRIVATE_KEY_KEY, privateKeyPem);

        // Create component with secret value
        String id = createComponent(rep);

        // Check secret value is not returned
        ComponentRepresentation returned = components.component(id).toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, returned.getConfig().getFirst(Attributes.PRIVATE_KEY_KEY));

        // Check component is configured correctly
        assertPublicKey(id, keyPair.getPublic());

        // Update component to make sure secret value is not set to '********'
        returned.getConfig().putSingle("priority", "200");
        components.component(id).update(returned);
        ComponentRepresentation returned2 = components.component(id).toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, returned2.getConfig().getFirst(Attributes.PRIVATE_KEY_KEY));
        assertEquals("200", returned.getConfig().getFirst("priority"));

        // Check component is still configured correctly
        assertPublicKey(id, keyPair.getPublic());
    }

    private void assertPublicKey(String providerId, PublicKey exectedPublicKey) {
        List<KeysMetadataRepresentation.KeyMetadataRepresentation> keyMetadata = realm.keys().getKeyMetadata().getKeys();
        for (KeysMetadataRepresentation.KeyMetadataRepresentation k : keyMetadata) {
            if (k.getProviderId().equals(providerId)) {
                assertEquals(PemUtils.encodeKey(exectedPublicKey), k.getPublicKey());
                return;
            }
        }
        fail("Key not found");
    }

    private String createComponent(ComponentRepresentation rep) {
        ComponentsResource components = realm.components();
        Response response = components.add(rep);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        return id;
    }

    private void assertErrror(Response response, String error) {
        if (!response.hasEntity()) {
            fail("No error message set");
        }

        ErrorRepresentation errorRepresentation = response.readEntity(ErrorRepresentation.class);
        assertEquals(error, errorRepresentation.getErrorMessage());
    }

    private ComponentRepresentation createComponentRepresentation(String privateKeyPem, String certificatePem) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("mycomponent");
        rep.setParentId(realmId);
        rep.setProviderId(RsaKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());

        MultivaluedHashMap config = new MultivaluedHashMap();

        rep.setConfig(config);
        return rep;
    }

}
