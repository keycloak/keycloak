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
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.info.ProviderRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.auth.page.login.Login;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
        assertNotNull(info.getThemes().get("admin"));
        assertNotNull(info.getThemes().get("email"));
        assertNotNull(info.getThemes().get("login"));
        assertNotNull(info.getThemes().get("welcome"));

        assertNotNull(info.getEnums());

        assertNotNull(info.getMemoryInfo());
        assertNotNull(info.getSystemInfo());
        assertNotNull(info.getCryptoInfo());
        String expectedSupportedKeystoreTypes = System.getProperty("auth.server.supported.keystore.types");
        if (expectedSupportedKeystoreTypes == null) {
            fail("Property 'auth.server.supported.keystore.types' not set");
        }
        Assert.assertNames(info.getCryptoInfo().getSupportedKeystoreTypes(), expectedSupportedKeystoreTypes.split(","));

        assertEquals(Version.VERSION, info.getSystemInfo().getVersion());
        assertNotNull(info.getSystemInfo().getServerTime());
        assertNotNull(info.getSystemInfo().getUptime());

        assertNotNull(info.getBuiltinProtocolMappers());
        assertNotNull(getBuiltinProtocolMapper(info, Login.OIDC, OIDCLoginProtocolFactory.ACR));

        if (isJpaRealmProvider()) {
            Map<String, ProviderRepresentation> jpaProviders = info.getProviders().get("connectionsJpa").getProviders();
            ProviderRepresentation jpaProvider = jpaProviders.values().iterator().next();
            log.infof("JPA Connections provider info: %s", jpaProvider.getOperationalInfo());
        }
    }

    /*@Test
    @DisableFeature(value = Profile.Feature.STEP_UP_AUTHENTICATION, skipRestart = true)
    public void testDisableStepupAuthenticationFeature() throws Exception {
        // refresh builtin based on feature changed
        testingClient.server().fetchString(it -> {
            ((OIDCLoginProtocolFactory) it.getKeycloakSessionFactory()
                .getProviderFactory(LoginProtocol.class, Login.OIDC))
                .initBuiltin();
            return null;
        });

        ServerInfoRepresentation info = adminClient.serverInfo().getInfo();
        assertNotNull(info.getBuiltinProtocolMappers());
        assertNull(getBuiltinProtocolMapper(info, Login.OIDC, OIDCLoginProtocolFactory.ACR));
    }*/

    private static ProtocolMapperRepresentation getBuiltinProtocolMapper(ServerInfoRepresentation info, String protocol, String name) {
        return info.getBuiltinProtocolMappers()
            .get(protocol)
            .stream()
            .filter(it -> name.equals(it.getName()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }
}
