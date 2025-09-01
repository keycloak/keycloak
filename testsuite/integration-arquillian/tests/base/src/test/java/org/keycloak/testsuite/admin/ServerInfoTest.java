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

import org.junit.Test;
import org.keycloak.common.Version;
import org.keycloak.crypto.Algorithm;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedRsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentTypeRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.info.ProviderRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.KeystoreUtils;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ServerInfoTest extends AbstractKeycloakTest {

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
        Assert.assertNames(info.getThemes().get("account"), "base", "keycloak.v3", "custom-account-provider");
        Assert.assertNames(info.getThemes().get("admin"), "base", "keycloak.v2");
        Assert.assertNames(info.getThemes().get("email"), "base", "keycloak");
        Assert.assertNames(info.getThemes().get("login"), "address", "base", "environment-agnostic", "keycloak", "keycloak.v2", "organization", "themeconfig");
        Assert.assertNames(info.getThemes().get("welcome"), "keycloak");

        assertNotNull(info.getEnums());

        assertNotNull(info.getMemoryInfo());
        assertNotNull(info.getSystemInfo());
        assertNotNull(info.getCryptoInfo());
        Assert.assertNames(info.getCryptoInfo().getSupportedKeystoreTypes(), KeystoreUtils.getSupportedKeystoreTypes());
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
        Assert.assertNames(keySizeRep.getOptions(), KeyUtils.getExpectedSupportedRsaKeySizes());

        assertEquals(Version.VERSION, info.getSystemInfo().getVersion());
        assertNotNull(info.getSystemInfo().getServerTime());
        assertNotNull(info.getSystemInfo().getUptime());

        Map<String, ProviderRepresentation> jpaProviders = info.getProviders().get("connectionsJpa").getProviders();
        ProviderRepresentation jpaProvider = jpaProviders.values().iterator().next();
        log.infof("JPA Connections provider info: %s", jpaProvider.getOperationalInfo());
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }
}
