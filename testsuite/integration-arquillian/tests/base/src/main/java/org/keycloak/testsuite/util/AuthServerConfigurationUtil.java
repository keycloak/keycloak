/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.util;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

public class AuthServerConfigurationUtil {

    private final SuiteContext suiteContext;
    private final ContainerController controller;

    public AuthServerConfigurationUtil(SuiteContext suiteContext, ContainerController controller) {
        this.suiteContext = suiteContext;
        this.controller = controller;
    }

    public void configureFixedHostname(String hostname, int httpPort, int httpsPort, boolean alwaysHttps) throws Exception {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            configureUndertow("fixed", hostname, httpPort, httpsPort, alwaysHttps);
        } else if (suiteContext.getAuthServerInfo().isJBossBased()) {
            configureWildFly("fixed", hostname, httpPort, httpsPort, alwaysHttps);
        } else {
            throw new RuntimeException("Don't know how to config");
        }
    }

    public void clearFixedHostname() throws Exception {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            configureUndertow("request", "localhost", -1, -1, false);
        } else if (suiteContext.getAuthServerInfo().isJBossBased()) {
            configureWildFly("request", "localhost", -1, -1, false);
        } else {
            throw new RuntimeException("Don't know how to config");
        }
    }

    public void configureUndertow(String provider, String hostname, int httpPort, int httpsPort, boolean alwaysHttps) {
        controller.stop(suiteContext.getAuthServerInfo().getQualifier());

        System.setProperty("keycloak.hostname.provider", provider);
        System.setProperty("keycloak.hostname.fixed.hostname", hostname);
        System.setProperty("keycloak.hostname.fixed.httpPort", String.valueOf(httpPort));
        System.setProperty("keycloak.hostname.fixed.httpsPort", String.valueOf(httpsPort));
        System.setProperty("keycloak.hostname.fixed.alwaysHttps", String.valueOf(alwaysHttps));

        controller.start(suiteContext.getAuthServerInfo().getQualifier());
    }

    public void configureWildFly(String provider, String hostname, int httpPort, int httpsPort, boolean alwaysHttps) throws Exception {
        OnlineManagementClient client = AuthServerTestEnricher.getManagementClient(suiteContext.getAuthServerInfo());
        Administration administration = new Administration(client);

        client.execute("/subsystem=keycloak-server/spi=hostname:write-attribute(name=default-provider,value=" + provider + ")");
        client.execute("/subsystem=keycloak-server/spi=hostname/provider=fixed:write-attribute(name=properties.hostname,value=" + hostname + ")");
        client.execute("/subsystem=keycloak-server/spi=hostname/provider=fixed:write-attribute(name=properties.httpPort,value=" + httpPort + ")");
        client.execute("/subsystem=keycloak-server/spi=hostname/provider=fixed:write-attribute(name=properties.httpsPort,value=" + httpsPort + ")");
        client.execute("/subsystem=keycloak-server/spi=hostname/provider=fixed:write-attribute(name=properties.alwaysHttps,value=" + alwaysHttps + ")");

        administration.reloadIfRequired();

        client.close();
    }
}
