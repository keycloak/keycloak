package org.keycloak.testsuite.arquillian;

import org.jboss.arquillian.container.spi.event.StartSuiteContainers;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.keycloak.testsuite.util.InfinispanContainer;


public class HotRodStoreTestEnricher {

    public static final String HOT_ROD_STORE_HOST_PROPERTY = "keycloak.connectionsHotRod.host";

    public static final boolean HOT_ROD_START_CONTAINER = Boolean.parseBoolean(System.getProperty("keycloak.testsuite.start-hotrod-container", "false"));

    private final InfinispanContainer hotRodContainer = new InfinispanContainer();

    public void beforeContainerStarted(@Observes(precedence = 1) StartSuiteContainers event) {
        if (!HOT_ROD_START_CONTAINER) return;
        hotRodContainer.start();

        // Add env variable, so it can be picked up by Keycloak
        System.setProperty(HOT_ROD_STORE_HOST_PROPERTY, hotRodContainer.getHost());
    }

    public void afterSuite(@Observes(precedence = 4) AfterSuite event) {
        if (!HOT_ROD_START_CONTAINER) return;
        hotRodContainer.stop();
    }
}
