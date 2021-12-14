package org.keycloak.testsuite.oauth;

import java.util.Arrays;
import org.jboss.arquillian.container.spi.Container;
import org.junit.BeforeClass;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.containers.KeycloakQuarkusServerDeployableContainer;
import org.keycloak.testsuite.util.ContainerAssume;

public class BackchannelLogoutOnQuarkusTest extends BackchannelLogoutTest {

    @BeforeClass
    public static void enabled() {
        ContainerAssume.assumeAuthServerQuarkus();
    }

    @Override
    public void postBackchannelLogoutNestedBrokeringRevokeOfflineSessions() throws Exception {
        try {
            enablePreLoadOfflineSessions();
            super.postBackchannelLogoutNestedBrokeringRevokeOfflineSessions();
        } finally {
            reset();
        }
    }

    @Override
    public void postBackchannelLogoutNestedBrokeringRevokeOfflineSessionsWithoutActiveUserSession() throws Exception {
        try {
            enablePreLoadOfflineSessions();
            super.postBackchannelLogoutNestedBrokeringRevokeOfflineSessionsWithoutActiveUserSession();
        } finally {
            reset();
        }
    }

    private void enablePreLoadOfflineSessions() throws Exception {
        KeycloakQuarkusServerDeployableContainer container = getQuarkusContainer();

        container.setAdditionalBuildArgs(Arrays.asList("--spi-user-sessions-infinispan-preload-offline-sessions-from-database=true"));
        container.restartServer();
        reconnectAdminClient();
    }

    private void reset() {
        KeycloakQuarkusServerDeployableContainer container = getQuarkusContainer();
        container.resetConfiguration();
    }

    private KeycloakQuarkusServerDeployableContainer getQuarkusContainer() {
        ContainerInfo authServerInfo = suiteContext.getAuthServerInfo();
        Container arquillianContainer = authServerInfo.getArquillianContainer();
        KeycloakQuarkusServerDeployableContainer container = (KeycloakQuarkusServerDeployableContainer) arquillianContainer.getDeployableContainer();
        return container;
    }
}