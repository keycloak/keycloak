package org.keycloak.testsuite.util;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.annotation.EnableCiba;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

public class CibaUtils {

    public static void enableCiba(SuiteContext suiteContext, EnableCiba.PROVIDER_ID provider) throws IOException, CliException, TimeoutException, InterruptedException {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            // NOP
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            // configure the selected provider and set it as the default vault provider.
            client.execute("/subsystem=keycloak-server/spi=decoupled-authn/:add(default-provider=" + provider.getName() + ")");
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
            client.execute("/subsystem=keycloak-server/spi=decoupled-authn/:remove");
            client.close();
        }
    }
}
