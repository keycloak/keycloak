/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.KeyType;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedEddsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class GeneratedEddsaKeyProviderTest {

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void defaultKidIsDerivedFromPublicKey() throws Exception {
        String id = createKeyProvider(null);

        KeyMetadataRepresentation key = getKey(id);
        PublicKey publicKey = KeyFactory.getInstance("EdDSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getMimeDecoder().decode(key.getPublicKey())));
        assertEquals(KeyUtils.createKeyId(publicKey), key.getKid());
    }

    @Test
    public void customKid() {
        String kid = "019a91a4-5b7e-7c3d-8f2a-4b6c8d0e1f23";
        String id = createKeyProvider(kid);

        assertEquals(kid, getKey(id).getKid());
    }

    private String createKeyProvider(String kid) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("valid");
        rep.setParentId(realm.admin().toRepresentation().getId());
        rep.setProviderId(GeneratedEddsaKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, Long.toString(System.currentTimeMillis()));
        if (kid != null) {
            rep.getConfig().putSingle(Attributes.KID_KEY, kid);
        }

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        realm.cleanup().add(r -> r.components().component(id).remove());
        response.close();
        return id;
    }

    private KeyMetadataRepresentation getKey(String componentId) {
        return realm.admin().keys().getKeyMetadata().getKeys().stream()
                .filter(k -> KeyType.OKP.equals(k.getType()) && componentId.equals(k.getProviderId()))
                .findFirst()
                .orElseThrow();
    }
}
