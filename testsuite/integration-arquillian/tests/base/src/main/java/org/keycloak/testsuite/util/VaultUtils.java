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


import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


/**
 * @author mhajas
 */
public class VaultUtils {

    public static void enableVault(SuiteContext suiteContext, EnableVault.PROVIDER_ID provider) throws IOException, CliException, TimeoutException, InterruptedException {
        ContainerInfo serverInfo = suiteContext.getAuthServerInfo();

        if (serverInfo.isUndertow()) {
            System.setProperty("keycloak.vault." + provider.getName() + ".provider.enabled", "true");
        } else if (serverInfo.isJBossBased()) {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            // configure the selected provider and set it as the default vault provider.
            client.execute("/subsystem=keycloak-server/spi=vault/:add(default-provider=" + provider.getName() + ")");
            for (String command : provider.getCliInstallationCommands()) {
                client.execute(command);
            }
            client.close();
        }
    }

    public static void disableVault(SuiteContext suiteContext, EnableVault.PROVIDER_ID provider) throws IOException, CliException, TimeoutException, InterruptedException {
        ContainerInfo serverInfo = suiteContext.getAuthServerInfo();

        if (serverInfo.isUndertow()) {
            System.setProperty("keycloak.vault." + provider.getName() + ".provider.enabled", "false");
        } else if (serverInfo.isJBossBased()) {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            for (String command : provider.getCliRemovalCommands()) {
                client.execute(command);
            }
            client.execute("/subsystem=keycloak-server/spi=vault/:remove");
            client.close();
        }
    }

}
