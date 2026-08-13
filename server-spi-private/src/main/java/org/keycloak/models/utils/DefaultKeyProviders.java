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

import java.util.Objects;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeyProviders {

    public static final String DEFAULT_PRIORITY = "100";

    public static void createProviders(RealmModel realm) {
        if (!hasProvider(realm, "rsa-generated")) {
            createRsaKeyProvider("rsa-generated", realm);
        }

        if (!hasProvider(realm, "rsa-enc-generated")) {
            createRsaEncKeyProvider("rsa-enc-generated", realm);
        }

        createSecretProvider(realm);
        createAesProvider(realm);
    }

    private static void createRsaKeyProvider(String name, RealmModel realm) {
        ComponentModel generated = new ComponentModel();
        generated.setName(name);
        generated.setParentId(realm.getId());
        generated.setProviderId("rsa-generated");
        generated.setProviderType(KeyProvider.class.getName());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", DEFAULT_PRIORITY);
        config.putSingle("keyUse", KeyUse.SIG.name());
        generated.setConfig(config);

        realm.addComponentModel(generated);
    }

    private static void createRsaEncKeyProvider(String name, RealmModel realm) {
        ComponentModel generated = new ComponentModel();
        generated.setName(name);
        generated.setParentId(realm.getId());
        generated.setProviderId("rsa-enc-generated");
        generated.setProviderType(KeyProvider.class.getName());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", DEFAULT_PRIORITY);
        config.putSingle("keyUse", KeyUse.ENC.name());
        config.putSingle("algorithm", Algorithm.RSA_OAEP);
        generated.setConfig(config);

        realm.addComponentModel(generated);
    }

    public static void createSecretProvider(RealmModel realm) {
        if (hasProvider(realm, "hmac-generated", Algorithm.HS512)) return;
        ComponentModel generated = new ComponentModel();
        generated.setName("hmac-generated-hs512");
        generated.setParentId(realm.getId());
        generated.setProviderId("hmac-generated");
        generated.setProviderType(KeyProvider.class.getName());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", DEFAULT_PRIORITY);
        config.putSingle("algorithm", Algorithm.HS512);
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
        config.putSingle("priority", DEFAULT_PRIORITY);
        generated.setConfig(config);

        realm.addComponentModel(generated);
    }

    protected static boolean hasProvider(RealmModel realm, String providerId) {
        return hasProvider(realm, providerId, null);
    }

    protected static boolean hasProvider(RealmModel realm, String providerId, String algorithm) {
        return realm.getComponentsStream(realm.getId(), KeyProvider.class.getName())
                .anyMatch(component -> Objects.equals(component.getProviderId(), providerId)
                        && (algorithm == null || algorithm.equals(component.getConfig().getFirst("algorithm"))));
    }

    public static void createProviders(RealmModel realm, String privateKeyPem, String certificatePem) {
        if (!hasProvider(realm, "rsa")) {
            ComponentModel rsa = new ComponentModel();
            rsa.setName("rsa");
            rsa.setParentId(realm.getId());
            rsa.setProviderId("rsa");
            rsa.setProviderType(KeyProvider.class.getName());

            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("keyUse", KeyUse.SIG.getSpecName());
            config.putSingle("priority", DEFAULT_PRIORITY);
            config.putSingle("privateKey", privateKeyPem);
            if (certificatePem != null) {
                config.putSingle("certificate", certificatePem);
            }
            rsa.setConfig(config);

            realm.addComponentModel(rsa);
        }

        if (!hasProvider(realm, "rsa-enc")) {
            ComponentModel rsaEnc = new ComponentModel();
            rsaEnc.setName("rsa-enc");
            rsaEnc.setParentId(realm.getId());
            rsaEnc.setProviderId("rsa-enc");
            rsaEnc.setProviderType(KeyProvider.class.getName());

            MultivaluedHashMap<String, String> configEnc = new MultivaluedHashMap<>();
            configEnc.putSingle("keyUse", KeyUse.ENC.getSpecName());
            configEnc.putSingle("priority", "100");
            configEnc.putSingle("privateKey", privateKeyPem);
            if (certificatePem != null) {
                configEnc.putSingle("certificate", certificatePem);
            }
            rsaEnc.setConfig(configEnc);

            realm.addComponentModel(rsaEnc);
        }

        createSecretProvider(realm);
        createAesProvider(realm);
    }

}
