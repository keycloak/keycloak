package org.keycloak.testsuite.util;

import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.annotation.SetDefaultProvider;
import org.keycloak.testsuite.arquillian.containers.KeycloakQuarkusServerDeployableContainer;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import java.io.IOException;
import java.util.Collections;

public class SpiProvidersSwitchingUtils {

    private static final String SUBSYSTEM_KEYCLOAK_SERVER_SPI = "/subsystem=keycloak-server/spi=";
    private static final String KEYCLOAKX_ARG_SPI_PREFIX = "--spi-";

    private SpiProvidersSwitchingUtils() {}

    public static void addProviderDefaultValue(SuiteContext suiteContext, SetDefaultProvider annotation) throws IOException, CliException {
        ContainerInfo authServerInfo = suiteContext.getAuthServerInfo();

        if (authServerInfo.isUndertow()) {
            System.setProperty("keycloak." + annotation.spi() + ".provider", annotation.providerId());
        } else if (authServerInfo.isQuarkus()) {
            KeycloakQuarkusServerDeployableContainer container = (KeycloakQuarkusServerDeployableContainer) authServerInfo.getArquillianContainer().getDeployableContainer();
            container.setAdditionalBuildArgs(Collections.singletonList(KEYCLOAKX_ARG_SPI_PREFIX + toDashCase(annotation.spi()) + "-provider=" + annotation.providerId()));
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();

            if (annotation.onlyUpdateDefault()) {
                client.execute(SUBSYSTEM_KEYCLOAK_SERVER_SPI + annotation.spi() + ":write-attribute(name=default-provider, value=" + annotation.providerId() + ")");
            } else {
                client.execute(SUBSYSTEM_KEYCLOAK_SERVER_SPI + annotation.spi() + "/:add(default-provider=\"" + annotation.providerId() + "\")");
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
            container.setAdditionalBuildArgs(Collections.singletonList(KEYCLOAKX_ARG_SPI_PREFIX + toDashCase(annotation.spi()) + "-provider=default"));
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            if (annotation.onlyUpdateDefault()) {
                client.execute(SUBSYSTEM_KEYCLOAK_SERVER_SPI + annotation.spi() + "/:undefine-attribute(name=default-provider)");
            } else {
                client.execute(SUBSYSTEM_KEYCLOAK_SERVER_SPI + annotation.spi() + "/:remove");
            }
            client.close();
        }
    }

    /**
     *  Parses the non-standard SPI-Name format to the standardized format
     *  we use in the Keycloak.X Configuration
     * @param s possibly non-standard spi name
     * @return standardized spi name in dash-case. e.g. userProfile -> user-profile
     */
    private static String toDashCase(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean l = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (l && Character.isUpperCase(c)) {
                sb.append('-');
                c = Character.toLowerCase(c);
                l = false;
            } else {
                l = Character.isLowerCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
