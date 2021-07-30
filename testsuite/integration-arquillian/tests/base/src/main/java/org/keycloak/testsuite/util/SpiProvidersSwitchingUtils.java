package org.keycloak.testsuite.util;

import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.annotation.SetDefaultProvider;
import org.keycloak.testsuite.arquillian.containers.KeycloakQuarkusServerDeployableContainer;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import java.io.IOException;

public class SpiProvidersSwitchingUtils {
    public static void addProviderDefaultValue(SuiteContext suiteContext, SetDefaultProvider annotation) throws IOException, CliException {
        ContainerInfo authServerInfo = suiteContext.getAuthServerInfo();

        if (authServerInfo.isUndertow()) {
            System.setProperty("keycloak." + annotation.spi() + ".provider", annotation.providerId());
        } else if (authServerInfo.isQuarkus()) {
            KeycloakQuarkusServerDeployableContainer container = (KeycloakQuarkusServerDeployableContainer) authServerInfo.getArquillianContainer().getDeployableContainer();
            container.forceReAugmentation("-Dkeycloak." + annotation.spi() + ".provider=" + annotation.providerId());
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();

            if (annotation.onlyUpdateDefault()) {
                client.execute("/subsystem=keycloak-server/spi=" + annotation.spi() + ":write-attribute(name=default-provider, value=" + annotation.providerId() + ")");
            } else {
                client.execute("/subsystem=keycloak-server/spi=" + annotation.spi() + "/:add(default-provider=\"" + annotation.providerId() + "\")");
            }

            client.close();
        }
    }

    public static void removeProvider(SuiteContext suiteContext, SetDefaultProvider annotation) throws IOException, CliException {
        ContainerInfo authServerInfo = suiteContext.getAuthServerInfo();

        if (authServerInfo.isUndertow()) {
            System.clearProperty("keycloak." + annotation.spi() + ".provider");
        } else if (authServerInfo.isQuarkus()) {
            KeycloakQuarkusServerDeployableContainer container = (KeycloakQuarkusServerDeployableContainer) authServerInfo.getArquillianContainer().getDeployableContainer();
            container.resetConfiguration();
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            if (annotation.onlyUpdateDefault()) {
                client.execute("/subsystem=keycloak-server/spi=" + annotation.spi() + "/:undefine-attribute(name=default-provider)");
            } else {
                client.execute("/subsystem=keycloak-server/spi=" + annotation.spi() + "/:remove");
            }
            client.close();
        }
    }
}
