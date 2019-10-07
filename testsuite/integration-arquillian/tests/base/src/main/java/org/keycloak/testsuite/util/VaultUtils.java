package org.keycloak.testsuite.util;


import org.jboss.arquillian.container.test.api.ContainerController;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


/**
 * @author mhajas
 */
public class VaultUtils {

    public static void enableVault(SuiteContext suiteContext, ContainerController controller) throws IOException, CliException, TimeoutException, InterruptedException {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            System.setProperty("keycloak.vault.plaintext.provider.enabled", "true");
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            Administration administration = new Administration(client);

            client.execute("/subsystem=keycloak-server/spi=vault/:add");
            client.execute("/subsystem=keycloak-server/spi=vault/provider=plaintext/:add(enabled=true,properties={dir => \"${jboss.home.dir}/standalone/configuration/vault\"})");

            administration.reload();

            client.close();
        }
    }

    public static void disableVault(SuiteContext suiteContext, ContainerController controller) throws IOException, CliException, TimeoutException, InterruptedException {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            System.setProperty("keycloak.vault.plaintext.provider.enabled", "false");
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            Administration administration = new Administration(client);

            client.execute("/subsystem=keycloak-server/spi=vault/:remove");

            administration.reload();

            client.close();
        }
    }

}
