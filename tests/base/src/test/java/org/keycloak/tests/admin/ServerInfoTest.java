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

package org.keycloak.tests.admin;

import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Version;
import org.keycloak.crypto.Algorithm;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedRsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentTypeRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.info.ProviderRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectCryptoHelper;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.crypto.CryptoHelper;
import org.keycloak.tests.utils.Assert;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class ServerInfoTest {

    @InjectAdminClient
    Keycloak adminClient;

    @InjectCryptoHelper
    CryptoHelper cryptoHelper;

    @Test
    public void testServerInfo() {
        ServerInfoRepresentation info = adminClient.serverInfo().getInfo();
        assertNotNull(info);

        assertNotNull(info.getProviders());
        assertNotNull(info.getProviders().get("realm"));
        assertNotNull(info.getProviders().get("user"));
        assertNotNull(info.getProviders().get("authenticator"));

        assertNotNull(info.getThemes());
        assertNotNull(info.getThemes().get("account"));
        Assert.assertNames(info.getThemes().get("account"), "base", "keycloak.v3");
        Assert.assertNames(info.getThemes().get("admin"), "base", "keycloak.v2");
        Assert.assertNames(info.getThemes().get("email"), "base", "keycloak");
        Assert.assertNames(info.getThemes().get("login"), "base", "keycloak", "keycloak.v2");
        Assert.assertNames(info.getThemes().get("welcome"), "keycloak");

        assertNotNull(info.getEnums());

        assertNotNull(info.getMemoryInfo());
        assertNotNull(info.getSystemInfo());

        assertNotNull(info.getCryptoInfo());
        Assert.assertNames(info.getCryptoInfo().getSupportedKeystoreTypes(), cryptoHelper.getExpectedSupportedKeyStoreTypes());
        Assert.assertNames(info.getCryptoInfo().getClientSignatureSymmetricAlgorithms(), Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);
        Assert.assertNames(info.getCryptoInfo().getClientSignatureAsymmetricAlgorithms(),
                Algorithm.ES256, Algorithm.ES384, Algorithm.ES512,
                Algorithm.EdDSA, Algorithm.PS256, Algorithm.PS384,
                Algorithm.PS512, Algorithm.RS256, Algorithm.RS384,
                Algorithm.RS512);

        ComponentTypeRepresentation rsaGeneratedProviderInfo = info.getComponentTypes().get(KeyProvider.class.getName())
                .stream()
                .filter(componentType -> GeneratedRsaKeyProviderFactory.ID.equals(componentType.getId()))
                .findFirst().orElseThrow(() -> new RuntimeException("Not found provider with ID 'rsa-generated'"));
        ConfigPropertyRepresentation keySizeRep = rsaGeneratedProviderInfo.getProperties()
                .stream()
                .filter(configProp -> Attributes.KEY_SIZE_KEY.equals(configProp.getName()))
                .findFirst().orElseThrow(() -> new RuntimeException("Not found provider with ID 'rsa-generated'"));
        Assert.assertNames(keySizeRep.getOptions(), cryptoHelper.getExpectedSupportedRsaKeySizes());

        assertEquals(Version.VERSION, info.getSystemInfo().getVersion());
        assertNotNull(info.getSystemInfo().getServerTime());
        assertNotNull(info.getSystemInfo().getUptime());

        Map<String, ProviderRepresentation> jpaProviders = info.getProviders().get("connectionsJpa").getProviders();
        ProviderRepresentation jpaProvider = jpaProviders.values().iterator().next();
        Assertions.assertNotNull(jpaProvider.getOperationalInfo());
    }
}
