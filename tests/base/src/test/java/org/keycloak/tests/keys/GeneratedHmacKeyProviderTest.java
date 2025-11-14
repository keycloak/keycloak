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

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.keys.GeneratedHmacKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.runonserver.RunHelpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class GeneratedHmacKeyProviderTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void defaultKeysize() throws Exception {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", GeneratedHmacKeyProviderFactory.ID);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();
        assertEquals(1, createdRep.getConfig().size());
        assertEquals(Long.toString(priority), createdRep.getConfig().getFirst("priority"));

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = null;
        for (KeysMetadataRepresentation.KeyMetadataRepresentation k : keys.getKeys()) {
            if (k.getAlgorithm().equals(Algorithm.HS256)) {
                key = k;
                break;
            }
        }

        assertEquals(id, key.getProviderId());
        assertEquals(KeyType.OCT, key.getType());
        assertEquals(priority, key.getProviderPriority());

        ComponentRepresentation component = runOnServer.fetch(RunHelpers.internalComponent(id));
        assertEquals(GeneratedHmacKeyProviderFactory.DEFAULT_HMAC_KEY_SIZE, Base64Url.decode(component.getConfig().getFirst("secret")).length);
    }

    @Test
    public void largeKeysize() {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", GeneratedHmacKeyProviderFactory.ID);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));
        rep.getConfig().putSingle("secretSize", "512");

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();
        assertEquals(2, createdRep.getConfig().size());
        assertEquals("512", createdRep.getConfig().getFirst("secretSize"));

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = null;
        for (KeysMetadataRepresentation.KeyMetadataRepresentation k : keys.getKeys()) {
            if (k.getAlgorithm().equals(Algorithm.HS256)) {
                key = k;
                break;
            }
        }

        assertEquals(id, key.getProviderId());
        assertEquals(KeyType.OCT, key.getType());
        assertEquals(priority, key.getProviderPriority());

        ComponentRepresentation component = runOnServer.fetch(RunHelpers.internalComponent(id));
        assertEquals(512, Base64Url.decode(component.getConfig().getFirst("secret")).length);
    }

    @Test
    public void updateKeysize() throws Exception {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", GeneratedHmacKeyProviderFactory.ID);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));

        try (Response response = realm.admin().components().add(rep)) {
            rep.setId(ApiUtil.getCreatedId(response));
        }

        ComponentRepresentation component = runOnServer.fetch(RunHelpers.internalComponent(rep.getId()));
        assertEquals(GeneratedHmacKeyProviderFactory.DEFAULT_HMAC_KEY_SIZE, Base64Url.decode(component.getConfig().getFirst("secret")).length);

        ComponentRepresentation createdRep = realm.admin().components().component(rep.getId()).toRepresentation();
        createdRep.getConfig().putSingle("secretSize", "512");
        realm.admin().components().component(rep.getId()).update(createdRep);

        component = runOnServer.fetch(RunHelpers.internalComponent(rep.getId()));
        assertEquals(512, Base64Url.decode(component.getConfig().getFirst("secret")).length);
        component = runOnServer.fetch(RunHelpers.internalComponent(rep.getId()));
        String secret = component.getConfig().getFirst("secret");

        createdRep = realm.admin().components().component(rep.getId()).toRepresentation();
        createdRep.getConfig().putSingle("secretSize", "");
        realm.admin().components().component(rep.getId()).update(createdRep);

        component = runOnServer.fetch(RunHelpers.internalComponent(rep.getId()));
        assertEquals("512", component.getConfig().getFirst("secretSize"));
        assertEquals(512, Base64Url.decode(component.getConfig().getFirst("secret")).length);
        component = runOnServer.fetch(RunHelpers.internalComponent(rep.getId()));
        assertEquals(secret, component.getConfig().getFirst("secret"));
    }

    @Test
    public void invalidKeysize() throws Exception {
        ComponentRepresentation rep = createRep("invalid", GeneratedHmacKeyProviderFactory.ID);
        rep.getConfig().putSingle("secretSize", "1234");

        Response response = realm.admin().components().add(rep);
        assertErrror(response, "'Secret size' should be 16, 24, 32, 64, 128, 256 or 512");
    }

    protected void assertErrror(Response response, String error) {
        if (!response.hasEntity()) {
            fail("No error message set");
        }

        ErrorRepresentation errorRepresentation = response.readEntity(ErrorRepresentation.class);
        assertEquals(error, errorRepresentation.getErrorMessage());
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
