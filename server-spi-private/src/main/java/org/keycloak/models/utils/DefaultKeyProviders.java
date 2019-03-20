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

package org.keycloak.models.utils;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.RealmModel;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeyProviders {

    public static void createProviders(RealmModel realm) {
        if (!hasProvider(realm, "rsa-generated")) {
            ComponentModel generated = new ComponentModel();
            generated.setName("rsa-generated");
            generated.setParentId(realm.getId());
            generated.setProviderId("rsa-generated");
            generated.setProviderType(KeyProvider.class.getName());

            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "100");
            generated.setConfig(config);

            realm.addComponentModel(generated);
        }

        createSecretProvider(realm);
        createAesProvider(realm);
    }

    public static void createSecretProvider(RealmModel realm) {
        if (hasProvider(realm, "hmac-generated")) return;
        ComponentModel generated = new ComponentModel();
        generated.setName("hmac-generated");
        generated.setParentId(realm.getId());
        generated.setProviderId("hmac-generated");
        generated.setProviderType(KeyProvider.class.getName());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", "100");
        config.putSingle("algorithm", Algorithm.HS256);
        generated.setConfig(config);

        realm.addComponentModel(generated);
    }

    public static void createAesProvider(RealmModel realm) {
        if (hasProvider(realm, "aes-generated")) return;
        ComponentModel generated = new ComponentModel();
        generated.setName("aes-generated");
        generated.setParentId(realm.getId());
        generated.setProviderId("aes-generated");
        generated.setProviderType(KeyProvider.class.getName());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", "100");
        generated.setConfig(config);

        realm.addComponentModel(generated);
    }

    protected static boolean hasProvider(RealmModel realm, String providerId) {
        List<ComponentModel> currentComponents = realm.getComponents(realm.getId(), KeyProvider.class.getName());
        for (ComponentModel current : currentComponents) {
            if (current.getProviderId().equals(providerId)) {
                return true;
            }
        }
        return false;
    }

    public static void createProviders(RealmModel realm, String privateKeyPem, String certificatePem) {
        if (!hasProvider(realm, "rsa")) {
            ComponentModel rsa = new ComponentModel();
            rsa.setName("rsa");
            rsa.setParentId(realm.getId());
            rsa.setProviderId("rsa");
            rsa.setProviderType(KeyProvider.class.getName());

            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "100");
            config.putSingle("privateKey", privateKeyPem);
            if (certificatePem != null) {
                config.putSingle("certificate", certificatePem);
            }
            rsa.setConfig(config);

            realm.addComponentModel(rsa);
        }

        createSecretProvider(realm);
        createAesProvider(realm);
    }

}
