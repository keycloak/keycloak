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

import java.security.PublicKey;
import java.util.Base64;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwk.AKPUtils;
import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.keys.GeneratedMldsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class GeneratedMldsaKeyProviderTest {

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void defaultKeysizeForMldsa44() {
        defaultKeysize("ML-DSA-44");
    }

    @Test
    public void defaultKeysizeForMldsa65() {
        defaultKeysize("ML-DSA-65");
    }

    @Test
    public void defaultKeysizeForMldsa87() {
        defaultKeysize("ML-DSA-87");
    }

    private void defaultKeysize(String algorithm) {
        String providerId = GeneratedMldsaKeyProviderFactory.ID;
        KeyUse keyUse = KeyUse.SIG;
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
        assertEquals(AlgorithmType.ML_DSA.name(), key.getType());
        assertEquals(priority, key.getProviderPriority());
        PublicKey pub = PemUtils.decodePublicKey(keys.getKeys().get(0).getPublicKey());
        byte[] reDecoded = Base64.getUrlDecoder().decode(AKPUtils.toEncodedPub(pub, algorithm));
        assertEquals(mldsaLength(algorithm), reDecoded.length);
        assertEquals(keyUse, key.getUse());
    }

    @Test
    public void updatePriority() {
        String providerId = GeneratedMldsaKeyProviderFactory.ID;
        KeyUse keyUse = KeyUse.SIG;
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

    protected int mldsaLength(String algorithm) {
        return switch (algorithm) {
            case "ML-DSA-44" -> 1312;
            case "ML-DSA-65" -> 1952;
            case "ML-DSA-87" -> 2592;
            default -> -1;
        };
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
