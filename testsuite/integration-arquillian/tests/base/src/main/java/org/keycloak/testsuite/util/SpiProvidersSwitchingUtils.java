package org.keycloak.testsuite.util;

import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.annotation.SetDefaultProvider;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class SpiProvidersSwitchingUtils {
    public static void addProviderDefaultValue(SuiteContext suiteContext, SetDefaultProvider annotation) throws IOException, CliException {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            System.setProperty("keycloak." + annotation.spi() + ".provider", annotation.providerId());
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            client.execute("/subsystem=keycloak-server/spi=" + annotation.spi() + "/:add(default-provider=\"" + annotation.providerId() + "\")");
            client.close();
        }
    }

    public static void removeProvider(SuiteContext suiteContext, SetDefaultProvider annotation) throws IOException, CliException {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            System.clearProperty("keycloak." + annotation.spi() + ".provider");
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            client.execute("/subsystem=keycloak-server/spi=" + annotation.spi() + "/:remove");
            client.close();
        }
    }
}
