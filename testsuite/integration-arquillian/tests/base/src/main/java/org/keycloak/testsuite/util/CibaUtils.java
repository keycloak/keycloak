/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.annotation.EnableCiba;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class CibaUtils {

    public static void enableCiba(SuiteContext suiteContext, EnableCiba.PROVIDER_ID provider) throws IOException, CliException, TimeoutException, InterruptedException {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            // NOP
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            // configure the selected provider and set it as the default vault provider.
            client.execute("/subsystem=keycloak-server/spi=ciba-auth-channel/:add(default-provider=" + provider.getName() + ")");
            for (String command : provider.getCliInstallationCommands()) {
                ModelNodeResult r = client.execute(command);
            }
            client.close();
        }
    }

    public static void disableCiba(SuiteContext suiteContext, EnableCiba.PROVIDER_ID provider) throws IOException, CliException, TimeoutException, InterruptedException {
        if (suiteContext.getAuthServerInfo().isUndertow() || suiteContext.getAuthServerInfo().isQuarkus()) {
            // NOP
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            for (String command : provider.getCliRemovalCommands()) {
                client.execute(command);
            }
            client.execute("/subsystem=keycloak-server/spi=ciba-auth-channel/:remove");
            client.close();
        }
    }
}
