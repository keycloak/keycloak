package org.keycloak.testsuite.util;


import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


/**
 * @author mhajas
 */
public class VaultUtils {

    public static void enableVault(SuiteContext suiteContext) throws IOException, CliException, TimeoutException, InterruptedException {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            System.setProperty("keycloak.vault.plaintext.provider.enabled", "true");
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            client.execute("/subsystem=keycloak-server/spi=vault/:add");
            client.execute("/subsystem=keycloak-server/spi=vault/provider=files-plaintext/:add(enabled=true,properties={dir => \"${jboss.home.dir}/standalone/configuration/vault\"})");
            client.close();
        }
    }

    public static void disableVault(SuiteContext suiteContext) throws IOException, CliException, TimeoutException, InterruptedException {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            System.setProperty("keycloak.vault.plaintext.provider.enabled", "false");
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            client.execute("/subsystem=keycloak-server/spi=vault/:remove");
            client.close();
        }
    }

}
