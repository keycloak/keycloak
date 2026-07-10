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

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedMlDsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
@DatabaseTest
public class GeneratedMlDsaKeyProviderTest {

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void createsAndRotatesEveryMlDsaParameterSet() {
        String[] algorithms = { Algorithm.ML_DSA_44, Algorithm.ML_DSA_65, Algorithm.ML_DSA_87 };
        for (int i = 0; i < algorithms.length; i++) {
            String algorithm = algorithms[i];
            String replacement = algorithms[(i + 1) % algorithms.length];
            ComponentRepresentation component = createComponent(algorithm);

            Response response = realm.admin().components().add(component);
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.components().component(id).remove());
            response.close();

            KeyMetadataRepresentation original = findKey(id);
            assertEquals(KeyType.AKP, original.getType());
            assertEquals(algorithm, original.getAlgorithm());
            assertNotNull(original.getPublicKey());

            ComponentRepresentation stored = realm.admin().components().component(id).toRepresentation();
            stored.getConfig().putSingle(GeneratedMlDsaKeyProviderFactory.MLDSA_ALGORITHM_KEY, replacement);
            realm.admin().components().component(id).update(stored);

            KeyMetadataRepresentation rotated = findKey(id);
            assertEquals(replacement, rotated.getAlgorithm());
            assertNotEquals(original.getKid(), rotated.getKid());
            assertNotEquals(original.getPublicKey(), rotated.getPublicKey());
        }
    }

    private ComponentRepresentation createComponent(String algorithm) {
        ComponentRepresentation component = new ComponentRepresentation();
        component.setName("generated-" + algorithm);
        component.setParentId(realm.admin().toRepresentation().getId());
        component.setProviderId(GeneratedMlDsaKeyProviderFactory.ID);
        component.setProviderType(KeyProvider.class.getName());
        component.setConfig(new MultivaluedHashMap<>());
        component.getConfig().putSingle(Attributes.PRIORITY_KEY, Long.toString(System.currentTimeMillis()));
        component.getConfig().putSingle(GeneratedMlDsaKeyProviderFactory.MLDSA_ALGORITHM_KEY, algorithm);
        return component;
    }

    private KeyMetadataRepresentation findKey(String providerId) {
        return realm.admin().keys().getKeyMetadata().getKeys().stream()
                .filter(key -> providerId.equals(key.getProviderId()))
                .findFirst()
                .orElseThrow();
    }
}
