package org.keycloak.testsuite.url;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.containers.KeycloakQuarkusServerDeployableContainer;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractHostnameTest extends AbstractKeycloakTest {

    private static final Logger LOGGER = Logger.getLogger(AbstractHostnameTest.class);

    @ArquillianResource
    protected ContainerController controller;

    void reset() throws Exception {
        LOGGER.info("Reset hostname config to default");

        if (suiteContext.getAuthServerInfo().isUndertow()) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            removeProperties("keycloak.hostname.provider",
                    "keycloak.frontendUrl",
                    "keycloak.adminUrl",
                    "keycloak.hostname.default.forceBackendUrlToFrontendUrl",
                    "keycloak.hostname.fixed.hostname",
                    "keycloak.hostname.fixed.httpPort",
                    "keycloak.hostname.fixed.httpsPort",
                    "keycloak.hostname.fixed.alwaysHttps");
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else if (suiteContext.getAuthServerInfo().isJBossBased()) {
            executeCli("/subsystem=keycloak-server/spi=hostname:remove",
                    "/subsystem=keycloak-server/spi=hostname/:add(default-provider=default)",
                    "/subsystem=keycloak-server/spi=hostname/provider=default/:add(properties={frontendUrl => \"${keycloak.frontendUrl:}\",forceBackendUrlToFrontendUrl => \"false\"},enabled=true)");
        } else if (suiteContext.getAuthServerInfo().isQuarkus()) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            KeycloakQuarkusServerDeployableContainer container = (KeycloakQuarkusServerDeployableContainer)suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            container.resetConfiguration();
            container.setAdditionalBuildArgs(Collections.singletonList("--spi-hostname-provider=default"));
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else {
            throw new RuntimeException("Don't know how to config");
        }

        reconnectAdminClient();
    }

    void configureDefault(String frontendUrl, boolean forceBackendUrlToFrontendUrl, String adminUrl) throws Exception {
        LOGGER.infov("Configuring default hostname provider: frontendUrl={0}, forceBackendUrlToFrontendUrl={1}, adminUrl={3}", frontendUrl, forceBackendUrlToFrontendUrl, adminUrl);

        if (suiteContext.getAuthServerInfo().isUndertow()) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            System.setProperty("keycloak.hostname.provider", "default");
            System.setProperty("keycloak.frontendUrl", frontendUrl);
            if (adminUrl != null){
                System.setProperty("keycloak.adminUrl", adminUrl);
            }
            System.setProperty("keycloak.hostname.default.forceBackendUrlToFrontendUrl", String.valueOf(forceBackendUrlToFrontendUrl));
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else if (suiteContext.getAuthServerInfo().isJBossBased()) {
            executeCli("/subsystem=keycloak-server/spi=hostname:remove",
                    "/subsystem=keycloak-server/spi=hostname/:add(default-provider=default)",
                    "/subsystem=keycloak-server/spi=hostname/provider=default/:add(properties={" +
                            "frontendUrl => \"" + frontendUrl + "\"" +
                            ",forceBackendUrlToFrontendUrl => \"" + forceBackendUrlToFrontendUrl + "\"" +
                            (adminUrl != null ? ",adminUrl=\"" + adminUrl + "\"" : "") + "},enabled=true)");
        } else if (suiteContext.getAuthServerInfo().isQuarkus()) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            KeycloakQuarkusServerDeployableContainer container = (KeycloakQuarkusServerDeployableContainer)suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            List<String> additionalArgs = new ArrayList<>();
            additionalArgs.add("--spi-hostname-default-frontend-url="+frontendUrl);
            additionalArgs.add("--spi-hostname-default-force-backend-url-to-frontend-url="+ forceBackendUrlToFrontendUrl);
            if (adminUrl != null){
                additionalArgs.add("--spi-hostname-default-admin-url="+adminUrl);
            }
            container.setAdditionalBuildArgs(additionalArgs);
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else {
            throw new RuntimeException("Don't know how to config");
        }

        reconnectAdminClient();
    }

    void configureFixed(String hostname, int httpPort, int httpsPort, boolean alwaysHttps) throws Exception {

        if (suiteContext.getAuthServerInfo().isUndertow()) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            System.setProperty("keycloak.hostname.provider", "fixed");
            System.setProperty("keycloak.hostname.fixed.hostname", hostname);
            System.setProperty("keycloak.hostname.fixed.httpPort", String.valueOf(httpPort));
            System.setProperty("keycloak.hostname.fixed.httpsPort", String.valueOf(httpsPort));
            System.setProperty("keycloak.hostname.fixed.alwaysHttps", String.valueOf(alwaysHttps));
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else if (suiteContext.getAuthServerInfo().isJBossBased()) {
            executeCli("/subsystem=keycloak-server/spi=hostname:remove",
                    "/subsystem=keycloak-server/spi=hostname/:add(default-provider=fixed)",
                    "/subsystem=keycloak-server/spi=hostname/provider=fixed/:add(properties={hostname => \"" + hostname + "\",httpPort => \"" + httpPort + "\",httpsPort => \"" + httpsPort + "\",alwaysHttps => \"" + alwaysHttps + "\"},enabled=true)");
        } else if (suiteContext.getAuthServerInfo().isQuarkus()) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            KeycloakQuarkusServerDeployableContainer container = (KeycloakQuarkusServerDeployableContainer)suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            container.setAdditionalBuildArgs(Collections.singletonList("--spi-hostname-provider=fixed" +
                    " --spi-hostname-fixed-hostname=" + hostname +
                    " --spi-hostname-fixed-http-port=" + httpPort +
                    " --spi-hostname-fixed-https-port=" + httpsPort +
                    " --spi-hostname-fixed-always-https=" + alwaysHttps));
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else {
            throw new RuntimeException("Don't know how to config");
        }

        reconnectAdminClient();
    }

    private void executeCli(String... commands) throws Exception {
        OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
        Administration administration = new Administration(client);

        LOGGER.debug("Running CLI commands:");
        for (String c : commands) {
            LOGGER.debug(c);
            client.execute(c).assertSuccess();
        }
        LOGGER.debug("Done");

        administration.reload();

        client.close();
    }

    private void removeProperties(String... keys) {
        for (String k : keys) {
            System.getProperties().remove(k);
        }
    }


}
