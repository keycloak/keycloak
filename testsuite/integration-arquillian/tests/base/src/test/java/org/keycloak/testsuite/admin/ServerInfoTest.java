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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
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

        assertEquals(Version.VERSION, info.getSystemInfo().getVersion());
        assertNotNull(info.getSystemInfo().getServerTime());
        assertNotNull(info.getSystemInfo().getUptime());

        log.infof("JPA Connections provider info: %s", info.getProviders().get("connectionsJpa").getProviders().get("default").getOperationalInfo().toString());
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }
}
